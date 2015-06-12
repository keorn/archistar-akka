package at.archistar.bft.message;

/**
 *
 * @author andy
 */
public interface MessageBuilder {
    public Object buildFor(String rcpt);
}
