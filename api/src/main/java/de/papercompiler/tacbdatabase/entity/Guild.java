package de.papercompiler.tacbdatabase.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.Instant;
import java.util.UUID;

/**
 * Guild entity.
 */
@DatabaseTable(tableName = "guilds")
public class Guild implements Entity {

    @DatabaseField(id = true, generatedId = true)
    private Long id;

    @DatabaseField(unique = true, canBeNull = false, index = true)
    private String name;

    @DatabaseField(unique = true)
    private String tag;

    @DatabaseField(canBeNull = false)
    private UUID ownerUuid;

    @DatabaseField
    private String description;

    @DatabaseField
    private Instant createdAt;

    @DatabaseField
    private Instant updatedAt;

    // Dirty flag for sync (not persisted to DB)
    private transient boolean dirty;

    public Guild() {
        // ORMLite requires a no-arg constructor
    }

    public Guild(String name, String tag, UUID ownerUuid) {
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getDescription() {
        return description;
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

    public void setName(String name) {
        this.name = name;
        markDirty();
    }

    public void setTag(String tag) {
        this.tag = tag;
        markDirty();
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        markDirty();
    }

    public void setDescription(String description) {
        this.description = description;
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
        if (obj == null || !(obj instanceof Guild)) return false;
        Guild other = (Guild) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
