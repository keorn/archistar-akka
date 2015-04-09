package eu.prismacloud.message;

/**
 *
 * @author andy
 */
public class Preprepare extends ReplicaCommand {
    
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
