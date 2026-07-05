package de.papercompiler.tacbdatabase.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.Instant;
import java.util.UUID;

/**
 * Ban entity.
 */
@DatabaseTable(tableName = "bans")
public class Ban implements Entity {

    @DatabaseField(id = true, generatedId = true)
    private Long id;

    @DatabaseField(uniqueCombo = true, canBeNull = false, index = true)
    private UUID targetUuid;

    @DatabaseField(uniqueCombo = true)
    private String targetIp;

    @DatabaseField(canBeNull = false)
    private UUID sourceUuid;

    @DatabaseField(canBeNull = false)
    private String reason;

    @DatabaseField
    private Instant expiresAt;

    @DatabaseField(canBeNull = false)
    private Instant createdAt;

    @DatabaseField
    private Instant updatedAt;

    // Dirty flag for sync (not persisted to DB)
    private transient boolean dirty;

    public Ban() {
        // ORMLite requires a no-arg constructor
    }

    public Ban(UUID targetUuid, UUID sourceUuid, String reason, Instant expiresAt) {
        this.targetUuid = targetUuid;
        this.sourceUuid = sourceUuid;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public String getReason() {
        return reason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
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

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public void setSourceUuid(UUID sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public void setReason(String reason) {
        this.reason = reason;
        markDirty();
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        markDirty();
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void markDirty() {
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof Ban)) return false;
        Ban other = (Ban) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
