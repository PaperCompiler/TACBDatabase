package de.papercompiler.tacbdatabase.util;

import java.util.UUID;

/**
 * Utility class for UUID serialization.
 * <p>
 * ORMLite can handle UUID serialization automatically using the STRING type.
 * This class provides helper methods for manual conversion if needed.
 */
public class UUIDSerializer {

    private UUIDSerializer() {
    }

    /**
     * Converts a UUID to its string representation for database storage.
     *
     * @param uuid the UUID to convert
     * @return the string representation, or null if uuid is null
     */
    public static String uuidToString(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    /**
     * Converts a string to a UUID.
     *
     * @param str the string to convert
     * @return the UUID, or null if str is null
     */
    public static UUID stringToUuid(String str) {
        return str == null ? null : UUID.fromString(str);
    }
}
