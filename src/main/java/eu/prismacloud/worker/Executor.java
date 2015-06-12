package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import eu.prismacloud.message.execution.Execute;
import eu.prismacloud.message.execution.ExecutedWithState;
import eu.prismacloud.message.execution.ExecutionCompleted;
import eu.prismacloud.message.replica_state.ExecutorReady;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * sequence numbers must be dense
 * 
 * @author andy
 */
public class Executor extends UntypedActor {

    private final int replicaId;
    
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    
    /* initial message has number 1 */
    private int lastExecuted = 0;
    
    private final Map<Integer, Execute> cmdQueue = new HashMap<>();
    
    private final Map<Integer, ActorRef> cmdSender = new HashMap<>();
    
    public static final int CHECKPOINT_INTERVAL = 128;
    
    private final ActorRef parent;
    
    static Props props(int replicaId, ActorRef parent) {
        return Props.create(new Creator<Executor>() {
           @Override
           public Executor create() throws Exception {
               return new Executor(replicaId, parent);
           }
        });
    }
    
    Executor(int replicaId, ActorRef parent) {
        this.replicaId = replicaId;
        this.parent = parent;
        
        /* TODO: initialize storage, etc. */
        
        this.parent.tell(new ExecutorReady(), ActorRef.noSender());
    }
    
    private void execute(Execute cmd, ActorRef sender) {
        log.info("replica[" + replicaId + "|" + cmd.sequenceNr + " EXECUTE " + cmd.command);
        lastExecuted++;
        log.warning("sending message back to " + sender);
        sender.tell(new ExecutionCompleted(cmd.sequenceNr), ActorRef.noSender());
        
        if (lastExecuted % CHECKPOINT_INTERVAL == 0) {
            log.warning("Sending ExecutedWithState to " + getContext().parent());
            parent.tell(new ExecutedWithState(lastExecuted, null), getSelf());
        }
    }
    
    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof Execute) {
            Execute cmd = (Execute)o;
            
            if (cmd.sequenceNr == lastExecuted + 1) {
                /* command sequence numbers are dense */
                execute(cmd, getSender());
                
                /* test if commands are queued */
                while (cmdQueue.containsKey(lastExecuted)) {
                    execute(cmdQueue.get(lastExecuted), cmdSender.get(lastExecuted));
                    cmdQueue.remove(lastExecuted);
                    cmdSender.remove(lastExecuted);
                }
            } else if (cmd.sequenceNr > lastExecuted) {
                log.warning("queuing message " + cmd.sequenceNr);
                cmdQueue.put(cmd.sequenceNr, cmd);
                cmdSender.put(cmd.sequenceNr, getSender());
            } else {
                log.warning("discarding message " + cmd.sequenceNr);
            }
        } else {
            unhandled(o);
        }
    }
}
