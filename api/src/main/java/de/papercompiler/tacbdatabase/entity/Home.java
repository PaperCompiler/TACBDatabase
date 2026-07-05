package de.papercompiler.tacbdatabase.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.Instant;
import java.util.UUID;

/**
 * Home entity.
 */
@DatabaseTable(tableName = "homes")
public class Home implements Entity {

    @DatabaseField(id = true, generatedId = true)
    private Long id;

    @DatabaseField(uniqueCombo = true, canBeNull = false, index = true)
    private UUID uuid;

    @DatabaseField(uniqueCombo = true, canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private String world;

    @DatabaseField(canBeNull = false)
    private double x;

    @DatabaseField(canBeNull = false)
    private double y;

    @DatabaseField(canBeNull = false)
    private double z;

    @DatabaseField
    private float yaw;

    @DatabaseField
    private float pitch;

    @DatabaseField
    private Instant createdAt;

    @DatabaseField
    private Instant updatedAt;

    // Dirty flag for sync (not persisted to DB)
    private transient boolean dirty;

    public Home() {
        // ORMLite requires a no-arg constructor
    }

    public Home(UUID uuid, String name, String world, double x, double y, double z, float yaw, float pitch) {
        this.uuid = uuid;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
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

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
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

    public void setWorld(String world) {
        this.world = world;
        markDirty();
    }

    public void setX(double x) {
        this.x = x;
        markDirty();
    }

    public void setY(double y) {
        this.y = y;
        markDirty();
    }

    public void setZ(double z) {
        this.z = z;
        markDirty();
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        markDirty();
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
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
        if (obj == null || !(obj instanceof Home)) return false;
        Home other = (Home) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
