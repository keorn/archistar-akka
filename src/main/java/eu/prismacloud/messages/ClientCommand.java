/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.prismacloud.messages;

/**
 *
 * @author andy
 */
public class ClientCommand {
    final private int sequenceId;
    
    final private String command;
    
    public ClientCommand(int sequenceId, String command) {
        this.sequenceId = sequenceId;
        this.command = command;
    }
    
    public int getSequenceId() {
        return this.sequenceId;
    }
    
    public String getCommand() {
        return this.command;
    }
}
