package eu.prismacloud.message;

import akka.actor.ActorRef;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author andy
 */
public class Configure {
    private final Set<String> peers;
    
    public Configure(Set<String> peers) {
        this.peers = peers;
    }
    
    public Set<String> getPeers() {
        return this.peers;
    }
    
    public static Configure fromReplicas(Set<ActorRef> replicas) {
        Set<String> replicasString = replicas.stream()
                                             .map(f -> f.path().toStringWithoutAddress())
                                             .collect(Collectors.toSet());
        return new Configure(replicasString);
    }
}
