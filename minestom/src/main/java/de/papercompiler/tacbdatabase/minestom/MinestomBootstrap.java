package de.papercompiler.tacbdatabase.minestom;

import de.papercompiler.tacbdatabase.TACBDatabase;
import de.papercompiler.tacbdatabase.config.TACBConfig;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;

/**
 * Bootstrap for Minestom servers.
 *
 * <p>Usage in your Minestom application:
 * <pre>
 *     public class MyApplication {
 *         private TACBDatabase database;
 *
 *         public void init() {
 *             MinecraftServer server = MinecraftServer.init(...);
 *             
 *             TACBConfig config = TACBConfig.builder()
 *                 .redis(RedisConfig.of("localhost", 6379))
 *                 .build();
 *             database = TACBDatabase.bootstrap(new MinestomPlatform(server), config);
 *             
 *             server.start();
 *         }
 *
 *         public void stop() {
 *             if (database != null) {
 *                 database.shutdown();
 *             }
 *         }
 *     }
 * </pre>
 */
public class MinestomBootstrap {

    private TACBDatabase database;

    /**
     * Bootstraps TACBDatabase with the given config.
     *
     * @param minecraftServer the Minestom MinecraftServer
     * @param config          the configuration
     * @return the TACBDatabase instance
     */
    public TACBDatabase bootstrap(MinecraftServer minecraftServer, TACBConfig config) {
        MinestomPlatform platform = new MinestomPlatform(minecraftServer);
        database = TACBDatabase.bootstrap(platform, config);
        return database;
    }

    /**
     * @return the TACBDatabase instance, or null if not bootstrapped yet
     */
    public TACBDatabase getDatabase() {
        return database;
    }

    /**
     * Shuts down TACBDatabase.
     */
    public void shutdown() {
        if (database != null) {
            database.shutdown();
            database = null;
        }
    }
}
