package eu.prismacloud.worker;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import static akka.testkit.JavaTestKit.duration;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import eu.prismacloud.message.execution.Execute;
import eu.prismacloud.message.execution.ExecutedWithState;
import eu.prismacloud.message.execution.ExecutionCompleted;
import eu.prismacloud.message.replica_state.ExecutorReady;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author andy
 */
public class ExecutorTest {
    static ActorSystem system;
    
    private final int replicaNr = 1;
    
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
    public void itShouldSendExecutorReady() {
        TestProbe proxy = new TestProbe(system);
        final TestActorRef<Executor> ref = TestActorRef.create(system,
                                                           Executor.props(replicaNr, proxy.ref()));
        proxy.expectMsgClass(duration("1 seconds"), ExecutorReady.class);
    }

    @Test
    public void itSendsTheResultToTheSender() {
        
        TestProbe proxy = new TestProbe(system);
        final TestActorRef<Executor> ref = TestActorRef.create(system,
                                                           Executor.props(replicaNr, proxy.ref()));
        final String operation = "the operation";
        final int seqNr = 1;
        
        new JavaTestKit(system) {{
            final Execute execute = new Execute(seqNr, operation);
            
            ref.tell(execute, getRef());
            expectMsgClass(duration("1 seconds"), ExecutionCompleted.class);
        }};
    }
    
    @Test
    public void itSendsExecutedWithStateMessageToParentAfterKExecutions() {
        
        TestProbe proxy = new TestProbe(system);
        final TestActorRef<Executor> ref = TestActorRef.create(system,
                                                           Executor.props(replicaNr, proxy.ref()));
        proxy.expectMsgClass(ExecutorReady.class);
        
        final String operation = "the operation";
        
        new JavaTestKit(system) {{
            for(int i = 1; i <= Executor.CHECKPOINT_INTERVAL + 1; i++) {
                ref.tell(new Execute(i, operation), getRef());
            }
            proxy.expectMsgClass(ExecutedWithState.class);
        }};
    }
}
