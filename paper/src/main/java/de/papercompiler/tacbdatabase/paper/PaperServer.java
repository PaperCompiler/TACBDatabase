package de.papercompiler.tacbdatabase.paper;

import de.papercompiler.tacbdatabase.platform.Server;

/**
 * Paper server wrapper.
 */
public class PaperServer implements Server {

    private final org.bukkit.Server bukkitServer;

    public PaperServer(org.bukkit.Server bukkitServer) {
        this.bukkitServer = bukkitServer;
    }

    @Override
    public String getName() {
        return bukkitServer.getName();
    }

    @Override
    public boolean isProxy() {
        return false;
    }
}
