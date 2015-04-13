package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.japi.Procedure;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.Commit;
import eu.prismacloud.message.Configure;
import eu.prismacloud.message.CreateTransaction;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.Prepare;
import eu.prismacloud.message.Preprepare;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import scala.Option;

public class Replica extends UntypedActor {
    
    private Set<ActorSelection> peers;
    
    private final int replicaId;
    
    private final int fCount = 1;
    
    private final boolean master;
    
    private int seqCounter = 0;
    
    private final HashMap<Integer, ClientCommand> clientMap = new HashMap<>();
    
    private final HashMap<Integer, Preprepare> preprepareMap = new HashMap<>();
    
    private final ActorRef executor;
    
    private final int viewNr = 1;
    
    /**
     * creates a transaction and forwards the message to the transaction
     */
    private Procedure<Object> configured = (Object message) -> {
        
        if (message instanceof ClientCommand) {
            ClientCommand cmd = (ClientCommand)message;
            
            MessageBuilder.validate(cmd);
            cmd.setSender(getSender());
            
            if (this.isMaster()) {
                int newSeq = ++seqCounter;
                getViewFor(viewNr).tell(new CreateTransaction(MessageBuilder.crateFakeSelfPreprepare(newSeq, viewNr, cmd),
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
            MessageBuilder.validate(cmd);
            
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
            MessageBuilder.validate(cmd);
            getViewFor(cmd.view).tell(cmd, ActorRef.noSender());
       } else if (message instanceof Commit) {
            Commit cmd = (Commit)message;
            MessageBuilder.validate(cmd);
            getViewFor(cmd.view).tell(cmd, ActorRef.noSender());
        } else {
            unhandled(message);
        }
    };
    
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
        this.executor = getContext().actorOf(Executor.props(replicaId));
    }
    
    @Override
    public void onReceive(Object message) {
        if (message instanceof Configure) {
            Configure config = (Configure)message;
            peers = config.getPeers().stream()
                                     .filter(f -> !f.equalsIgnoreCase(getSelf().path().toStringWithoutAddress()))
                                     .map(f -> context().actorSelection(f))
                                     .collect(Collectors.toSet());
            
            /* BUG: should we wait until the View is ready? */
            /* TODO: there should be only one active view -> can't we check against this? */
            getContext().actorOf(View.props(viewNr, isMaster(), replicaId, executor, peers), "view-" + viewNr);

            /* become "normal" listener loop */
            getContext().become(configured);
        } else {
            unhandled(message);
        }
    }
}
