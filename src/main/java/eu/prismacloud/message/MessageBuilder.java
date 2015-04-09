package eu.prismacloud.message;

import java.nio.ByteBuffer;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.Poly1305;

/**
 * 
 * @author andy
 */
public class MessageBuilder {

    /**
     * 
     * @param operation the Operation (as in PBFT)
     * @param clientSequence an internal client-sequence id which should allow
     *                       for easier per-client parallel command submission
     * @return a newly build client command 
     */
    public static ClientCommand createRequest(String rcpt, int clientSequence, String operation) {
        final long timestamp = System.currentTimeMillis();
        
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", rcpt));
        
        mac.update(ClientCommand.command, 0, ClientCommand.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(clientSequence).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(8).putLong(timestamp).array();
        mac.update(bytes, 0, bytes.length);
        
        byte[] theMac = new byte[16];
        mac.doFinal(theMac, 0);

        return new ClientCommand(clientSequence, operation, timestamp, theMac);
    }
    
    public static Preprepare createPreprepare(String rcpt, int sequenceNr, ClientCommand client) {
        
        /* currently we only support one single view */
        final int view = 1;
        
        /* create digest */
        Digest digestGen = new SHA512Digest();
        digestGen.update(ClientCommand.command, 0, ClientCommand.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(client.sequenceId).array();
        digestGen.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(8).putLong(client.timestamp).array();
        digestGen.update(bytes, 0, bytes.length);
        byte[] digest = new byte[64];
        digestGen.doFinal(digest, 0);

        /* create mac */
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", rcpt));
 
        mac.update(Preprepare.command, 0, Preprepare.command.length);
        bytes = ByteBuffer.allocate(4).putInt(sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(4).putInt(view).array();
        mac.update(bytes, 0, bytes.length);
        mac.update(digest, 0, digest.length);
        byte[] macByte = new byte[16];
        mac.doFinal(macByte, 0);
        
        return new Preprepare(sequenceNr, client.sequenceId, view, digest, macByte);
    }
    
    public static Preprepare crateFakeSelfPreprepare(int sequenceNr, int view, ClientCommand client) {
        return new Preprepare(sequenceNr, client.sequenceId, view, null, null);
    }

    public static Prepare createPrepare(String rcpt, Preprepare cmd) {
        
        /* create mac */
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", rcpt));
 
        mac.update(Prepare.command, 0, Prepare.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(cmd.sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(4).putInt(cmd.view).array();
        mac.update(bytes, 0, bytes.length);
        mac.update(cmd.digest, 0, cmd.digest.length);
        byte[] macByte = new byte[16];
        mac.doFinal(macByte, 0);
        
        return new Prepare(cmd.sequenceNr, cmd.view, cmd.digest, macByte);
    }
    
    public static Commit createCommit(String rcpt, int sequenceNr, int view) {
        
        /* create mac */
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", rcpt));
 
        mac.update(Commit.command, 0, Commit.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(4).putInt(view).array();
        mac.update(bytes, 0, bytes.length);
        byte[] macByte = new byte[16];
        mac.doFinal(macByte, 0);
        
        return new Commit(sequenceNr, view, macByte);
    }
    
    public static ClientCommandResult createClientCommandResult(String rcpt, int clientSeqNr, String result) {
        
        /* create mac */
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", rcpt));
 
        mac.update(ClientCommandResult.command, 0, ClientCommandResult.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(clientSeqNr).array();
        mac.update(bytes, 0, bytes.length);
        byte[] tmp = result.getBytes();
        mac.update(tmp, 0, tmp.length);
        byte[] macByte = new byte[16];
        mac.doFinal(macByte, 0);
        
        return new ClientCommandResult(result, clientSeqNr, macByte);
    }
}
