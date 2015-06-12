package eu.prismacloud;

import eu.prismacloud.worker.Replica;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import eu.prismacloud.message.replica_state.Configure;
import eu.prismacloud.message.CommonMessageBuilder;
import eu.prismacloud.message.replica_state.ReplicaConfigured;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static org.fest.assertions.api.Assertions.assertThat;
import scala.concurrent.ExecutionContextExecutor;

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

    private void configureReplicas(Set<ActorRef> replicas, ExecutionContextExecutor dispatcher) throws Exception {
        final ArrayList<Future<Object>> promisesConfigure = new ArrayList<>();

        replicas.parallelStream()
                .forEach(f -> promisesConfigure.add(Patterns.ask(f, Configure.fromReplicas(replicas), timeout)));
        final Future<Iterable<Object>> aggregate = Futures.sequence(promisesConfigure, dispatcher);
        Iterable<Object> result = Await.result(aggregate, timeout.duration());

        int counter = 0;
        for (Object o : result) {
            assert (o instanceof ReplicaConfigured);
            counter++;
        }
        assert (counter == 4);
    }

    @Test
    public void testSetup() throws Exception {

        ActorSystem system = ActorSystem.create("System_1");
        Set<ActorRef> replicas = createReplicas(system, replicaCount);

        new JavaTestKit(system) {
            {
                configureReplicas(replicas, system.dispatcher());
                System.err.println("setup completed");
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
                configureReplicas(replicas, system.dispatcher());
                System.err.println("setup completed");

                final ArrayList<Future<Object>> promises = new ArrayList<>();

                replicas.parallelStream().forEach(ref -> promises.add(Patterns.ask(ref, CommonMessageBuilder.createRequest(ref.path().name(), 1, "fubar"), timeout)));

                final Future<Iterable<Object>> aggregate = Futures.sequence(promises, system.dispatcher());

                Iterable<Object> result = Await.result(aggregate, timeout.duration());
                
                int sum = 0;
                for (Object o : result) {
                    
                    System.err.println("result: " + o);
                    sum++;
                }
                assertThat(sum).isEqualTo(4);
                System.err.println("got " + sum + " replies!");

            }
        };
        JavaTestKit.shutdownActorSystem(system);
    }
}
