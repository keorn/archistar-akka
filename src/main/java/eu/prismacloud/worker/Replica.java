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
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.Prepare;
import eu.prismacloud.message.Preprepare;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class Replica extends UntypedActor {
    
    private Set<ActorSelection> peers;
    
    private final int replicaId;
    
    private final int fCount = 1;
    
    private final boolean master;
    
    private int seqCounter = 0;
    
    private final HashMap<Integer, ClientCommand> clientMap = new HashMap<>();
    
    private final HashMap<Integer, ActorRef> clientRefMap = new HashMap<>();
    
    private final HashMap<Integer, ActorRef> transactionMap = new HashMap<>();
    
    private final HashMap<Integer, ActorRef> transactionMapClient = new HashMap<>();
    
    private final ActorRef executor;
    
    private final int viewNr = 1;
    
    private ActorRef createTransaction(ClientCommand cmd, int seq, Preprepare preprepare, ActorRef sender) {
        return getContext().actorOf(Transaction.props(isMaster(), preprepare, replicaId, executor, peers, fCount, seq, cmd, sender));
    }
    
    private Procedure<Object> configured = (Object message) -> {
        
        if (message instanceof ClientCommand) {
            ClientCommand cmd = (ClientCommand)message;
            
            MessageBuilder.validate(cmd);
            
            if (this.isMaster()) {
                int newSeq = ++seqCounter;
                ActorRef t = createTransaction(cmd, newSeq, MessageBuilder.crateFakeSelfPreprepare(newSeq, viewNr, cmd), getSender());
                transactionMap.put(seqCounter, t);
                transactionMapClient.put(cmd.sequenceId, t);
            } else {
                /* was the client id already mentioned before? */
                if (transactionMapClient.containsKey(cmd.sequenceId)) {
                    transactionMapClient.get(cmd.sequenceId).tell(cmd, getSender());
                } else {
                    /* cannot create transaction without sequence count */
                    clientMap.put(cmd.sequenceId, cmd);
                    clientRefMap.put(cmd.sequenceId, getSender());
                }
            }
        } else if (message instanceof Preprepare) {
            Preprepare cmd = (Preprepare)message;
            
            MessageBuilder.validate(cmd);
            
            if (transactionMap.containsKey(cmd.sequenceNr)) {
                ActorRef t = transactionMap.get(cmd.clientSequence);
                transactionMapClient.put(cmd.clientSequence, t);
                
                t.tell(cmd, ActorRef.noSender());
                if (clientMap.containsKey(cmd.clientSequence)) {
                    ClientCommand cCmd = clientMap.remove(cmd.clientSequence);
                    t.tell(cCmd, clientRefMap.remove(cmd.clientSequence));
                }
            } else {
                ClientCommand cCmd = clientMap.remove(cmd.clientSequence);
                ActorRef t = createTransaction(cCmd, cCmd.sequenceId, cmd, clientRefMap.remove(cmd.clientSequence));
                transactionMap.put(cmd.sequenceNr, t);
                transactionMapClient.put(cmd.clientSequence, t);
            }
        } else if (message instanceof Prepare) {
            Prepare cmd = (Prepare)message;
            
            MessageBuilder.validate(cmd);
            
            ActorRef t = transactionMap.computeIfAbsent(cmd.sequenceNr,
                                                        f -> createTransaction(null, cmd.sequenceNr, null, ActorRef.noSender()));
            t.tell(cmd, ActorRef.noSender());
        } else if (message instanceof Commit) {
            Commit cmd = (Commit)message;
            
            MessageBuilder.validate(cmd);
            
            ActorRef t = transactionMap.computeIfAbsent(cmd.sequenceNr,
                                                        f -> createTransaction(null, cmd.sequenceNr, null, ActorRef.noSender()));
            t.tell(cmd, ActorRef.noSender());
        } else {
            unhandled(message);
        }
    };
    
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
            
            /* become "normal" listener loop */
            getContext().become(configured);
        } else {
            unhandled(message);
        }
    }
}
