package de.papercompiler.tacbdatabase.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import de.papercompiler.tacbdatabase.TACBDatabase;
import de.papercompiler.tacbdatabase.config.TACBConfig;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Bootstrap for Velocity proxy.
 *
 * <p>Usage in your plugin:
 * <pre>
 *     &#64;Plugin(id = "myplugin", name = "MyPlugin", version = "1.0")
 *     public class MyPlugin {
 *         private TACBDatabase database;
 *
 *         &#64;Subscribe
 *         public void onProxyInitialization(ProxyInitializeEvent event) {
 *             TACBConfig config = TACBConfig.builder()
 *                 .database(DatabaseConfig.of("localhost", 5432, "tacb", "user", "pass", 10))
 *                 .redis(RedisConfig.of("localhost", 6379))
 *                 .build();
 *             database = TACBDatabase.bootstrap(new VelocityPlatform(getServer()), config);
 *         }
 *
 *         &#64;Subscribe
 *         public void onProxyShutdown(ProxyShutdownEvent event) {
 *             if (database != null) {
 *                 database.shutdown();
 *             }
 *         }
 *     }
 * </pre>
 */
@Plugin(id = "tacbdatabase", name = "TACBDatabase", version = "1.0-SNAPSHOT")
public class VelocityBootstrap {

    private TACBDatabase database;

    /**
     * Called when the proxy is initializing.
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // This is a placeholder. Users should bootstrap TACBDatabase in their own plugin.
        // This class exists as a reference implementation.
    }

    /**
     * Called when the proxy is shutting down.
     */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (database != null) {
            database.shutdown();
        }
    }

    /**
     * Bootstraps TACBDatabase with the given config.
     *
     * @param proxyServer the Velocity proxy server
     * @param config      the configuration
     * @return the TACBDatabase instance
     */
    public TACBDatabase bootstrap(com.velocitypowered.api.proxy.ProxyServer proxyServer, TACBConfig config) {
        VelocityPlatform platform = new VelocityPlatform(proxyServer);
        database = TACBDatabase.bootstrap(platform, config);
        return database;
    }

    /**
     * @return the TACBDatabase instance, or null if not bootstrapped yet
     */
    public TACBDatabase getDatabase() {
        return database;
    }
}
