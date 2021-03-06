 package at.archistar.bft.views;

import at.archistar.bft.views.View;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import at.archistar.bft.replica.message.ClientCommand;
import at.archistar.bft.message.CommonMessageBuilder;
import at.archistar.bft.executor.message.Execute;
import at.archistar.bft.views.CommitBuilder;
import at.archistar.bft.views.PrepareBuilder;
import at.archistar.bft.views.Preprepare;
import at.archistar.bft.views.PreprepareBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author andy
 */
public class ViewTest {
    
    static ActorSystem system;
    
    private final int f = 1;
    
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
    public void viewShouldCallExecutor() {
        
        final JavaTestKit executor = new JavaTestKit(system);
        final JavaTestKit peers = new JavaTestKit(system);

        final int viewNr = 1;
        
        final TestActorRef<View> ref = TestActorRef.create(system,
                                                           View.props(viewNr, false, 1, executor.getRef(), peers.getRef()));
        final String rcpt = "rcpt";
        final int clientSeqNr = 1;
        final String operation = "the operation";
        final int seqNr = 1;
        final int fCount = 1;
        
        new JavaTestKit(system) {{
            
            final ClientCommand initialRequest = CommonMessageBuilder.createRequest(rcpt, clientSeqNr, operation);
            final Preprepare preprepare = new PreprepareBuilder(seqNr, viewNr, initialRequest).buildFor(rcpt);
            
            ref.tell(initialRequest, getRef());
            ref.tell(preprepare, getRef());
            
            for(int i = 0; i < 2 * f; i++) {
                ref.tell(new PrepareBuilder(seqNr, viewNr, preprepare.digest).buildFor(rcpt), getRef());
            }
            
            for(int i = 0; i < 2 * f; i++) {
                ref.tell(new CommitBuilder(seqNr, viewNr).buildFor(rcpt), getRef());
            }
            executor.expectMsgClass(duration("3 seconds"), Execute.class);
        }};
    }
}
