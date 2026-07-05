package de.papercompiler.tacbdatabase.packet;

import java.util.concurrent.CompletableFuture;

/**
 * Manager for sending and receiving packets via Redis pub/sub.
 */
public interface PacketManager {

    /**
     * Sends a packet.
     *
     * @param packet the packet to send
     * @param <T>    the packet type
     * @return a future that completes when sent
     */
    <T extends Packet> CompletableFuture<Void> send(T packet);

    /**
     * Registers a handler for a specific packet type.
     *
     * @param type    the packet class
     * @param handler the handler
     * @param <T>     the packet type
     */
    <T extends Packet> void registerHandler(Class<T> type, PacketHandler<T> handler);

    /**
     * Unregisters a handler for a specific packet type.
     *
     * @param type the packet class
     * @param <T>  the packet type
     */
    <T extends Packet> void unregisterHandler(Class<T> type);

    /**
     * Closes the packet manager and releases resources.
     */
    void close();
}
