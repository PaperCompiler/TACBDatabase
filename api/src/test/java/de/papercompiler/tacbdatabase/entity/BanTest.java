package de.papercompiler.tacbdatabase.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BanTest {

    @Test
    void constructor_WithRequiredFields_SetsFieldsAndMarksDirty() {
        UUID targetUuid = UUID.randomUUID();
        UUID sourceUuid = UUID.randomUUID();
        String reason = "Griefing";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        Ban ban = new Ban(targetUuid, sourceUuid, reason, expiresAt);

        assertEquals(targetUuid, ban.getTargetUuid());
        assertNull(ban.getTargetIp());
        assertEquals(sourceUuid, ban.getSourceUuid());
        assertEquals(reason, ban.getReason());
        assertEquals(expiresAt, ban.getExpiresAt());
        assertNotNull(ban.getCreatedAt());
        assertNotNull(ban.getUpdatedAt());
        assertTrue(ban.isDirty());
    }

    @Test
    void noArgConstructor_CreatesEmptyBan() {
        Ban ban = new Ban();

        assertNull(ban.getId());
        assertNull(ban.getTargetUuid());
        assertNull(ban.getTargetIp());
        assertNull(ban.getSourceUuid());
        assertNull(ban.getReason());
        assertNull(ban.getExpiresAt());
        assertFalse(ban.isDirty());
    }

    @Test
    void parameterizedConstructor_SetsDirtyFlag() {
        Ban ban = new Ban(UUID.randomUUID(), UUID.randomUUID(), "Griefing", Instant.now().plusSeconds(3600));

        assertTrue(ban.isDirty());
    }

    @Test
    void setReason_UpdatesAndMarksDirty() {
        Ban ban = new Ban();
        ban.setDirty(false);

        ban.setReason("Hacking");

        assertEquals("Hacking", ban.getReason());
        assertTrue(ban.isDirty());
    }

    @Test
    void setExpiresAt_UpdatesAndMarksDirty() {
        Ban ban = new Ban();
        ban.setDirty(false);

        Instant expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60);
        ban.setExpiresAt(expiresAt);

        assertEquals(expiresAt, ban.getExpiresAt());
        assertTrue(ban.isDirty());
    }

    @Test
    void setTargetIp_UpdatesWithoutMarkingDirty() {
        Ban ban = new Ban();
        ban.setDirty(false);

        ban.setTargetIp("192.168.1.1");

        assertEquals("192.168.1.1", ban.getTargetIp());
        // setTargetIp does not call markDirty()
        assertFalse(ban.isDirty());
    }

    @Test
    void equals_SameId_ReturnsTrue() {
        Ban b1 = new Ban();
        b1.setId(1L);

        Ban b2 = new Ban();
        b2.setId(1L);

        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        Ban b1 = new Ban();
        b1.setId(1L);

        Ban b2 = new Ban();
        b2.setId(2L);

        assertNotEquals(b1, b2);
    }

    @Test
    void equals_NullId_ReturnsFalse() {
        Ban b1 = new Ban();
        Ban b2 = new Ban();

        assertNotEquals(b1, b2);
    }

    @Test
    void setDirty_CanToggleDirtyFlag() {
        Ban ban = new Ban();
        assertFalse(ban.isDirty());

        ban.setDirty(true);
        assertTrue(ban.isDirty());

        ban.setDirty(false);
        assertFalse(ban.isDirty());
    }
}
