package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.Procedure;
import eu.prismacloud.message.CheckPoint;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.CommonMessageBuilder;
import eu.prismacloud.message.replica_state.Configure;
import eu.prismacloud.message.CreateTransaction;
import eu.prismacloud.message.execution.ExecutedWithState;
import eu.prismacloud.message.replica_state.ExecutorReady;
import eu.prismacloud.message.replica_state.RemoteReplicasReady;
import eu.prismacloud.message.replica_state.ReplicaConfigured;
import eu.prismacloud.message.replica_state.ViewReady;
import eu.prismacloud.message.replica.Commit;
import eu.prismacloud.message.replica.Prepare;
import eu.prismacloud.message.replica.Preprepare;
import eu.prismacloud.message.replica.PreprepareBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import scala.Option;

/**
 * Replica makes sure that a View always has valid transactions (including
 * initial client command as well as Parts with sequence numbers) -- this might
 * change our view-change protocol though.
 * 
 * @author andy
 */
public class Replica extends UntypedActor {
        
    private final int replicaId;
    
    private final int fCount = 1;
    
    private final boolean master;
    
    private int seqCounter = 0;
    
    private final HashMap<Integer, ClientCommand> clientMap = new HashMap<>();
    
    private final HashMap<Integer, Preprepare> preprepareMap = new HashMap<>();
    
    private final ActorRef executor;
    
    private ActorRef remoteReplicas;
    
    private final int viewNr = 1;
    
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    
    /**
     * creates a transaction and forwards the message to the transaction
     */
    private Procedure<Object> configured = (Object message) -> {
        
        System.err.println("received message: " + message);
                
        if (message instanceof ClientCommand) {
            ClientCommand cmd = (ClientCommand)message;
            
            CommonMessageBuilder.validate(cmd);
            cmd.setSender(getSender());
            
            if (this.isMaster()) {
                int newSeq = ++seqCounter;
                getViewFor(viewNr).tell(new CreateTransaction(new PreprepareBuilder(newSeq, viewNr, cmd).buildFakeSelfPreprepare(),
                                                              fCount, cmd), getSender());
            } else {
                /* was the client id already mentioned before? */
                if (preprepareMap.containsKey(cmd.sequenceId)) {
                   getViewFor(viewNr).tell(new CreateTransaction(preprepareMap.remove(cmd.sequenceId),
                                                                 fCount, cmd), getSender()); 
                } else {
                    clientMap.put(cmd.sequenceId, cmd);
                }
            }
        } else if (message instanceof Preprepare) {
            Preprepare cmd = (Preprepare)message;
            CommonMessageBuilder.validate(cmd);
            
            if (isMaster()) {
                assert(false);
            }
            
            if (clientMap.containsKey(cmd.clientSequence)) {
                getViewFor(viewNr).tell(new CreateTransaction(cmd, fCount,
                                                              clientMap.remove(cmd.clientSequence)), getSender()); 
            } else {
                preprepareMap.put(cmd.sequenceNr, cmd);
            }
        } else if (message instanceof Prepare) {
            Prepare cmd = (Prepare)message;
            CommonMessageBuilder.validate(cmd);
            getViewFor(cmd.view).tell(cmd, ActorRef.noSender());
       } else if (message instanceof Commit) {
            Commit cmd = (Commit)message;
            CommonMessageBuilder.validate(cmd);
            getViewFor(cmd.view).tell(cmd, ActorRef.noSender());
        } else if (message instanceof ExecutedWithState) {
            ExecutedWithState cmd = (ExecutedWithState)message;
            CheckPoint cp = new CheckPoint(cmd.sequenceNr, cmd.stateDigest, null);
            addCheckpoint(cp);
            remoteReplicas.tell(cp, ActorRef.noSender());
        } else {
            unhandled(message);
        }
    };
    
    private final HashMap<Integer, Set<CheckPoint>> checkpoints = new HashMap<>();
        
    private ActorRef getViewFor(int viewNr) {
        final Option<ActorRef> child = getContext().child("view-" + viewNr);
        if (child.isDefined()) {
            return child.get();
        } else {
            assert(false);
            return null;
        }
    }
    
    private boolean isMaster() {
        return this.master;
    }
    
    public static Props props(int replicaId, boolean master) {
        return Props.create(new Creator<Replica>() {
           @Override
           public Replica create() throws Exception {
               return new Replica(replicaId, master);
           }
        });
    }
    
    public Replica(int replicaId, boolean master) {
        this.master = master;
        this.replicaId = replicaId;
        this.executor = getContext().actorOf(Executor.props(replicaId, getSelf()));
    }
    
    @Override
    public void onReceive(Object message) {
        if (message instanceof ExecutorReady) {
            executorReady = true;
        } else if (message instanceof Configure) {
            Configure config = (Configure)message;

            String me = getSelf().path().toStringWithoutAddress();
            
            /* remove myself from peers */
            Set<ActorSelection> peers = config.getPeers().stream().filter(f -> !f.equalsIgnoreCase(me))
                                              .map(f -> context().actorSelection(f))
                                              .collect(Collectors.toSet());
        
            remoteReplicas = getContext().actorOf(RemoteReplicas.props(peers, getSelf()));
                        
            /* TODO: there should be only one active view -> can't we check against this? */
            getContext().actorOf(View.props(viewNr, isMaster(), replicaId, executor, remoteReplicas), "view-" + viewNr);
            
            configurerer = getSender();
            
            log.debug("new becoming configuring..");
            getContext().become(configuring);
        } else {
            unhandled(message);
        }
    }
    
    private ActorRef configurerer;
    
    private boolean executorReady = false;
    
    private boolean remotePeersReady = false;
    
    private boolean viewReady = false;
    
    private Procedure<Object> configuring = (Object message) -> {
        if (message instanceof ExecutorReady) {
            executorReady = true;
        } else if (message instanceof RemoteReplicasReady) {
            remotePeersReady = true;
        } else if (message instanceof ViewReady) {
            viewReady = true;
        } else {
            unhandled(message);
        }
        
        if (executorReady && remotePeersReady && viewReady) {
            getContext().become(configured);
            log.debug("new becoming configured..");
            configurerer.tell(new ReplicaConfigured(), ActorRef.noSender());
        }
    };

    private void addCheckpoint(CheckPoint checkPoint) {
        int seqNr = checkPoint.lastSequenceNr;
        
        if (checkpoints.containsKey(seqNr)) {
            checkpoints.get(seqNr).add(checkPoint);
            
            long count = checkpoints.get(seqNr).stream().filter(x -> x.digest ==  checkPoint.digest).count();
            if (count >= 2*fCount+1) {
                checkpoints.keySet().stream()
                           .filter(x -> x <= seqNr)
                           .collect(Collectors.toSet())
                           .forEach(x -> checkpoints.remove(x));
            }
            /* TODO: update watermarks */
        } else {
            HashSet<CheckPoint> tmp = new HashSet<>();
            tmp.add(checkPoint);
            
            checkpoints.put(checkPoint.lastSequenceNr, tmp);
        }
    }
    
    public int getReplicaId() {
        return replicaId;
    }
}
