package de.papercompiler.tacbdatabase.packet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.Map;

/**
 * Packet for broadcasting server status (for external services/monitoring).
 * <p>
 * This is NOT meant to be displayed in-game. It's for other services
 * that need to know server health, player counts, etc.
 */
public class ServerStatusPacket implements Packet {

    public static final String CHANNEL = "tacb:server:status";
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @JsonProperty("server_name")
    private String serverName;

    @JsonProperty("online_players")
    private int onlinePlayers;

    @JsonProperty("max_players")
    private int maxPlayers;

    @JsonProperty("tps")
    private double tps;

    @JsonProperty("memory_used_mb")
    private long memoryUsedMb;

    @JsonProperty("memory_max_mb")
    private long memoryMaxMb;

    @JsonProperty("extra")
    private Map<String, Object> extra;

    public ServerStatusPacket() {
        // Jackson requires a no-arg constructor
    }

    public ServerStatusPacket(String serverName, int onlinePlayers, int maxPlayers,
                              double tps, long memoryUsedMb, long memoryMaxMb) {
        this.serverName = serverName;
        this.onlinePlayers = onlinePlayers;
        this.maxPlayers = maxPlayers;
        this.tps = tps;
        this.memoryUsedMb = memoryUsedMb;
        this.memoryMaxMb = memoryMaxMb;
        this.extra = null;
    }

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    @Override
    public byte[] serialize() throws IOException {
        return MAPPER.writeValueAsBytes(this);
    }

    public String getServerName() {
        return serverName;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public double getTps() {
        return tps;
    }

    public long getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public long getMemoryMaxMb() {
        return memoryMaxMb;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }
}
