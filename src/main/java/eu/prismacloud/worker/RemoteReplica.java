package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.replica_state.RemoteReplicaReady;

/**
 *
 * @author andy
 */
public class RemoteReplica extends UntypedActor {
    
    private final ActorSelection peer;
    
    private final ActorRef parent;
    
    public static Props props(ActorSelection peer, ActorRef parent) {
        return Props.create(new Creator<RemoteReplica>() {
           @Override
           public RemoteReplica create() throws Exception {
               return new RemoteReplica(peer, parent);
           }
        });
    }
    
    RemoteReplica(ActorSelection peer, ActorRef parent) {
        this.peer = peer;
        this.parent = parent;
        
        this.parent.tell(new RemoteReplicaReady(peer), getSelf());
    }

    @Override
    public void onReceive(Object o) throws Exception {
        
        if (o instanceof MessageBuilder) {
            MessageBuilder b = (MessageBuilder)o;
            Object msg = b.buildFor("rcpt");
            peer.tell(msg, getSender());
        }
    }   
}
