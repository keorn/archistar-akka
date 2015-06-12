package eu.prismacloud.message.replica;

/**
 *
 * @author andy
 */
public class Preprepare {
    
    public final int sequenceNr;
    
    public final int clientSequence;
    
    public final int view;
    
    public final byte[] digest;
    
    public final byte[] mac;
    
    public static final byte[] command = "PREPREPARE".getBytes();
    
    Preprepare(int sequenceNr, int clientSequence, int view, byte[] digest, byte[] mac) {
        this.sequenceNr = sequenceNr;
        this.clientSequence = clientSequence;
        this.view = view;
        this.digest = digest;
        this.mac = mac;
    }
}
