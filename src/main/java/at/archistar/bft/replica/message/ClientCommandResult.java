package at.archistar.bft.replica.message;

/**
 *
 * @author andy
 */
public class ClientCommandResult {
    
    public final static byte[] command = "REPLY".getBytes();
    
    public final String result;
    
    public final int clientSequence;
    
    public final byte[] mac;
    
    public ClientCommandResult(String result, int clientSequence, byte[] mac) {
        this.result = result;
        this.mac = mac;
        this.clientSequence = clientSequence;
    }
}
