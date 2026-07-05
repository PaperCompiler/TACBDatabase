package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.entity.Ban;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for {@link Ban} entities.
 */
public interface BanRepository extends Repository<Ban, Long> {

    /**
     * Finds an active ban for a player UUID.
     *
     * @param uuid the player UUID
     * @return the active ban, or empty if not found
     */
    CompletableFuture<Optional<Ban>> findActiveByUuid(UUID uuid);

    /**
     * Finds an active ban for an IP address.
     *
     * @param ip the IP address
     * @return the active ban, or empty if not found
     */
    CompletableFuture<Optional<Ban>> findActiveByIp(String ip);
}
