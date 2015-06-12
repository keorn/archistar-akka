package at.archistar.bft.views;

import at.archistar.bft.message.MessageBuilder;
import at.archistar.bft.message.PseudoKeyManager;
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 *
 * @author andy
 */
public class CommitBuilder implements MessageBuilder {
    
    private final int sequenceNr;
    
    private final int view;
        
    public CommitBuilder(int sequenceNr, int view) {
        this.sequenceNr = sequenceNr;
        this.view = view;
    }
    
    public static byte[] createCommitMAC(KeyParameter key, int sequenceNr, int view) {

        Poly1305 mac = new Poly1305();
        mac.init(key);
 
        mac.update(Commit.command, 0, Commit.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(4).putInt(view).array();
        mac.update(bytes, 0, bytes.length);
        byte[] macByte = new byte[16];
        
        mac.doFinal(macByte, 0);
        return macByte;
    }
    
    @Override
    public Commit buildFor(String rcpt) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createCommitMAC(key, sequenceNr, view);        
        return new Commit(sequenceNr, view, mac);
    }
}
