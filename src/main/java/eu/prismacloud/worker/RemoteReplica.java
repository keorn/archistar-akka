package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.RemoteReplicasReady;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * Currently this class should know all remote replicas.. long-time solution
 * would be, that it spawns a sub-actor per replica
 * 
 * @author andy
 */
public class RemoteReplica extends UntypedActor {

    private final Set<ActorSelection> peers;
    
    public static Props props(Set<String> peers, String me) {
        return Props.create(new Creator<RemoteReplica>() {
           @Override
           public RemoteReplica create() throws Exception {
               return new RemoteReplica(peers, me);
           }
        });
    }
    
    RemoteReplica(Set<String> stringPeers, String me) {
        
        peers = stringPeers.stream().filter(f -> !f.equalsIgnoreCase(me))
                                    .map(f -> context().actorSelection(f))
                                    .collect(Collectors.toSet());
        
        System.err.println("replicas: " + peers.size());
        
        /* TODO: start an actor per peer and wait till it responsive */
        
        /* just pretent that we're instantly ready */
        getContext().parent().tell(new RemoteReplicasReady(), ActorRef.noSender());
    }
    
    @Override
    public void onReceive(Object o) {
        if (o instanceof MessageBuilder) {
            MessageBuilder b = (MessageBuilder)o;
            
            peers.parallelStream()
                 .forEach(x -> x.tell(b.buildFor(x.pathString()), getSelf()));
        }
        
    }
}
