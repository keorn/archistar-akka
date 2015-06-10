package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.replica.Commit;
import eu.prismacloud.message.CreateTransaction;
import eu.prismacloud.message.CommonMessageBuilder;
import eu.prismacloud.message.execution.ExecutionCompleted;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.ViewReady;
import eu.prismacloud.message.replica.Prepare;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * The view is responsible for managing transactions
 * 
 * @author andy
 */
public class View extends UntypedActor {
    
    public final int viewNr;

    public final boolean primary;
    
    public final int replicaId;
    
    public final ActorRef cmdExecutor;
    
    private final ActorRef peers;
    
    private final Map<Integer, Transaction> transactions = new HashMap<>();
    
    static Props props(int viewNr, boolean primary, int replicaId, ActorRef cmdExecutor, ActorRef peers) {
        return Props.create(new Creator<View>() {
           @Override
           public View create() throws Exception {
               return new View(viewNr, primary, replicaId, cmdExecutor, peers);
           }
        });
    }
    
    View(int viewNr, boolean primary, int replicaId, ActorRef cmdExecutor, ActorRef peers) {
        this.viewNr = viewNr;
        this.primary = primary;
        this.replicaId = replicaId;
        this.cmdExecutor = cmdExecutor;
        this.peers = peers;
        
        getContext().parent().tell(new ViewReady(), ActorRef.noSender());
    }
    
    private Transaction findOrCreateTransaction(int sequenceNr, int fCount) {
        return transactions.computeIfAbsent(sequenceNr, k -> new Transaction(primary, replicaId, fCount, sequenceNr));
    }
    
    @Override
    public void onReceive(Object message) throws Exception {
        
        List<MessageBuilder> results = new LinkedList<>();
        
        if (message instanceof CreateTransaction) {
            CreateTransaction cmd = (CreateTransaction)message;
            
            Transaction tx =  new Transaction(primary, replicaId, cmd.fCount, cmd.preprepare.sequenceNr);
            
            tx.addClientCommand(cmd.cmd,
                                x -> peers.tell(x, ActorRef.noSender()),
                                x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
            tx.addPreprepare(cmd.preprepare,
                             x -> peers.tell(x, ActorRef.noSender()),
                             x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
            transactions.put(cmd.preprepare.sequenceNr, tx);
        } else if (message instanceof Prepare) {
            Prepare cmd = (Prepare)message;
            CommonMessageBuilder.validate(cmd);

            findOrCreateTransaction(cmd.sequenceNr, 1)
                .addPrepare(cmd,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
         } else if (message instanceof Commit) {
            Commit cmd = (Commit)message;
            CommonMessageBuilder.validate(cmd);
            
            findOrCreateTransaction(cmd.sequenceNr, 1)
                 .addCommit(cmd,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
        } else if (message instanceof ExecutionCompleted) {
            ExecutionCompleted cmd = (ExecutionCompleted)message;
            
            Transaction tx = transactions.remove(cmd.sequenceNr);
            tx.getClient().tell(CommonMessageBuilder.createClientCommandResult("rcpt", tx.getClientSeq(), "result!"), cmdExecutor);
        } else {
            unhandled(message);
        }
    }
}
