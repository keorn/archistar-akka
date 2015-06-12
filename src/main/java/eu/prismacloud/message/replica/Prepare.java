package eu.prismacloud.message.replica;

/**
 *
 * @author andy
 */
public class Prepare extends ReplicaMessage {
    
    public final byte[] digest;
    
    public final byte[] mac;
    
    public final static byte[] command = "PREPARE".getBytes();
    
    Prepare(int sequenceNr, int view, byte[] digest, byte[] mac) {
        super(view, sequenceNr);
        this.digest= digest;
        this.mac = mac;
    }
}
