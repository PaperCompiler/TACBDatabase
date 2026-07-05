package de.papercompiler.tacbdatabase.platform;

/**
 * Abstraction over the platform-specific server instance.
 */
public interface Server {

    /**
     * @return the name of the server (e.g., "lobby-1", "proxy")
     */
    String getName();

    /**
     * @return true if this is the proxy/master node
     */
    boolean isProxy();
}
