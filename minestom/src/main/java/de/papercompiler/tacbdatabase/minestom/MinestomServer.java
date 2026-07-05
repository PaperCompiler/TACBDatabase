package de.papercompiler.tacbdatabase.minestom;

import de.papercompiler.tacbdatabase.platform.Server;
import net.minestom.server.MinecraftServer;

/**
 * Minestom server wrapper.
 */
public class MinestomServer implements Server {

    private final MinecraftServer minecraftServer;

    public MinestomServer(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    @Override
    public String getName() {
        return "minestom-server";
    }

    @Override
    public boolean isProxy() {
        return false;
    }
}
