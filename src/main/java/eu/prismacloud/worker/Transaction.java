package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.Commit;
import eu.prismacloud.message.Execute;
import eu.prismacloud.message.Prepare;
import eu.prismacloud.message.Preprepare;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author andy
 */
public class Transaction extends UntypedActor {
    
    private enum STATE {
        INITIALIZING,
        PREPREPARED,
        PREPARED,
        COMMITED
    }
    
    private STATE state;

    private final int fCount;
    
    private final int sequenceNr;
    
    private ActorRef client;
    
    private ClientCommand clientCommand;
    
    private final Set<ActorSelection> peers;
    
    private final Set<Prepare> prepareCommands;
    
    private final Set<Commit> commitCommands;
    
    private final  ActorRef executor;
    
    private final int replicaId;
        
    private boolean prepreparedReceived = false;

    public static Props props(boolean primary, boolean prepreparedReceived, int replicaId, ActorRef executor, Set<ActorSelection> peers, int f, int sequenceNr, ClientCommand cmd, final ActorRef client) {
        return Props.create(new Creator<Transaction>() {
           @Override
           public Transaction create() throws Exception {
                System.err.println("replica " + replicaId + " set CLIENT to " +client);
               return new Transaction(primary, prepreparedReceived, replicaId, executor, peers, f, sequenceNr, cmd, client);
           }
        });
    }
    
    private Transaction(boolean primary, boolean prepreparedReceived, int replicaId, ActorRef executor, Set<ActorSelection> peers, int f, int sequenceNr, ClientCommand cmd, ActorRef client) {
        this.fCount = f;
        this.client = client;
        this.sequenceNr = sequenceNr;
        this.clientCommand = cmd;
        this.peers = peers;
        this.prepareCommands = new HashSet<>();
        this.commitCommands = new HashSet<>();
        this.executor = executor;
        this.replicaId = replicaId;
        this.prepreparedReceived  = true;
 
        if (primary) {
            System.err.println("replica " + replicaId + " init as PREPARED");
            state = STATE.PREPARED;
            assert(prepreparedReceived == true);
            sendMessageToPeers(new Preprepare(sequenceNr, cmd.getSequenceId()));
        } else if (!primary && prepreparedReceived) {
            System.err.println("replica " + replicaId + " init as PREPREPARED");
            state = STATE.PREPREPARED;
            sendMessageToPeers(new Prepare(this.sequenceNr));
        } else {
            System.err.println("replica " + replicaId + " init as INITIALIZING");
            state = STATE.INITIALIZING;
        }
    }
    
    private void sendMessageToPeers(Object message) {
        peers.parallelStream()
             .forEach(f -> f.tell(message, getSelf()));
    }
    
    private void checkState() {
        if (state == STATE.INITIALIZING && prepreparedReceived && clientCommand != null) {
            System.err.println("replica " + replicaId + " INIT -> PREPREPARED, sending PREPARE message");
            state = STATE.PREPREPARED;
            sendMessageToPeers(new Prepare(this.sequenceNr));
        }
        if (state == STATE.PREPREPARED && prepareCommands.size() == 2*fCount) {
            System.err.println("replica " + replicaId + " PREPREPARED -> PREPARED, sending PREPARE message");
            commitCommands.add(new Commit(sequenceNr));
            sendMessageToPeers(new Commit(sequenceNr));
            state = STATE.PREPARED;
        }
        if (state == STATE.PREPARED && commitCommands.size() == (2*fCount + 1)) {
            System.err.println("replica " + replicaId + " PREPARED -> EXECUTE");
            this.executor.tell(new Execute(sequenceNr, this.clientCommand.getCommand()), this.client);
        }
    }
    
    @Override
    public void onReceive(Object o) throws Exception {
        
        System.err.println("Transaction[" + replicaId + "|" + sequenceNr + "] got message " + o);
        
        if (o instanceof ClientCommand) {
            this.clientCommand = (ClientCommand)o;
            System.err.println("replica " + replicaId + " set CLIENT (through client command) to " +client);
            this.client = getSender();
            checkState();
        } else if (o instanceof Preprepare) {
            this.prepreparedReceived = true;
            checkState();
        } else if (o instanceof Prepare) {
            prepareCommands.add((Prepare)o);
            checkState();
        } else if (o instanceof Commit) {
            commitCommands.add((Commit)o);
            checkState();
        } else {
            unhandled(o);
        }
    }
    
    public int getSequenceNr() {
        return this.sequenceNr;
    }
}
