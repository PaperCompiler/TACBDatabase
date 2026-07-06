package de.papercompiler.tacbdatabase.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void constructor_WithUuidAndName_SetsFieldsAndMarksDirty() {
        UUID uuid = UUID.randomUUID();
        String name = "TestPlayer";

        TACBPlayer player = new TACBPlayer(uuid, name);

        assertEquals(uuid, player.getUuid());
        assertEquals(name, player.getName());
        assertNotNull(player.getFirstJoin());
        assertNotNull(player.getLastJoin());
        assertNotNull(player.getCreatedAt());
        assertNotNull(player.getUpdatedAt());
        assertTrue(player.isDirty());
    }

    @Test
    void noArgConstructor_CreatesEmptyPlayer() {
        TACBPlayer player = new TACBPlayer();

        assertNull(player.getId());
        assertNull(player.getUuid());
        assertNull(player.getName());
        assertEquals(0, player.getPlaytimeTicks());
        assertFalse(player.isDirty());
    }

    @Test
    void parameterizedConstructor_SetsDirtyFlag() {
        TACBPlayer player = new TACBPlayer(UUID.randomUUID(), "Test");

        assertTrue(player.isDirty());
    }

    @Test
    void setName_UpdatesNameAndMarksDirty() {
        TACBPlayer player = new TACBPlayer();
        player.setDirty(false);

        player.setName("NewName");

        assertEquals("NewName", player.getName());
        assertTrue(player.isDirty());
        assertNotNull(player.getUpdatedAt());
    }

    @Test
    void setLastServer_UpdatesAndMarksDirty() {
        TACBPlayer player = new TACBPlayer();
        player.setDirty(false);

        player.setLastServer("lobby-1");

        assertEquals("lobby-1", player.getLastServer());
        assertTrue(player.isDirty());
    }

    @Test
    void setPlaytimeTicks_UpdatesAndMarksDirty() {
        TACBPlayer player = new TACBPlayer();
        player.setDirty(false);

        player.setPlaytimeTicks(1000);

        assertEquals(1000, player.getPlaytimeTicks());
        assertTrue(player.isDirty());
    }

    @Test
    void equals_SameId_ReturnsTrue() {
        TACBPlayer p1 = new TACBPlayer();
        p1.setId(1L);

        TACBPlayer p2 = new TACBPlayer();
        p2.setId(1L);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        TACBPlayer p1 = new TACBPlayer();
        p1.setId(1L);

        TACBPlayer p2 = new TACBPlayer();
        p2.setId(2L);

        assertNotEquals(p1, p2);
    }

    @Test
    void equals_NullId_ReturnsFalse() {
        TACBPlayer p1 = new TACBPlayer();
        TACBPlayer p2 = new TACBPlayer();

        assertNotEquals(p1, p2);
    }

    @Test
    void equals_SameInstance_ReturnsTrue() {
        TACBPlayer p1 = new TACBPlayer();
        p1.setId(1L);

        assertEquals(p1, p1);
    }

    @Test
    void equals_NullObject_ReturnsFalse() {
        TACBPlayer p1 = new TACBPlayer();
        p1.setId(1L);

        assertNotEquals(p1, null);
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        TACBPlayer p1 = new TACBPlayer();
        p1.setId(1L);

        assertNotEquals(p1, "not a player");
    }

    @Test
    void hashCode_NullId_UsesIdentityHashCode() {
        TACBPlayer p1 = new TACBPlayer();
        TACBPlayer p2 = new TACBPlayer();

        // Both should have identity hash codes (different from each other)
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void setDirty_CanToggleDirtyFlag() {
        TACBPlayer player = new TACBPlayer();
        assertFalse(player.isDirty());

        player.setDirty(true);
        assertTrue(player.isDirty());

        player.setDirty(false);
        assertFalse(player.isDirty());
    }
}
