package de.papercompiler.tacbdatabase.packet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.Map;

/**
 * Generic custom event packet.
 * <p>
 * Channel is configurable per event type, e.g.:
 * {@code tacb:event:player:join}, {@code tacb:event:guild:create}
 */
public class CustomEventPacket implements Packet {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @JsonProperty("event")
    private String event;

    @JsonProperty("data")
    private Map<String, Object> data;

    public CustomEventPacket() {
        // Jackson requires a no-arg constructor
    }

    public CustomEventPacket(String event, Map<String, Object> data) {
        this.event = event;
        this.data = data;
    }

    @Override
    public String getChannel() {
        return "tacb:event:" + event;
    }

    @Override
    public byte[] serialize() throws IOException {
        return MAPPER.writeValueAsBytes(this);
    }

    public String getEvent() {
        return event;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
