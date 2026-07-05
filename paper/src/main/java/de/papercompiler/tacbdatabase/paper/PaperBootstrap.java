package de.papercompiler.tacbdatabase.paper;

import de.papercompiler.tacbdatabase.TACBDatabase;
import de.papercompiler.tacbdatabase.config.TACBConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * Bootstrap for Paper servers.
 *
 * <p>Usage in your plugin:
 * <pre>
 *     public class MyPlugin extends JavaPlugin {
 *         private TACBDatabase database;
 *
 *         @Override
 *         public void onEnable() {
 *             TACBConfig config = TACBConfig.builder()
 *                 .redis(RedisConfig.of("localhost", 6379))
 *                 .build();
 *             database = TACBDatabase.bootstrap(new PaperPlatform(this), config);
 *         }
 *
 *         @Override
 *         public void onDisable() {
 *             if (database != null) {
 *                 database.shutdown();
 *             }
 *         }
 *     }
 * </pre>
 */
public class PaperBootstrap {

    private final JavaPlugin plugin;
    private TACBDatabase database;

    public PaperBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads config from the plugin's data folder and bootstraps TACBDatabase.
     *
     * @param configPath the path to the config file (relative to data folder)
     * @return the TACBDatabase instance
     */
    public TACBDatabase bootstrap(String configPath) {
        Path configFile = plugin.getDataFolder().toPath().resolve(configPath);
        TACBConfig config = TACBConfig.load(configFile);
        return bootstrap(config);
    }

    /**
     * Bootstraps TACBDatabase with the given config.
     *
     * @param config the configuration
     * @return the TACBDatabase instance
     */
    public TACBDatabase bootstrap(TACBConfig config) {
        PaperPlatform platform = new PaperPlatform(plugin);
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
