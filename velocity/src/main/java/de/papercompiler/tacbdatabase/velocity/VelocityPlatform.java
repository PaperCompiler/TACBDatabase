package de.papercompiler.tacbdatabase.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.papercompiler.tacbdatabase.platform.Platform;
import de.papercompiler.tacbdatabase.platform.Server;
import de.papercompiler.tacbdatabase.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Velocity platform implementation.
 */
public class VelocityPlatform implements Platform {

    private final ProxyServer proxyServer;
    private final VelocityServer server;
    private final VelocityScheduler scheduler;
    private final Logger logger;

    public VelocityPlatform(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        this.server = new VelocityServer(proxyServer);
        this.scheduler = new VelocityScheduler(proxyServer);
        this.logger = LoggerFactory.getLogger("TACBDatabase-Velocity");
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
     * @return the underlying Velocity proxy server
     */
    public ProxyServer getProxyServer() {
        return proxyServer;
    }
}
