package eu.prismacloud.message;

/**
 *
 * @author andy
 */
public class Prepare {
    
    public final int sequenceNr;
    
    public final int view;
    
    public final byte[] digest;
    
    public final byte[] mac;
    
    public final static byte[] command = "PREPARE".getBytes();
    
    Prepare(int sequenceNr, int view, byte[] digest, byte[] mac) {
        this.sequenceNr = sequenceNr;
        this.view = view;
        this.digest= digest;
        this.mac = mac;
    }
}
