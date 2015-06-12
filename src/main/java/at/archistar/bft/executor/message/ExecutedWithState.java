package at.archistar.bft.executor.message;

/**
 *
 * @author ait
 */
public class ExecutedWithState {
    public final int sequenceNr;
    
    public final byte[] stateDigest;
    
    public ExecutedWithState(int sequenceNr, byte[] stateDigest) {
        this.sequenceNr = sequenceNr;
        this.stateDigest = stateDigest;
    }
}
