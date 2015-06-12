package at.archistar.bft.message;

import at.archistar.bft.replica.message.ClientCommand;
import at.archistar.bft.replica.message.ClientCommandResult;
import at.archistar.bft.replica.message.CheckPoint;
import at.archistar.bft.executor.message.ExecutedWithState;
import at.archistar.bft.views.Prepare;
import at.archistar.bft.views.Commit;
import at.archistar.bft.views.CommitBuilder;
import at.archistar.bft.views.PrepareBuilder;
import at.archistar.bft.views.Preprepare;
import at.archistar.bft.views.PreprepareBuilder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * 
 * @author andy
 */
public class CommonMessageBuilder {

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
        byte[] newMac = PreprepareBuilder.createPreprepareMAC(key, cmd.sequenceNr, cmd.viewNr, cmd.digest);
        
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }
    
    public static void validate(Prepare cmd) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", "rcpt");
        byte[] newMac = PrepareBuilder.createPrepareMAC(key, cmd.sequenceNr, cmd.viewNr, cmd.digest);
        
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }
    
    public static void validate(Commit cmd) {
        KeyParameter key = PseudoKeyManager.keyFromTo("this", "rcpt");
        byte[] newMac = CommitBuilder.createCommitMAC(key, cmd.sequenceNr, cmd.viewNr);
        
        if (!Arrays.equals(newMac, cmd.mac)) {
            throw new RuntimeException("MAC not equal!");   
        }
    }

    public static CheckPoint createCheckpoint(String rcpt, ExecutedWithState cmd) {
         /* create mac */
        Poly1305 mac = new Poly1305();
        mac.init(PseudoKeyManager.keyFromTo("this", rcpt));
 
        mac.update(ClientCommandResult.command, 0, ClientCommandResult.command.length);
        byte[] bytes = ByteBuffer.allocate(4).putInt(cmd.sequenceNr).array();
        mac.update(bytes, 0, bytes.length);
        byte[] tmp = cmd.stateDigest;
        mac.update(tmp, 0, tmp.length);
        byte[] macByte = new byte[16];
        mac.doFinal(macByte, 0);
        
        return new CheckPoint(cmd.sequenceNr, cmd.stateDigest, macByte);
    }
}
