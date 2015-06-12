package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.replica_state.RemoteReplicaReady;
import eu.prismacloud.message.replica_state.RemoteReplicasReady;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * Currently this class should know all remote replicas.. long-time solution
 * would be, that it spawns a sub-actor per replica
 * 
 * @author andy
 */
public class RemoteReplicas extends UntypedActor {

    private final Set<ActorSelection> inactivePeers;
    
    private final Set<ActorRef> activePeers;
    
    private final ActorRef parent;
    
    public static Props props(Set<ActorSelection> peers, ActorRef parent) {
        return Props.create(new Creator<RemoteReplicas>() {
           @Override
           public RemoteReplicas create() throws Exception {
               return new RemoteReplicas(peers, parent);
           }
        });
    }
    
    RemoteReplicas(Set<ActorSelection> stringPeers, ActorRef parent) {
        
        this.parent = parent;
        this.activePeers = new HashSet<>();
        
        System.err.println("replica count: " + stringPeers.size());
        
        stringPeers.forEach(x -> getContext().actorOf(RemoteReplica.props(x, getSelf())));
        this.inactivePeers = stringPeers;
        
        if (inactivePeers.isEmpty()) {
            parent.tell(new RemoteReplicasReady(), ActorRef.noSender());
        }
    }
    
    @Override
    public void onReceive(Object o) {
        
        if (o instanceof RemoteReplicaReady) {
            activePeers.add(getSender());
            inactivePeers.remove(((RemoteReplicaReady)o).forReplica);
            
            if (inactivePeers.isEmpty()) {
                parent.tell(new RemoteReplicasReady(), ActorRef.noSender());
            }
        } else if (o instanceof MessageBuilder) {
            MessageBuilder b = (MessageBuilder)o;
            
            activePeers.parallelStream()
                 .forEach(x -> x.tell(b, getSender()));
        }
        
    }
}
