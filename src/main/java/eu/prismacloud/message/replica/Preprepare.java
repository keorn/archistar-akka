package eu.prismacloud.message.replica;

/**
 *
 * @author andy
 */
public class Preprepare extends ReplicaMessage {
    
    public final int clientSequence;
    
    public final byte[] digest;
    
    public final byte[] mac;
    
    public static final byte[] command = "PREPREPARE".getBytes();
    
    Preprepare(int sequenceNr, int clientSequence, int view, byte[] digest, byte[] mac) {
        super(view, sequenceNr);
        this.clientSequence = clientSequence;
        this.digest = digest;
        this.mac = mac;
    }
}
