package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.entity.Home;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for {@link Home} entities.
 */
public interface HomeRepository extends Repository<Home, Long> {

    /**
     * Finds all homes for a player.
     *
     * @param uuid the player UUID
     * @return a list of homes
     */
    CompletableFuture<List<Home>> findByPlayer(UUID uuid);

    /**
     * Finds a specific home by player UUID and home name.
     *
     * @param uuid the player UUID
     * @param name the home name
     * @return the home, or empty if not found
     */
    CompletableFuture<Optional<Home>> findByName(UUID uuid, String name);
}
