package de.papercompiler.tacbdatabase.pubsub;

/**
 * Handler for pub/sub messages.
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * Called when a message is received.
     *
     * @param channel the channel the message was received on
     * @param message the message content
     */
    void onMessage(String channel, String message);
}
