package de.papercompiler.tacbdatabase.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all database entities.
 */
public interface Entity extends Serializable {

    /**
     * @return the unique database ID (null if not persisted yet)
     */
    Long getId();

    /**
     * @return when the entity was first created
     */
    Instant getCreatedAt();

    /**
     * @return when the entity was last updated
     */
    Instant getUpdatedAt();

    /**
     * Checks if this entity is equal to another based on ID.
     *
     * @param obj the object to compare
     * @return true if the entities have the same non-null ID
     */
    @Override
    boolean equals(Object obj);

    /**
     * @return the hash code based on the entity ID
     */
    @Override
    int hashCode();
}
