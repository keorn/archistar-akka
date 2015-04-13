package eu.prismacloud.message;

import akka.actor.ActorRef;

/**
 * NOTE: in traditional PBFT the client is also passed within the ClientCommand.
 *       In our prototype this is achieved by the implicit sender reference.
 * 
 * @author andy
 */
public class ClientCommand {
    
    final public int sequenceId;
    
    final public String operation;
    
    final public long timestamp;
    
    final public static byte[] command = "REQUEST".getBytes();
    
    /* if we would have quantum links we wouldn't have to create MACs */
    final public byte[] mac;
    
    private ActorRef sender;
    
    ClientCommand(int sequenceId, String command, long timestamp, byte[] mac) {
        this.sequenceId = sequenceId;
        this.operation = command;
        this.timestamp = timestamp;
        this.mac = mac;
    }
    
    public ActorRef getSender() {
        return this.sender;
    }
    
    public ActorRef setSender(ActorRef sender) {
        return this.sender = sender;
    }
}
