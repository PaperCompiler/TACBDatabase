package de.papercompiler.tacbdatabase.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UUIDSerializerTest {

    @Test
    void uuidToString_WithValidUuid_ReturnsString() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        String result = UUIDSerializer.uuidToString(uuid);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", result);
    }

    @Test
    void uuidToString_WithNull_ReturnsNull() {
        String result = UUIDSerializer.uuidToString(null);

        assertNull(result);
    }

    @Test
    void stringToUuid_WithValidString_ReturnsUuid() {
        String str = "550e8400-e29b-41d4-a716-446655440000";

        UUID result = UUIDSerializer.stringToUuid(str);

        assertEquals(UUID.fromString(str), result);
    }

    @Test
    void stringToUuid_WithNull_ReturnsNull() {
        UUID result = UUIDSerializer.stringToUuid(null);

        assertNull(result);
    }

    @Test
    void roundTrip_UuidToStringAndBack_PreservesUuid() {
        UUID original = UUID.randomUUID();

        String stringForm = UUIDSerializer.uuidToString(original);
        UUID recovered = UUIDSerializer.stringToUuid(stringForm);

        assertEquals(original, recovered);
    }

    @Test
    void stringToUuid_WithInvalidString_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            UUIDSerializer.stringToUuid("not-a-uuid");
        });
    }
}
