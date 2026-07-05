 package de.papercompiler.tacbdatabase.packet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.UUID;

/**
 * Packet for syncing player data across servers.
 */
public class PlayerSyncPacket implements Packet {

    public static final String CHANNEL = "tacb:player:sync";
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @JsonProperty("uuid")
    private UUID uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("server")
    private String server;

    @JsonProperty("playtime_ticks")
    private long playtimeTicks;

    public PlayerSyncPacket() {
        // Jackson requires a no-arg constructor
    }

    public PlayerSyncPacket(UUID uuid, String name, String server) {
        this.uuid = uuid;
        this.name = name;
        this.server = server;
        this.playtimeTicks = 0;
    }

    public PlayerSyncPacket(UUID uuid, String name, String server, long playtimeTicks) {
        this.uuid = uuid;
        this.name = name;
        this.server = server;
        this.playtimeTicks = playtimeTicks;
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    @Override
    public byte[] serialize() throws IOException {
        return MAPPER.writeValueAsBytes(this);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public long getPlaytimeTicks() {
        return playtimeTicks;
    }
}
