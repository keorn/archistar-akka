package eu.prismacloud.worker;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.MessageBuilder;
import java.util.HashSet;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author andy
 */
public class TransactionTest {
   
    static ActorSystem system;
    
    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }
    
    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }
    
    @Test
    public void transactionShouldBeInitializingAfterCreation() {
        final HashSet<ActorSelection> peers = new HashSet<>();        
        final TestActorRef<Transaction> ref = TestActorRef.create(system,
                                                                  Transaction.props(false, 1,  null, peers, 1, 1));
        final Transaction actor = ref.underlyingActor();
        Assert.assertEquals(actor.getState(), Transaction.STATE.INITIALIZING);
    }
    
    @Test
    public void nonPrimaryTransactionShouldBePreparedAfter2f1Prepares() {
        final HashSet<ActorSelection> peers = new HashSet<>();
        final int f = 1;
        final int seqNr = 1;
        
        final TestActorRef<Transaction> ref = TestActorRef.create(system,
                                                                  Transaction.props(false, 1,  null, peers, f, seqNr));
        final Transaction actor = ref.underlyingActor();
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        new JavaTestKit(system) {{
            final ClientCommand initialRequest = MessageBuilder.createRequest(rcpt, clientSeqNr, operation);
            ref.tell(initialRequest, getRef());
            Assert.assertEquals(actor.getState(), Transaction.STATE.INITIALIZING);
            ref.tell(MessageBuilder.createPreprepare(rcpt, clientSeqNr, initialRequest), getRef());
            Assert.assertEquals(actor.getState(), Transaction.STATE.PREPREPARED);
        }};
    }
    
    @Test
    public void nonPrimaryShouldBePrepreparedAfterPreprepareAndClientCommand() {
        
        final HashSet<ActorSelection> peers = new HashSet<>();        
        final TestActorRef<Transaction> ref = TestActorRef.create(system,
                                                                  Transaction.props(false, 1,  null, peers, 1, 1));
        final Transaction actor = ref.underlyingActor();
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        new JavaTestKit(system) {{
            final ClientCommand initialRequest = MessageBuilder.createRequest(rcpt, clientSeqNr, operation);
            ref.tell(initialRequest, getRef());
            Assert.assertEquals(actor.getState(), Transaction.STATE.INITIALIZING);
            ref.tell(MessageBuilder.createPreprepare(rcpt, clientSeqNr, initialRequest), getRef());
            Assert.assertEquals(actor.getState(), Transaction.STATE.PREPREPARED);
        }};
    }
    
    @Test
    public void primaryShouldBePreparedAfterClientCommand() {
        
        final HashSet<ActorSelection> peers = new HashSet<>();
        
        final TestActorRef<Transaction> ref = TestActorRef.create(system,
                                                                  Transaction.props(true, 1,  null, peers, 1, 1));
        final Transaction actor = ref.underlyingActor();
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        
        new JavaTestKit(system) {{
            final ClientCommand initialRequest = MessageBuilder.createRequest(rcpt, clientSeqNr, operation);
            ref.tell(initialRequest, getRef());
            Assert.assertEquals(actor.getState(), Transaction.STATE.PREPARED);
        }};
    }
}
