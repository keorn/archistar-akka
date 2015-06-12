package at.archistar.bft.replica.message;

/**
 *
 * @author andy
 */
public class CheckPoint {
 
    public final int lastSequenceNr;
    
    public final byte[] mac;
    
    public final byte[] digest;
    
    public final static byte[] command = "COMMIT".getBytes();
    
    public CheckPoint(int lastSequenceNr, byte[] digest, byte[] mac) {
        this.lastSequenceNr = lastSequenceNr;
        this.digest = digest;
        this.mac = mac;
    }
}
