package eu.prismacloud.message.execution;

/**
 *
 * @author ait
 */
public class ExecutionCompleted {

    public ExecutionCompleted(int sequenceNr) {
        this.sequenceNr = sequenceNr;
    }
    
    public final int sequenceNr;
}
