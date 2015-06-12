package at.archistar.bft.views;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import at.archistar.bft.replica.message.ClientCommand;
import at.archistar.bft.message.CommonMessageBuilder;
import at.archistar.bft.executor.message.ExecutionCompleted;
import at.archistar.bft.views.message.ViewReady;
import java.util.HashMap;
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

    private int seqCounter = 0;

    private final int fCount = 1;

    /* map from sequence id to transaction */
    private final Map<Integer, Transaction> transactions = new HashMap<>();

    /* store unmachted ClientCommands by client-sequence-id */
    private final HashMap<Integer, ClientCommand> unmatchedClientCommands = new HashMap<>();

    /* map from client-sequence-id to sequence-id */
    private final HashMap<Integer, Transaction> prepreparedTransactions = new HashMap<>();

    public static Props props(int viewNr, boolean primary, int replicaId, ActorRef cmdExecutor, ActorRef peers) {
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

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof ClientCommand) {
            ClientCommand cmd = (ClientCommand) message;
            CommonMessageBuilder.validate(cmd);

            if (primary) {
                int newSeq = ++seqCounter;
                Transaction tx = new Transaction(primary, replicaId, fCount, newSeq);

                tx.addClientCommand(cmd,
                        x -> peers.tell(x, ActorRef.noSender()),
                        x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
                tx.addPreprepare(new PreprepareBuilder(newSeq, viewNr, cmd).buildFakeSelfPreprepare(),
                        x -> peers.tell(x, ActorRef.noSender()),
                        x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
                transactions.put(newSeq, tx);
            } else {
                if (prepreparedTransactions.containsKey(cmd.sequenceId)) {
                    Transaction tx = prepreparedTransactions.get(cmd.sequenceId);
                    tx.addClientCommand(cmd,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
                } else {
                    unmatchedClientCommands.put(cmd.sequenceId, cmd);
                }
            }
        } else if (message instanceof ViewMessage) {
            ViewMessage rMsg = (ViewMessage)message;
            
            Transaction tx = transactions.computeIfAbsent(rMsg.sequenceNr, x -> new Transaction(primary, replicaId, fCount, x));
            
            if (message instanceof Preprepare) {
                Preprepare preprepare = (Preprepare)message;
                CommonMessageBuilder.validate(preprepare);
                assert (!primary);
                
                if (unmatchedClientCommands.containsKey(preprepare.clientSequence)) {
                    ClientCommand cc = unmatchedClientCommands.remove(preprepare.clientSequence);
                    tx.addClientCommand(cc,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
                }
                tx.addPreprepare(preprepare,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
            } else if (message instanceof Prepare) {
                Prepare cmd = (Prepare) message;
                CommonMessageBuilder.validate(cmd);

                tx.addPrepare(cmd,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
            } else if (message instanceof Commit) {
                Commit cmd = (Commit) message;
                CommonMessageBuilder.validate(cmd);

                tx.addCommit(cmd,
                            x -> peers.tell(x, ActorRef.noSender()),
                            x -> cmdExecutor.tell(x.buildFor("client"), getSelf()));
            }
        } else if (message instanceof ExecutionCompleted) {
            ExecutionCompleted cmd = (ExecutionCompleted) message;

            Transaction tx = transactions.remove(cmd.sequenceNr);
            tx.getClient().tell(CommonMessageBuilder.createClientCommandResult("rcpt", tx.getClientSeq(), "result!"), cmdExecutor);
        } else {
            unhandled(message);
        }
    }
}
