package eu.prismacloud.message.execution;

import akka.actor.ActorRef;
import eu.prismacloud.message.MessageBuilder;

/**
 *
 * @author andy
 */
public class ExecuteBuilder implements MessageBuilder {
        
    private final int sequenceNr;
    
    private final String command;
    
    private final ActorRef client;
    
    public ExecuteBuilder(int sequenceNr, String command, ActorRef client) {
        this.command = command;
        this.sequenceNr = sequenceNr;
        this.client = client;
    }
    
    @Override
    public Execute buildFor(String rcpt) {
        return new Execute(sequenceNr, command);
    }
    
    public ActorRef getClient() {
        return client;
    }
}
