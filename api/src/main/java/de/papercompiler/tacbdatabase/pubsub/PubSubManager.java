package de.papercompiler.tacbdatabase.pubsub;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction over Redis pub/sub.
 */
public interface PubSubManager {

    /**
     * Subscribes to a channel.
     *
     * @param channel the channel name
     * @param handler the message handler
     * @return a future that completes when subscribed
     */
    CompletableFuture<Void> subscribe(String channel, MessageHandler handler);

    /**
     * Unsubscribes from a channel.
     *
     * @param channel the channel name
     * @return a future that completes when unsubscribed
     */
    CompletableFuture<Void> unsubscribe(String channel);

    /**
     * Publishes a message to a channel.
     *
     * @param channel the channel name
     * @param message the message to publish
     * @return a future that completes when published
     */
    CompletableFuture<Void> publish(String channel, Object message);

    /**
     * Publishes raw bytes to a channel.
     *
     * @param channel the channel name
     * @param data    the raw data
     * @return a future that completes when published
     */
    CompletableFuture<Void> publish(String channel, byte[] data);

    /**
     * Closes the pub/sub manager and releases resources.
     */
    void close();
}
