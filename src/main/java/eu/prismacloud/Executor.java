/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.prismacloud;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.messages.Execute;

/**
 *
 * @author andy
 */
public class Executor extends UntypedActor {

    private final int replicaId;
    
    private int lastExecuted = -1;
    
    static Props props(int replicaId) {
        return Props.create(new Creator<Executor>() {
           @Override
           public Executor create() throws Exception {
               return new Executor(replicaId);
           }
        });
    }
    
    Executor(int replicaId) {
        this.replicaId = replicaId;
    }
    
    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof Execute) {
            Execute cmd = (Execute)o;
            
            if (cmd.getSequenceNr() > lastExecuted) {
                System.err.println("\nreplica[" + replicaId + "|" + cmd.getSequenceNr() + " EXECUTE " + cmd.getCommand());
                lastExecuted = cmd.getSequenceNr();
            }
            /* TODO: send result back to client */
        } else {
            unhandled(o);
        }
    }
}
