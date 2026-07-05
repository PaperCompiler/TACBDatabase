package de.papercompiler.tacbdatabase.minestom;

import de.papercompiler.tacbdatabase.platform.Platform;
import de.papercompiler.tacbdatabase.platform.Server;
import de.papercompiler.tacbdatabase.util.Scheduler;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minestom platform implementation.
 */
public class MinestomPlatform implements Platform {

    private final MinecraftServer minecraftServer;
    private final MinestomServer server;
    private final MinestomScheduler scheduler;
    private final Logger logger;

    public MinestomPlatform(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
        this.server = new MinestomServer(minecraftServer);
        this.scheduler = new MinestomScheduler(minecraftServer);
        this.logger = LoggerFactory.getLogger("TACBDatabase-Minestom");
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
     * @return the underlying Minestom MinecraftServer
     */
    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
