package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generic repository interface for database operations.
 *
 * @param <T>  the entity type
 * @param <ID> the entity ID type
 */
public interface Repository<T extends Entity, ID> {

    /**
     * Finds an entity by its ID.
     *
     * @param id the entity ID
     * @return the entity, or empty if not found
     */
    CompletableFuture<Optional<T>> findById(ID id);

    /**
     * Finds all entities.
     *
     * @return a list of all entities
     */
    CompletableFuture<List<T>> findAll();

    /**
     * Saves a new entity.
     *
     * @param entity the entity to save
     * @return the saved entity (with ID populated)
     */
    CompletableFuture<T> save(T entity);

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update
     * @return the updated entity
     */
    CompletableFuture<T> update(T entity);

    /**
     * Deletes an entity.
     *
     * @param entity the entity to delete
     * @return a future that completes when done
     */
    CompletableFuture<Void> delete(T entity);

    /**
     * Deletes an entity by its ID.
     *
     * @param id the entity ID
     * @return a future that completes when done
     */
    CompletableFuture<Void> deleteById(ID id);

    /**
     * Checks if an entity exists.
     *
     * @param id the entity ID
     * @return true if the entity exists
     */
    CompletableFuture<Boolean> exists(ID id);

    /**
     * Counts all entities.
     *
     * @return the total count
     */
    CompletableFuture<Long> count();
}
