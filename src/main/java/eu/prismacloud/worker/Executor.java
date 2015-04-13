package eu.prismacloud.worker;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.Execute;

/**
 *
 * TODO: sequence numbers must be dense
 * 
 * @author andy
 */
public class Executor extends UntypedActor {

    private final int replicaId;
    
    private int lastExecuted = -1;
    
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
    
    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof Execute) {
            Execute cmd = (Execute)o;
            
            if (cmd.getSequenceNr() > lastExecuted) {
                System.err.println("\nreplica[" + replicaId + "|" + cmd.getSequenceNr() + " EXECUTE " + cmd.getCommand());
                lastExecuted = cmd.getSequenceNr();
                System.err.println("sending message back to " + getSender());
                getSender().tell("something was executed", getSelf());
            } else {
                /* TODO: send error back to client? */
            }
        } else {
            unhandled(o);
        }
    }
}
