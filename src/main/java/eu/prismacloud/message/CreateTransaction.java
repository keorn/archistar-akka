package eu.prismacloud.message;

/**
 *
 * @author andy
 */
public class CreateTransaction {
    
    final public Preprepare preprepare;
    
    final public int fCount;
    
    final public ClientCommand cmd;
    
    public CreateTransaction(Preprepare preprepare, int fCount, ClientCommand cmd) {
        this.preprepare = preprepare;
        this.fCount = fCount;
        this.cmd = cmd;
    }
}
