package at.archistar.bft.remotes.messages;

import akka.actor.ActorSelection;

/**
 *
 * @author andy
 */
public class RemoteReplicaReady {
    public final ActorSelection forReplica;
    
    public RemoteReplicaReady(ActorSelection forReplica) {
        this.forReplica = forReplica;
    }
}
