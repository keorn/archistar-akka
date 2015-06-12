package at.archistar.bft.views;

/**
 *
 * @author andy
 */
public abstract class ViewMessage {
    
    public final int viewNr;
    
    public final int sequenceNr;
    
    ViewMessage(int viewNr, int sequenceNr) {
        this.viewNr = viewNr;
        this.sequenceNr = sequenceNr;
    }
}
