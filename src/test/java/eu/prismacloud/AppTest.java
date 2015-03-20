package eu.prismacloud;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import eu.prismacloud.messages.ClientCommand;
import eu.prismacloud.messages.Configure;
import java.util.HashSet;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;

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
    public void simpleOneWay() throws Exception {

        new JavaTestKit(system) {{
            /* send Configure message to all replicas */
            replicas.parallelStream()
                    .forEach(f -> f.tell(Configure.fromReplicas(replicas), ActorRef.noSender()));
    
            /* send a request and wait for an answer; TODO: can we do this with parallelStream and Exception handling? */
            
            new Within(Duration.create(10, "second")) {
            
                public void run() {
                    for(ActorRef ref : replicas) {
                        ref.tell(new ClientCommand(1, "fubar"), ActorRef.noSender());
                    }
                /*
                Timeout timeout = new Timeout(Duration.create(5, "seconds"));
                Future<Object> result = Patterns.ask(ref, new ClientCommand(1, "fubar"), timeout);
                Await.result(result, timeout.duration());
                    }*/
                
                    expectNoMsg();
                }
            };
        }};
    }
}
