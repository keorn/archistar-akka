package at.archistar.bft.views;

/**
 *
 * @author andy
 */
public class Prepare extends ViewMessage {
    
    public final byte[] digest;
    
    public final byte[] mac;
    
    public final static byte[] command = "PREPARE".getBytes();
    
    Prepare(int sequenceNr, int view, byte[] digest, byte[] mac) {
        super(view, sequenceNr);
        this.digest= digest;
        this.mac = mac;
    }
}
