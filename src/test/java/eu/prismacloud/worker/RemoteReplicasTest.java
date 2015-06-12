package eu.prismacloud.worker;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import eu.prismacloud.message.replica_state.RemoteReplicasReady;
import java.util.HashSet;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author andy
 */
public class RemoteReplicasTest {
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
    public void remoteReplicasShouldReturnASuccesWhenEmpty() {
        
        final Set<ActorSelection> peers = new HashSet<>();
        
        new JavaTestKit(system) {{
            
            TestProbe proxy = new TestProbe(system);
            
            final TestActorRef<RemoteReplicas> ref = TestActorRef.create(system,
                                                     RemoteReplicas.props(peers, proxy.ref()));
            
            proxy.expectMsgClass(RemoteReplicasReady.class);
        }};
    }
}
