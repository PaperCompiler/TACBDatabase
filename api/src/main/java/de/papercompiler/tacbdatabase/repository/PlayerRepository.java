package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.entity.TACBPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for {@link TACBPlayer} entities.
 */
public interface PlayerRepository extends Repository<TACBPlayer, Long> {

    /**
     * Finds a player by their UUID.
     *
     * @param uuid the player UUID
     * @return the player, or empty if not found
     */
    CompletableFuture<Optional<TACBPlayer>> findByUuid(UUID uuid);

    /**
     * Finds a player by their name.
     *
     * @param name the player name
     * @return the player, or empty if not found
     */
    CompletableFuture<Optional<TACBPlayer>> findByName(String name);

    /**
     * Finds all online players (players with a lastServer set).
     *
     * @return a list of online players
     */
    CompletableFuture<List<TACBPlayer>> findOnline();
}
