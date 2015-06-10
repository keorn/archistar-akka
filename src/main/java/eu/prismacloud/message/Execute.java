/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.prismacloud.message;

/**
 *
 * @author andy
 */
public class Execute {
    
    public final int sequenceNr;
    
    public final String command;
    
    public Execute(int sequenceNr, String command) {
        this.command = command;
        this.sequenceNr = sequenceNr;
    }
}
