package eu.prismacloud.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import eu.prismacloud.message.ClientCommand;
import eu.prismacloud.message.Commit;
import eu.prismacloud.message.CreateTransaction;
import eu.prismacloud.message.MessageBuilder;
import eu.prismacloud.message.Prepare;
import eu.prismacloud.message.Preprepare;
import java.util.Set;
import scala.Option;

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
    
    private final Set<ActorSelection> peers;
    
    static Props props(int viewNr, boolean primary, int replicaId, ActorRef cmdExecutor, Set<ActorSelection> peers) {
        return Props.create(new Creator<View>() {
           @Override
           public View create() throws Exception {
               return new View(viewNr, primary, replicaId, cmdExecutor, peers);
           }
        });
    }
    
    View(int viewNr, boolean primary, int replicaId, ActorRef cmdExecutor, Set<ActorSelection> peers) {
        this.viewNr = viewNr;
        this.primary = primary;
        this.replicaId = replicaId;
        this.cmdExecutor = cmdExecutor;
        this.peers = peers;
    }
    
    private ActorRef createTransaction(Preprepare preprepare, ClientCommand cmd, int fCount, ActorRef sender) {
        
        final Option<ActorRef> child = getContext().child("transaction-" + preprepare.sequenceNr);
        
        ActorRef t = null;
        if (child.isDefined()) {
            t = child.get();
        } else {
            t = getContext().actorOf(Transaction.props(primary, replicaId,  cmdExecutor, peers, fCount, preprepare.sequenceNr),
                                        "transaction-" + preprepare.sequenceNr);
        }
        
        t.tell(cmd, sender);
        t.tell(preprepare, sender);
        
        return t;
    }
    
    private ActorRef findOrCreateChild(int sequenceNr) {
        /* why is option.getOrElse not working? */
        final Option<ActorRef> child = getContext().child("transaction-" + sequenceNr);
        if (child.isDefined()) {
            return child.get();
        } else {
            return getContext().actorOf(Transaction.props(primary, replicaId, cmdExecutor, peers, viewNr, sequenceNr), "transaction-" + sequenceNr);
        }
    }
    
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof CreateTransaction) {
            CreateTransaction cmd = (CreateTransaction)message;
            createTransaction(cmd.preprepare, cmd.cmd, cmd.fCount, cmd.cmd.getSender());
        } else if (message instanceof Prepare) {
            Prepare cmd = (Prepare)message;
            MessageBuilder.validate(cmd);

            ActorRef t = findOrCreateChild(cmd.sequenceNr);
            t.tell(cmd, ActorRef.noSender());
         } else if (message instanceof Commit) {
            Commit cmd = (Commit)message;
            MessageBuilder.validate(cmd);

            ActorRef t = findOrCreateChild(cmd.sequenceNr);
            t.tell(cmd, ActorRef.noSender());
        } else {
            unhandled(message);
        }
    }
}
