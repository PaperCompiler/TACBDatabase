package de.papercompiler.tacbdatabase.packet;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;

/**
 * Base interface for all packets.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlayerSyncPacket.class, name = "player_sync"),
        @JsonSubTypes.Type(value = ServerStatusPacket.class, name = "server_status"),
        @JsonSubTypes.Type(value = CustomEventPacket.class, name = "custom_event")
})
public interface Packet {

    /**
     * @return the Redis channel this packet should be sent on
     */
    String getChannel();

    /**
     * Serializes the packet to bytes.
     *
     * @return the serialized bytes
     * @throws IOException if serialization fails
     */
    byte[] serialize() throws IOException;

    /**
     * Deserializes bytes into a packet of the given type.
     *
     * @param data  the serialized bytes
     * @param clazz the target class
     * @param <T>   the packet type
     * @return the deserialized packet
     * @throws IOException if deserialization fails
     */
    static <T extends Packet> T deserialize(byte[] data, Class<T> clazz) throws IOException {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.readValue(data, clazz);
    }
}
