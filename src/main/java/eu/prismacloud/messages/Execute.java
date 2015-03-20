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
public class Execute {
    private final int sequenceNr;
    
    private final String command;
    
    public Execute(int sequenceNr, String command) {
        this.command = command;
        this.sequenceNr = sequenceNr;
    }
    
    public int getSequenceNr() {
        return this.sequenceNr;
    }
    
    public String getCommand() {
        return this.command;
    }
}
