package de.papercompiler.tacbdatabase.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Economy account entity.
 */
@DatabaseTable(tableName = "economy")
public class Economy implements Entity {

    @DatabaseField(id = true, generatedId = true)
    private Long id;

    @DatabaseField(unique = true, canBeNull = false, index = true)
    private UUID uuid;

    @DatabaseField(canBeNull = false)
    private BigDecimal balance;

    @DatabaseField
    private String currency;

    @DatabaseField
    private Instant createdAt;

    @DatabaseField
    private Instant updatedAt;

    // Dirty flag for sync (not persisted to DB)
    private transient boolean dirty;

    public Economy() {
        // ORMLite requires a no-arg constructor
    }

    public Economy(UUID uuid, BigDecimal balance, String currency) {
        this.uuid = uuid;
        this.balance = balance;
        this.currency = currency;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
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

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        markDirty();
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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
        if (obj == null || !(obj instanceof Economy)) return false;
        Economy other = (Economy) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
