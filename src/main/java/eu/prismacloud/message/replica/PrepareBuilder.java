package eu.prismacloud.message.replica;

import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.PseudoKeyManager;
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 *
 * @author andy
 */
public class PrepareBuilder implements MessageBuilder {
        
    private final int sequenceNr;
    
    private final int viewNr;
    
    private final byte[] digest;
        
    public PrepareBuilder(int sequenceNr, int viewNr, byte[] digest) {
        this.sequenceNr = sequenceNr;
        this.viewNr = viewNr;
        this.digest = digest;
    }
    
    @Override
    public Prepare buildFor(String rcpt) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createPrepareMAC(key, sequenceNr, viewNr, digest);
        return new Prepare(sequenceNr, viewNr, digest, mac);
    }
    
    public static byte[] createPrepareMAC(KeyParameter key, int sequenceNr, int view, byte[] digest) {
        
        /* create mac */
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", "rcpt"));
 
        mac.update(Prepare.command, 0, Prepare.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(4).putInt(view).array();
        mac.update(bytes, 0, bytes.length);
        mac.update(digest, 0, digest.length);
        byte[] macByte = new byte[16];
        
        mac.doFinal(macByte, 0);
        return macByte;
    }
}
