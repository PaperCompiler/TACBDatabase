package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.entity.Economy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for {@link Economy} entities.
 */
public interface EconomyRepository extends Repository<Economy, Long> {

    /**
     * Finds an economy account by player UUID.
     *
     * @param uuid the player UUID
     * @return the economy account, or empty if not found
     */
    CompletableFuture<Optional<Economy>> findByUuid(UUID uuid);

    /**
     * Adjusts a player's balance by the given amount.
     *
     * @param uuid   the player UUID
     * @param amount the amount to add (can be negative)
     * @return the updated economy account
     */
    CompletableFuture<Economy> adjustBalance(UUID uuid, BigDecimal amount);
}
