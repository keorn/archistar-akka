package eu.prismacloud;

import eu.prismacloud.worker.Replica;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.Configure;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import static org.fest.assertions.api.Assertions.assertThat;

public class AppTest {
    
    static ActorSystem system;
    
    static Set<ActorRef> replicas;
    
    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("System_1");
        
        replicas = new HashSet<>();
        replicas.add(system.actorOf(Replica.props(1, true), "main-actor-1"));
        replicas.add(system.actorOf(Replica.props(2, false), "main-actor-2"));
        replicas.add(system.actorOf(Replica.props(3, false), "main-actor-3"));
        replicas.add(system.actorOf(Replica.props(4, false), "main-actor-4"));
    }
    
    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void simpleTwoWay() throws Exception {

        new JavaTestKit(system) {{
            /* send Configure message to all replicas */
            replicas.parallelStream()
                    .forEach(f -> f.tell(Configure.fromReplicas(replicas), ActorRef.noSender()));
            
            new Within(Duration.create(5, "second")) {
            
                public void run() {
                
                    final Timeout timeout = new Timeout(Duration.create(3, "seconds"));
                    
                    final ArrayList<Future<Object>> promises = new ArrayList<>();
                    
                    replicas.parallelStream().forEach(ref -> promises.add(Patterns.ask(ref, new ClientCommand(1, "fubar"), timeout)));
                    
                    final Future<Iterable<Object>> aggregate = Futures.sequence(promises, system.dispatcher());
                    
                    try {
                        Iterable<Object> result = Await.result(aggregate, timeout.duration());
                        int sum = 0;
                        for(Object o : result) {
                            System.err.println("result: " + o);
                            sum++;
                        }
                        assertThat(sum).isEqualTo(4);
                        System.err.println("got " + sum + " replies!");
                     
                    } catch (Exception ex) {
                        Logger.getLogger(AppTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    expectNoMsg();
                }
            };
        }};
    }
}
