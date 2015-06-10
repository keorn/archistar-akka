package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.Execute;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * TODO: sequence numbers must be dense
 * 
 * @author andy
 */
public class Executor extends UntypedActor {

    private final int replicaId;
    
    /* initial message has number 1 */
    private int lastExecuted = 0;
    
    private final Map<Integer, Execute> cmdQueue = new HashMap<>();
    private final Map<Integer, ActorRef> cmdSender = new HashMap<>();
    
    static Props props(int replicaId) {
        return Props.create(new Creator<Executor>() {
           @Override
           public Executor create() throws Exception {
               return new Executor(replicaId);
           }
        });
    }
    
    Executor(int replicaId) {
        this.replicaId = replicaId;
    }
    
    private void execute(Execute cmd, ActorRef sender) {
        System.err.println("\nreplica[" + replicaId + "|" + cmd.getSequenceNr() + " EXECUTE " + cmd.getCommand());
        lastExecuted++;
        System.err.println("sending message back to " + sender);
        getSender().tell("something was executed", getSelf());        
    }
    
    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof Execute) {
            Execute cmd = (Execute)o;
            
            if (cmd.getSequenceNr() == lastExecuted + 1) {
                /* command sequence numbers are dense */
                execute(cmd, getSender());
                
                /* test if commands are queued */
                while (cmdQueue.containsKey(lastExecuted)) {
                    execute(cmdQueue.get(lastExecuted), cmdSender.get(lastExecuted));
                    cmdQueue.remove(lastExecuted);
                    cmdSender.remove(lastExecuted);
                }
            } else if (cmd.getSequenceNr() > lastExecuted) {
                System.err.println("queuing message " + cmd.getSequenceNr());
                cmdQueue.put(cmd.getSequenceNr(), cmd);
                cmdSender.put(cmd.getSequenceNr(), getSender());
            } else {
                System.err.println("discarding message " + cmd.getSequenceNr());
            }
        } else {
            unhandled(o);
        }
    }
}
