package eu.prismacloud.message.replica;

/**
 *
 * @author andy
 */
public abstract class ReplicaMessage {
    
    public final int viewNr;
    
    public final int sequenceNr;
    
    ReplicaMessage(int viewNr, int sequenceNr) {
        this.viewNr = viewNr;
        this.sequenceNr = sequenceNr;
    }
}
