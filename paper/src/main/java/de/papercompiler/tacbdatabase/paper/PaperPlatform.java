package de.papercompiler.tacbdatabase.paper;

import de.papercompiler.tacbdatabase.platform.Platform;
import de.papercompiler.tacbdatabase.platform.Server;
import de.papercompiler.tacbdatabase.util.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Paper platform implementation.
 */
public class PaperPlatform implements Platform {

    private final JavaPlugin plugin;
    private final PaperServer server;
    private final PaperScheduler scheduler;
    private final Logger logger;

    public PaperPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.server = new PaperServer(plugin.getServer());
        this.scheduler = new PaperScheduler(plugin);
        this.logger = LoggerFactory.getLogger("TACBDatabase-" + plugin.getName());
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * @return the underlying Bukkit plugin
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
}
