package at.archistar.bft;

import at.archistar.bft.replica.Replica;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import at.archistar.bft.replica.message.Configure;
import at.archistar.bft.message.CommonMessageBuilder;
import at.archistar.bft.replica.message.ClientCommandResult;
import at.archistar.bft.replica.message.ReplicaConfigured;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import scala.concurrent.duration.Duration;

public class AppTest {

    private static final int replicaCount = 4;

    private final Timeout timeout = new Timeout(Duration.create(4, "seconds"));

    private Set<ActorRef> createReplicas(ActorSystem system, int replicaCount) {
        Set<ActorRef> replicas = new HashSet<>();
        for (int i = 0; i < replicaCount; i++) {
            replicas.add(system.actorOf(Replica.props(i + 1, i == 0), "main-actor-" + (i + 1)));
        }
        return replicas;
    }
    
    @Test
    public void testSetup() throws Exception {

        ActorSystem system = ActorSystem.create("System_1");
        
        Set<ActorRef> replicas = createReplicas(system, replicaCount);

        new JavaTestKit(system) {
            {
                replicas.parallelStream()
                    .forEach(f -> f.tell(Configure.fromReplicas(replicas), getRef()));
                for(int i = 0; i < replicas.size(); i++) {
                    expectMsgClass(timeout.duration(), ReplicaConfigured.class);
                }
            }
        };
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testClientRequest() throws Exception {

        ActorSystem system = ActorSystem.create("System_2");
        Set<ActorRef> replicas = createReplicas(system, replicaCount);

        new JavaTestKit(system) {
            {
                replicas.parallelStream()
                    .forEach(f -> f.tell(Configure.fromReplicas(replicas), getRef()));
                for(int i = 0; i < replicas.size(); i++) {
                    expectMsgClass(timeout.duration(), ReplicaConfigured.class);
                }


                replicas.parallelStream().forEach(ref -> ref.tell(CommonMessageBuilder.createRequest(ref.path().name(), 1, "fubar"), getRef()));
                for(int i = 0; i < replicas.size(); i++) {
                    expectMsgClass(timeout.duration(), ClientCommandResult.class);
                }
            }
        };
        JavaTestKit.shutdownActorSystem(system);
    }
}
