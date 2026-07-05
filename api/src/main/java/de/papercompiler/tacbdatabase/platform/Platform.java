package de.papercompiler.tacbdatabase.platform;

import de.papercompiler.tacbdatabase.util.Scheduler;

/**
 * Abstraction over the Minecraft platform (Paper, Velocity, Minestom).
 *
 * <p>Each platform module provides an implementation of this interface.
 */
public interface Platform {

    /**
     * @return the server instance
     */
    Server getServer();

    /**
     * @return a scheduler for async/repeating tasks
     */
    Scheduler getScheduler();

    /**
     * @return the platform's logger
     */
    org.slf4j.Logger getLogger();
}
