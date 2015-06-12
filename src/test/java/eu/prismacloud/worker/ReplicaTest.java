package eu.prismacloud.worker;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import eu.prismacloud.message.replica_state.Configure;
import eu.prismacloud.message.replica_state.ReplicaConfigured;
import java.util.HashSet;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author andy
 */
public class ReplicaTest {
    static ActorSystem system;
    
    private final int f = 1;
    
    private final int replicaId = 1;
    
    private final boolean master = true;
    
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
    public void replicaShouldBeConfigured() {
        
        final TestActorRef<Replica> ref = TestActorRef.create(system,
                                          Replica.props(replicaId, master));
        
        final Set<String> peers = new HashSet<>();
        
        new JavaTestKit(system) {{
            ref.tell(new Configure(peers), getRef());
            expectMsgClass(ReplicaConfigured.class);
        }};
    }
}
