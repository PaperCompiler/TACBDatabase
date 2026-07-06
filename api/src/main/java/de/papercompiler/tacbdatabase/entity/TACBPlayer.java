package de.papercompiler.tacbdatabase.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.Instant;
import java.util.UUID;

/**
 * Player entity.
 */
@DatabaseTable(tableName = "players")
public class TACBPlayer implements Entity {

    @DatabaseField(id = true, generatedId = true)
    private Long id;

    @DatabaseField(unique = true, canBeNull = false, index = true)
    private UUID uuid;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField
    private String lastServer;

    @DatabaseField
    private Instant firstJoin;

    @DatabaseField
    private Instant lastJoin;

    @DatabaseField
    private long playtimeTicks;

    @DatabaseField
    private Instant createdAt;

    @DatabaseField
    private Instant updatedAt;

    // Dirty flag for sync (not persisted to DB)
    private transient boolean dirty;

    public TACBPlayer() {
        // ORMLite requires a no-arg constructor
    }

    public TACBPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = Instant.now();
        this.lastJoin = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getLastServer() {
        return lastServer;
    }

    public Instant getFirstJoin() {
        return firstJoin;
    }

    public Instant getLastJoin() {
        return lastJoin;
    }

    public long getPlaytimeTicks() {
        return playtimeTicks;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
        markDirty();
    }

    public void setLastServer(String lastServer) {
        this.lastServer = lastServer;
        markDirty();
    }

    public void setFirstJoin(Instant firstJoin) {
        this.firstJoin = firstJoin;
        markDirty();
    }

    public void setLastJoin(Instant lastJoin) {
        this.lastJoin = lastJoin;
        markDirty();
    }

    public void setPlaytimeTicks(long playtimeTicks) {
        this.playtimeTicks = playtimeTicks;
        markDirty();
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        markDirty();
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        markDirty();
    }

    private void markDirty() {
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof TACBPlayer)) return false;
        TACBPlayer other = (TACBPlayer) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
