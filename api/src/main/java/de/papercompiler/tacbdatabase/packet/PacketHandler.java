package de.papercompiler.tacbdatabase.packet;

/**
 * Handler for incoming packets.
 *
 * @param <T> the packet type
 */
@FunctionalInterface
public interface PacketHandler<T extends Packet> {

    /**
     * Called when a packet is received.
     *
     * @param packet the received packet
     */
    void handle(T packet);
}
