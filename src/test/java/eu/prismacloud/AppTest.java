package eu.prismacloud;

import eu.prismacloud.worker.Replica;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Dispatcher;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import eu.prismacloud.message.Configure;
import eu.prismacloud.message.CommonMessageBuilder;
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
import org.junit.After;
import org.junit.Before;

public class AppTest {
    
    ActorSystem system;
    
    Set<ActorRef> replicas;
    
    @Before
    public void setup() {
        system = ActorSystem.create("System_1");
        
        replicas = new HashSet<>();
        replicas.add(system.actorOf(Replica.props(1, true), "main-actor-1"));
        replicas.add(system.actorOf(Replica.props(2, false), "main-actor-2"));
        replicas.add(system.actorOf(Replica.props(3, false), "main-actor-3"));
        replicas.add(system.actorOf(Replica.props(4, false), "main-actor-4"));
    }
    
    @After
    public void teardown() {
        JavaTestKit.shutdownActorSystem(system);
    }
    
    final Timeout timeout = new Timeout(Duration.create(3, "seconds"));
    
    private void setupSystem(Dispatcher dispatcher) throws Exception {
        
        final ArrayList<Future<Object>> promisesConfigure = new ArrayList<>();
            
        replicas.parallelStream()
                .forEach(f -> promisesConfigure.add(Patterns.ask(f, Configure.fromReplicas(replicas), timeout)));
        final Future<Iterable<Object>> aggregate = Futures.sequence(promisesConfigure, dispatcher);
        Iterable<Object> result = Await.result(aggregate, timeout.duration());

        int sum = 0;
        for(Object o : result) {
            sum++;
        }
        assertThat(sum).isEqualTo(4);
    }
    
    @Test
    public void testSetup() throws Exception {
        new JavaTestKit(system) {{
            
            
            final ArrayList<Future<Object>> promisesConfigure = new ArrayList<>();
            final Timeout timeout = new Timeout(Duration.create(3, "seconds"));

            replicas.parallelStream()
                    .forEach(f -> promisesConfigure.add(Patterns.ask(f, Configure.fromReplicas(replicas), timeout)));
            final Future<Iterable<Object>> aggregate = Futures.sequence(promisesConfigure, system.dispatcher());
            Iterable<Object> result = Await.result(aggregate, timeout.duration());

            int sum = 0;
            for(Object o : result) {
                System.err.println("configure-result: " + o);
                sum++;
            }
            assertThat(sum).isEqualTo(4);
        }};
    }

    @Test
    public void testClientRequest() throws Exception {

        new JavaTestKit(system) {{
            /* send Configure message to all replicas and wait for the result */
            final ArrayList<Future<Object>> promisesConfigure = new ArrayList<>();
            final Timeout timeout = new Timeout(Duration.create(3, "seconds"));
            
            replicas.parallelStream()
                    .forEach(f -> promisesConfigure.add(Patterns.ask(f, Configure.fromReplicas(replicas), timeout)));
            final Future<Iterable<Object>> aggregate = Futures.sequence(promisesConfigure, system.dispatcher());
            Iterable<Object> result = Await.result(aggregate, timeout.duration());
                        int sum = 0;
                        for(Object o : result) {
                            System.err.println("configure-result: " + o);
                            sum++;
                        }
                        assertThat(sum).isEqualTo(4);

            
            /* TODO: this should be racy, we need to wait until the replica has finished configuration! */
            
            new Within(Duration.create(5, "second")) {
            
                public void run() {
                
                    
                    final ArrayList<Future<Object>> promises = new ArrayList<>();
   
                    replicas.parallelStream().forEach(ref -> promises.add(Patterns.ask(ref, CommonMessageBuilder.createRequest(ref.path().name(), 1, "fubar"), timeout)));
                    
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
