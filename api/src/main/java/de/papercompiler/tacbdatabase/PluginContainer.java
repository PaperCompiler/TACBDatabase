package de.papercompiler.tacbdatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for managing TACBDatabase instances across plugins.
 * <p>
 * This class provides a registry where plugins can register their database
 * instances, allowing other plugins to access them for cross-plugin data sharing.
 * <p>
 * Usage:
 * <pre>
 *     // In your plugin's onEnable
 *     PluginContainer.register("myplugin", database);
 *     
 *     // In another plugin
 *     TACBDatabase db = PluginContainer.get("myplugin");
 *     if (db != null) {
 *         db.getPlayerRepository().findByUuid(uuid);
 *     }
 * </pre>
 */
public final class PluginContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginContainer.class);
    
    private static final Map<String, TACBDatabase> INSTANCES = new ConcurrentHashMap<>();

    private PluginContainer() {
    }

    /**
     * Registers a TACBDatabase instance for cross-plugin access.
     *
     * @param pluginId the unique plugin identifier
     * @param database the TACBDatabase instance to register
     * @return true if registration succeeded, false if already registered
     */
    public static boolean register(String pluginId, TACBDatabase database) {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalArgumentException("Plugin ID cannot be null or empty");
        }
        if (database == null) {
            throw new IllegalArgumentException("Database instance cannot be null");
        }
        
        TACBDatabase previous = INSTANCES.putIfAbsent(pluginId, database);
        if (previous != null) {
            LOGGER.warn("Plugin '{}' is already registered, ignoring", pluginId);
            return false;
        }
        
        LOGGER.info("Registered TACBDatabase for plugin '{}'", pluginId);
        return true;
    }

    /**
     * Gets a registered TACBDatabase instance by plugin ID.
     *
     * @param pluginId the plugin identifier
     * @return the TACBDatabase instance, or null if not found
     */
    public static TACBDatabase get(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return null;
        }
        return INSTANCES.get(pluginId);
    }

    /**
     * Unregisters a TACBDatabase instance.
     *
     * @param pluginId the plugin identifier
     * @return the unregistered instance, or null if not found
     */
    public static TACBDatabase unregister(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return null;
        }
        
        TACBDatabase removed = INSTANCES.remove(pluginId);
        if (removed != null) {
            LOGGER.info("Unregistered TACBDatabase for plugin '{}'", pluginId);
        }
        return removed;
    }

    /**
     * Checks if a plugin is registered.
     *
     * @param pluginId the plugin identifier
     * @return true if the plugin is registered
     */
    public static boolean isRegistered(String pluginId) {
        return pluginId != null && INSTANCES.containsKey(pluginId);
    }

    /**
     * Gets all registered plugin IDs.
     *
     * @return a set of registered plugin IDs
     */
    public static Set<String> getRegisteredPlugins() {
        return new HashSet<>(INSTANCES.keySet());
    }

    /**
     * Shuts down all registered database instances.
     * <p>
     * This is typically called during server shutdown.
     */
    public static void shutdownAll() {
        LOGGER.info("Shutting down all registered TACBDatabase instances...");
        
        for (Map.Entry<String, TACBDatabase> entry : INSTANCES.entrySet()) {
            try {
                entry.getValue().shutdown();
                LOGGER.info("Shutdown TACBDatabase for plugin '{}'", entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown TACBDatabase for plugin '{}'", entry.getKey(), e);
            }
        }
        
        INSTANCES.clear();
        LOGGER.info("All TACBDatabase instances shut down");
    }
}