package eu.prismacloud;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.japi.Procedure;
import eu.prismacloud.messages.ClientCommand;
import eu.prismacloud.messages.Commit;
import eu.prismacloud.messages.Configure;
import eu.prismacloud.messages.Prepare;
import eu.prismacloud.messages.Preprepare;
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
    
    private final HashMap<Integer, ActorRef> transactionMap = new HashMap<>();
    
    private final HashMap<Integer, ActorRef> transactionMapClient = new HashMap<>();
    
    private final ActorRef executor;
    
    private ActorRef createTransaction(ClientCommand cmd, int seq, boolean prepreparedReceived) {
        return getContext().actorOf(Transaction.props(isMaster(), prepreparedReceived, replicaId, executor, peers, fCount, seq, cmd, getSender()));
    }
    
    private int getReplicaId() {
        return this.replicaId;
    }
    
    private Procedure<Object> configured = (Object message) -> {
        
        if (message instanceof ClientCommand) {
            ClientCommand cmd = (ClientCommand)message;
            if (this.isMaster()) {
                ActorRef t = createTransaction(cmd, ++seqCounter, true);
                transactionMap.put(seqCounter, t);
                transactionMapClient.put(cmd.getSequenceId(), t);
            } else {
                /* was the client id already mentioned before? */
                if (transactionMapClient.containsKey(cmd.getSequenceId())) {
                    transactionMapClient.get(cmd.getSequenceId()).tell(cmd, ActorRef.noSender());
                } else {
                    /* cannot create transaction without sequence count */
                    clientMap.put(cmd.getSequenceId(), cmd);
                }
            }
        } else if (message instanceof Preprepare) {
            Preprepare cmd = (Preprepare)message;            
            
            if (transactionMap.containsKey(cmd.getSequenceNr())) {
                ActorRef t = transactionMap.get(cmd.getClientSequence());
                transactionMapClient.put(cmd.getClientSequence(), t);
                
                t.tell(cmd, ActorRef.noSender());
                if (clientMap.containsKey(cmd.getClientSequence())) {
                    ClientCommand cCmd = clientMap.remove(cmd.getClientSequence());
                    t.tell(cCmd, ActorRef.noSender());
                }
            } else {
                ClientCommand cCmd = clientMap.remove(cmd.getClientSequence());
                ActorRef t = createTransaction(cCmd, cCmd.getSequenceId(), true);
                transactionMap.put(cmd.getSequenceNr(), t);
                transactionMapClient.put(cmd.getClientSequence(), t);
            }
        } else if (message instanceof Prepare) {
            Prepare cmd = (Prepare)message;
            ActorRef t = transactionMap.computeIfAbsent(cmd.getSequenceNr(),
                                                        f -> createTransaction(null, cmd.getSequenceNr(), false));
            t.tell(cmd, ActorRef.noSender());
        } else if (message instanceof Commit) {
            Commit cmd = (Commit)message;
            ActorRef t = transactionMap.computeIfAbsent(cmd.getSequenceNr(),
                                                        f -> createTransaction(null, cmd.getSequenceNr(), false));
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
