package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.Commit;
import eu.prismacloud.message.Execute;
import eu.prismacloud.message.MessageBuilder;
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
        
    private Preprepare preprepare;
    
    private final int viewNr = 1;

    public static Props props(boolean primary, Preprepare preprepare, int replicaId, ActorRef executor, Set<ActorSelection> peers, int f, int sequenceNr, ClientCommand cmd, final ActorRef client) {
        return Props.create(new Creator<Transaction>() {
           @Override
           public Transaction create() throws Exception {
                System.err.println("replica " + replicaId + " set CLIENT to " +client);
               return new Transaction(primary, preprepare, replicaId, executor, peers, f, sequenceNr, cmd, client);
           }
        });
    }
    
    private Transaction(boolean primary, Preprepare preprepare, int replicaId, ActorRef executor, Set<ActorSelection> peers, int f, int sequenceNr, ClientCommand cmd, ActorRef client) {
        this.fCount = f;
        this.client = client;
        this.sequenceNr = sequenceNr;
        this.clientCommand = cmd;
        this.peers = peers;
        this.prepareCommands = new HashSet<>();
        this.commitCommands = new HashSet<>();
        this.executor = executor;
        this.replicaId = replicaId;
        this.preprepare = preprepare;
 
        if (primary) {
            System.err.println("replica " + replicaId + " init as PREPARED");
            state = STATE.PREPARED;
            //sendMessageToPeers(new Preprepare(sequenceNr, cmd.sequenceId));
            this.preprepare = MessageBuilder.crateFakeSelfPreprepare(sequenceNr, viewNr, cmd);
            peers.parallelStream()
                 .forEach(x -> x.tell(MessageBuilder.createPreprepare(x.pathString(), sequenceNr, cmd), getSelf()));
        } else if (!primary && (preprepare != null)) {
            System.err.println("replica " + replicaId + " init as PREPREPARED");
            state = STATE.PREPREPARED;
            //sendMessageToPeers(new Prepare(this.sequenceNr));
            peers.parallelStream()
                 .forEach(x -> x.tell(MessageBuilder.createPrepare(x.pathString(), this.preprepare), getSelf()));
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
        if (state == STATE.INITIALIZING && (preprepare != null) && clientCommand != null) {
            System.err.println("replica " + replicaId + " INIT -> PREPREPARED, sending PREPARE message");
            state = STATE.PREPREPARED;
            //sendMessageToPeers(new Prepare(this.sequenceNr));
            assert(this.preprepare != null);
            peers.parallelStream()
                 .forEach(x -> x.tell(MessageBuilder.createPrepare(x.pathString(), this.preprepare), getSelf()));
        }
        if (state == STATE.PREPREPARED && prepareCommands.size() == 2*fCount) {
            System.err.println("replica " + replicaId + " PREPREPARED -> PREPARED, sending PREPARE message");
            commitCommands.add(MessageBuilder.createCommit("self", sequenceNr, viewNr));
            //sendMessageToPeers(new Commit(sequenceNr));
            peers.parallelStream()
                 .forEach(x -> x.tell(MessageBuilder.createCommit(x.pathString(), sequenceNr, viewNr), getSelf()));
            state = STATE.PREPARED;
        }
        if (state == STATE.PREPARED && commitCommands.size() == (2*fCount + 1)) {
            System.err.println("replica " + replicaId + " PREPARED -> EXECUTE");
            this.executor.tell(new Execute(sequenceNr, this.clientCommand.operation), this.client);
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
            this.preprepare = (Preprepare)o;
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
