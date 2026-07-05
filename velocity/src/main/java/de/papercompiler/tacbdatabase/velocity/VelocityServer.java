package de.papercompiler.tacbdatabase.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import de.papercompiler.tacbdatabase.platform.Server;

/**
 * Velocity server wrapper.
 */
public class VelocityServer implements Server {

    private final ProxyServer proxyServer;

    public VelocityServer(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return proxyServer.getVersion().getName();
    }

    @Override
    public boolean isProxy() {
        return true;
    }
}
