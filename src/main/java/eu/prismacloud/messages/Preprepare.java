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
public class Preprepare extends ReplicaCommand {
    
    private final int sequenceNr;
    
    private final int clientSequence;
    
    public Preprepare(int sequenceNr, int clientSequence) {
        this.sequenceNr = sequenceNr;
        this.clientSequence = clientSequence;
    }
    
    public int getSequenceNr() {
        return this.sequenceNr;
    }
    
    public int getClientSequence() {
        return this.clientSequence;
    }
}
