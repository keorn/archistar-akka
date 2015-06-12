package eu.prismacloud.worker;

import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.CommonMessageBuilder;
import eu.prismacloud.message.replica.CommitBuilder;
import eu.prismacloud.message.replica.PrepareBuilder;
import eu.prismacloud.message.replica.Preprepare;
import eu.prismacloud.message.replica.PreprepareBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author andy
 */
public class TransactionTest {
   
    private final int f = 1;
    
    private final int seqNr = 1;
    
    private final int replicaNr = 1;
        
    @Test
    public void transactionShouldBeInitializingAfterCreation() {
        final Transaction tx = new Transaction(false, replicaNr,  f, seqNr);
        Assert.assertEquals(tx.getState(), Transaction.STATE.INITIALIZING);
    }
    
    @Test
    public void nonPrimaryTransactionShouldBePrepreparedAfter2f1Prepares() {
        
        final Transaction tx = new Transaction(false, replicaNr,  f, seqNr);

        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        final ClientCommand initialRequest = CommonMessageBuilder.createRequest(rcpt, clientSeqNr, operation);
        tx.addClientCommand(initialRequest, x -> {} , x -> {});
        Assert.assertEquals(tx.getState(), Transaction.STATE.INITIALIZING);
        tx.addPreprepare(new PreprepareBuilder(seqNr, 1, initialRequest).buildFor(rcpt), x -> {}, x -> {});
        Assert.assertEquals(tx.getState(), Transaction.STATE.PREPREPARED);
    }
    
    @Test
    public void primaryShouldBePreparedAfterClientCommand() {
        
        final Transaction tx = new Transaction(true, replicaNr, f, seqNr);
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        final ClientCommand initialRequest = CommonMessageBuilder.createRequest(rcpt, clientSeqNr, operation);
        tx.addClientCommand(initialRequest, x -> {}, x -> {});
        Assert.assertEquals(tx.getState(), Transaction.STATE.PREPARED);
    }
    
    @Test
    public void nonPrimaryShouldBePreparedAfterAdditional2fPrepares() {
        
        final Transaction tx = new Transaction(false, replicaNr, f, seqNr);
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        final ClientCommand initialRequest = CommonMessageBuilder.createRequest(rcpt, clientSeqNr, operation);
        tx.addClientCommand(initialRequest, x -> {}, x -> {});
            
        final Preprepare preprepare = new PreprepareBuilder(1, 1, initialRequest).buildFor(rcpt);
        tx.addPreprepare(preprepare, x -> {}, x -> {});
            
        for(int i = 0; i < 2 * f; i++) {
            Assert.assertEquals(tx.getState(), Transaction.STATE.PREPREPARED);
            tx.addPrepare(new PrepareBuilder(preprepare.sequenceNr, preprepare.viewNr, preprepare.digest).buildFor(rcpt), x -> {}, x -> {});
        }
        Assert.assertEquals(tx.getState(), Transaction.STATE.PREPARED);
    }
    
    @Test
    public void nonPrimaryShouldBeCommitedAfterAdditional2fCommits() {
        
        final Transaction tx = new Transaction(false, replicaNr, f, seqNr);
        
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        final ClientCommand initialRequest = CommonMessageBuilder.createRequest(rcpt, clientSeqNr, operation);
        tx.addClientCommand(initialRequest, x -> {}, x -> {});
            
        final Preprepare preprepare = new PreprepareBuilder(seqNr, 1, initialRequest).buildFor(rcpt);
        tx.addPreprepare(preprepare, x -> {}, x -> {});
            
        for(int i = 0; i < 2 * f; i++) {
            tx.addPrepare(new PrepareBuilder(seqNr, 1, preprepare.digest).buildFor(rcpt), x -> {}, x -> {});
        }
            
        for(int i = 0; i < 2 * f; i++) {
            Assert.assertEquals(tx.getState(), Transaction.STATE.PREPARED);
            tx.addCommit(new CommitBuilder(seqNr, 1).buildFor(rcpt), x -> {}, x -> {});
        }
        Assert.assertEquals(tx.getState(), Transaction.STATE.COMMITED);
    }
 }
