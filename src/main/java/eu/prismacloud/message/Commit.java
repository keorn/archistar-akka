package eu.prismacloud.message;

/**
 *
 * @author andy
 */
public class Commit {
 
    public final int sequenceNr;
    
    public final int view;
    
    public final byte[] mac;
    
    public final static byte[] command = "COMMIT".getBytes();
    
    Commit(int sequenceNr, int view, byte[] mac) {
        this.sequenceNr = sequenceNr;
        this.view = view;
        this.mac = mac;
    }
}
