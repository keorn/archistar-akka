package at.archistar.bft.views;

import at.archistar.bft.message.MessageBuilder;
import at.archistar.bft.message.PseudoKeyManager;
import at.archistar.bft.replica.message.ClientCommand;
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 *
 * @author andy
 */
public class PreprepareBuilder implements MessageBuilder {
    
    private final int sequenceNr;
    
    private final int clientSequence;
    
    private final int view;
            
    private final ClientCommand client;
    
    public PreprepareBuilder(int sequenceNr, int view, ClientCommand cmd) {
        this.sequenceNr = sequenceNr;
        this.clientSequence = cmd.sequenceId;
        this.view = view;
        this.client = cmd;
    }
    
    @Override
    public Preprepare buildFor(String rcpt) {        
        /* create digest */
        Digest digestGen = new SHA512Digest();
        digestGen.update(Preprepare.command, 0, Preprepare.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(client.sequenceId).array();
        digestGen.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(8).putLong(client.timestamp).array();
        digestGen.update(bytes, 0, bytes.length);
        byte[] digest = new byte[64];
        digestGen.doFinal(digest, 0);

        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createPreprepareMAC(key, sequenceNr, view, digest);
        
        return new Preprepare(sequenceNr, client.sequenceId, view, digest, mac);
    }
    
    public static byte[] createPreprepareMAC(KeyParameter key, int sequenceNr, int view, byte[] digest) {
        byte[] bytes;
        
        Poly1305 mac = new Poly1305();
        mac.init(key);
 
        mac.update(Preprepare.command, 0, Preprepare.command.length);
        bytes = ByteBuffer.allocate(4).putInt(sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(4).putInt(view).array();
        mac.update(bytes, 0, bytes.length);
        mac.update(digest, 0, digest.length);
        byte[] macByte = new byte[16];
        mac.doFinal(macByte, 0);

        return macByte;
    }
    
    public Preprepare buildFakeSelfPreprepare() {
        return new Preprepare(sequenceNr, client.sequenceId, view, null, null);
    }
}
