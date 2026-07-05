package de.papercompiler.tacbdatabase.platform;

/**
 * Represents the type of Minecraft platform TACBDatabase is running on.
 */
public enum PlatformType {
    /**
     * Paper / Spigot / Bukkit server
     */
    PAPER,

    /**
     * Velocity proxy
     */
    VELOCITY,

    /**
     * Minestom server
     */
    MINESTOM;

    /**
     * Detects the platform type from a {@link Platform} instance.
     *
     * @param platform the platform
     * @return the detected platform type
     * @throws IllegalArgumentException if the platform type cannot be determined
     */
    public static PlatformType fromPlatform(Platform platform) {
        String className = platform.getClass().getName();

        if (className.contains("paper") || className.contains("bukkit") || className.contains("spigot")) {
            return PAPER;
        } else if (className.contains("velocity")) {
            return VELOCITY;
        } else if (className.contains("minestom")) {
            return MINESTOM;
        }

        throw new IllegalArgumentException("Cannot detect platform type from: " + className);
    }
}
