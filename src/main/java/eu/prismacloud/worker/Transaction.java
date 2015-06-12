package eu.prismacloud.worker;

import akka.actor.ActorRef;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.replica.Commit;
import eu.prismacloud.message.execution.ExecuteBuilder;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.replica.CommitBuilder;
import eu.prismacloud.message.replica.Prepare;
import eu.prismacloud.message.replica.PrepareBuilder;
import eu.prismacloud.message.replica.Preprepare;
import eu.prismacloud.message.replica.PreprepareBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author andy
 */
public class Transaction {
    
    private static final Logger log = Logger.getLogger("global");

    public static enum STATE {
        INITIALIZING,
        PREPREPARED,
        PREPARED,
        COMMITED
    }
    
    private STATE state;

    private final int fCount;
    
    private final int sequenceNr;
        
    private ClientCommand clientCommand;
        
    private final Set<Prepare> prepareCommands;
    
    private final Set<Commit> commitCommands;
        
    private final int replicaId;
        
    private Preprepare preprepare;
    
    private final int viewNr = 1;
    
    private final boolean primary;

    public Transaction(boolean primary, int replicaId, int f, int sequenceNr) {
        this.fCount = f;
        this.sequenceNr = sequenceNr;
        this.prepareCommands = new HashSet<>();
        this.commitCommands = new HashSet<>();
        this.replicaId = replicaId;
        this.state = STATE.INITIALIZING;
        this.primary = primary;
        
        log.log(Level.FINER, "replica {0} init as INITIALIZING", replicaId);
    }
    
    private void checkState(Consumer<MessageBuilder> peers, Consumer<ExecuteBuilder> exec) {
        
        System.err.println("replica: " + replicaId + " state: " + state + " preprepared: " + (preprepare != null) + " prepare-msg#: " + prepareCommands.size() + " commit-msg#: " + commitCommands.size());
        
        if (primary && state == STATE.INITIALIZING && clientCommand != null) {
            log.log(Level.FINER, "replica {0} INIT -> PREPARED(master)", replicaId);
            state = STATE.PREPARED;
            PreprepareBuilder builder = new PreprepareBuilder(sequenceNr, viewNr, clientCommand);
            
            this.preprepare = builder.buildFakeSelfPreprepare();
            peers.accept(builder);
        }
        if (!primary && state == STATE.INITIALIZING && (preprepare != null) && clientCommand != null) {
            log.log(Level.FINER, "replica {0} INIT -> PREPREPARED, sending PREPARE message", replicaId);
            state = STATE.PREPREPARED;
            peers.accept(new PrepareBuilder(preprepare.sequenceNr, preprepare.viewNr, preprepare.digest));
        }
        
        if (state == STATE.PREPREPARED && prepareCommands.size() == 2*fCount) {
            log.log(Level.FINER, "replica {0} PREPREPARED -> PREPARED, sending COMMIT message", replicaId);
            CommitBuilder builder = new CommitBuilder(sequenceNr, viewNr);
            commitCommands.add(builder.buildFor("self"));
            state = STATE.PREPARED;
            peers.accept(builder);
        }
        if (state == STATE.PREPARED && commitCommands.size() == (2*fCount + 1)) {
            log.log(Level.WARNING, "replica {0} PREPARED -> EXECUTE", replicaId);
            state = STATE.COMMITED;
            exec.accept(new ExecuteBuilder(sequenceNr, this.clientCommand.operation, this.clientCommand.getSender()));
        }
        
        System.err.println("replica: " + replicaId + "(A) state: " + state + " preprepared: " + (preprepare != null) + " prepare-msg#: " + prepareCommands.size() + " commit-msg#: " + commitCommands.size());
    }
    
    public void addClientCommand(ClientCommand cmd, Consumer<MessageBuilder> peers, Consumer<ExecuteBuilder> exec) {
        this.clientCommand = cmd;
        checkState(peers, exec);        
    }
    
    public void addPrepare(Prepare cmd, Consumer<MessageBuilder> peers, Consumer<ExecuteBuilder> exec) {
        prepareCommands.add(cmd);
        checkState(peers, exec);
    }
    
    public void addPreprepare(Preprepare cmd, Consumer<MessageBuilder> peers, Consumer<ExecuteBuilder> exec) {
        this.preprepare = cmd;
        checkState(peers, exec);
    }
    
    public void addCommit(Commit cmd, Consumer<MessageBuilder> peers, Consumer<ExecuteBuilder> exec) {
        commitCommands.add(cmd);
        checkState(peers, exec);
    }

    STATE getState() {
        return this.state;
    }
    
    public ActorRef getClient() {
        return this.clientCommand.getSender();
    }
    
    int getClientSeq() {
        return this.clientCommand.sequenceId;
    }
}
