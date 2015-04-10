package eu.prismacloud.message;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;

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

        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createClientMessageMAC(key, clientSequence, timestamp);
        return new ClientCommand(clientSequence, operation, timestamp, mac);
    }
    
    private static byte[] createClientMessageMAC(KeyParameter key, int clientSequence, long timestamp) {
        Poly1305 mac = new Poly1305();
        mac.init(key);
        mac.update(ClientCommand.command, 0, ClientCommand.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(clientSequence).array();
        mac.update(bytes, 0, bytes.length);
        bytes = ByteBuffer.allocate(8).putLong(timestamp).array();
        mac.update(bytes, 0, bytes.length);

        byte[] theMac = new byte[16];
        mac.doFinal(theMac, 0);
        return theMac;
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

        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createPreprepareMAC(key, sequenceNr, view, digest);
        
        return new Preprepare(sequenceNr, client.sequenceId, view, digest, mac);
    }
    
    private static byte[] createPreprepareMAC(KeyParameter key, int sequenceNr, int view, byte[] digest) {
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
    
    public static Preprepare crateFakeSelfPreprepare(int sequenceNr, int view, ClientCommand client) {
        return new Preprepare(sequenceNr, client.sequenceId, view, null, null);
    }
    
    public static Prepare createPrepare(String rcpt, Preprepare cmd) {
        
        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createPreprepareMAC(key, cmd.sequenceNr, cmd.view, cmd.digest);
        return new Prepare(cmd.sequenceNr, cmd.view, cmd.digest, mac);
    }
    
    private static byte[] createPrepareMAC(KeyParameter key, int sequenceNr, int view, byte[] digest) {
        
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
    
    public static Commit createCommit(String rcpt, int sequenceNr, int view) {
        
        KeyParameter key = PseudoKeyManager.keyFromTo("this", rcpt);
        byte[] mac = createCommitMAC(key, sequenceNr, view);        
        return new Commit(sequenceNr, view, mac);
    }
    
    
    private static byte[] createCommitMAC(KeyParameter key, int sequenceNr, int view) {

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

    public static void validate(ClientCommand cmd) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", "rcpt");
        byte[] newMac = createClientMessageMAC(key, cmd.sequenceId, cmd.timestamp);
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }

    public static void validate(Preprepare cmd) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", "rcpt");
        byte[] newMac = createPreprepareMAC(key, cmd.sequenceNr, cmd.view, cmd.digest);
        
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }
    
    public static void validate(Prepare cmd) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", "rcpt");
        byte[] newMac = createPrepareMAC(key, cmd.sequenceNr, cmd.view, cmd.digest);
        
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }
    
    public static void validate(Commit cmd) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", "rcpt");
        byte[] newMac = createCommitMAC(key, cmd.sequenceNr, cmd.view);
        
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }
}
