package eu.prismacloud.message.replica;

/**
 *
 * @author andy
 */
public class Commit extends ReplicaMessage {
 
    public final byte[] mac;
    
    public final static byte[] command = "COMMIT".getBytes();
    
    Commit(int sequenceNr, int view, byte[] mac) {
        super(view, sequenceNr);
        this.mac = mac;
    }
}