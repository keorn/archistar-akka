package eu.prismacloud.message;

/**
 *
 * @author andy
 */
public interface MessageBuilder {
    public Object buildFor(String rcpt);
}
