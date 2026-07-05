package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.entity.Guild;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for {@link Guild} entities.
 */
public interface GuildRepository extends Repository<Guild, Long> {

    /**
     * Finds a guild by its owner's UUID.
     *
     * @param ownerUuid the owner UUID
     * @return the guild, or empty if not found
     */
    CompletableFuture<Optional<Guild>> findByOwner(UUID ownerUuid);

    /**
     * Finds a guild by its tag.
     *
     * @param tag the guild tag
     * @return the guild, or empty if not found
     */
    CompletableFuture<Optional<Guild>> findByTag(String tag);
}
