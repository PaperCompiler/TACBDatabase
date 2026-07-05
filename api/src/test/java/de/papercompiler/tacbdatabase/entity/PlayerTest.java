package de.papercompiler.tacbdatabase.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void constructor_WithUuidAndName_SetsFieldsAndMarksDirty() {
        UUID uuid = UUID.randomUUID();
        String name = "TestPlayer";

        Player player = new Player(uuid, name);

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
        Player player = new Player();

        assertNull(player.getId());
        assertNull(player.getUuid());
        assertNull(player.getName());
        assertEquals(0, player.getPlaytimeTicks());
        assertFalse(player.isDirty());
    }

    @Test
    void parameterizedConstructor_SetsDirtyFlag() {
        Player player = new Player(UUID.randomUUID(), "Test");

        assertTrue(player.isDirty());
    }

    @Test
    void setName_UpdatesNameAndMarksDirty() {
        Player player = new Player();
        player.setDirty(false);

        player.setName("NewName");

        assertEquals("NewName", player.getName());
        assertTrue(player.isDirty());
        assertNotNull(player.getUpdatedAt());
    }

    @Test
    void setLastServer_UpdatesAndMarksDirty() {
        Player player = new Player();
        player.setDirty(false);

        player.setLastServer("lobby-1");

        assertEquals("lobby-1", player.getLastServer());
        assertTrue(player.isDirty());
    }

    @Test
    void setPlaytimeTicks_UpdatesAndMarksDirty() {
        Player player = new Player();
        player.setDirty(false);

        player.setPlaytimeTicks(1000);

        assertEquals(1000, player.getPlaytimeTicks());
        assertTrue(player.isDirty());
    }

    @Test
    void equals_SameId_ReturnsTrue() {
        Player p1 = new Player();
        p1.setId(1L);

        Player p2 = new Player();
        p2.setId(1L);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        Player p1 = new Player();
        p1.setId(1L);

        Player p2 = new Player();
        p2.setId(2L);

        assertNotEquals(p1, p2);
    }

    @Test
    void equals_NullId_ReturnsFalse() {
        Player p1 = new Player();
        Player p2 = new Player();

        assertNotEquals(p1, p2);
    }

    @Test
    void equals_SameInstance_ReturnsTrue() {
        Player p1 = new Player();
        p1.setId(1L);

        assertEquals(p1, p1);
    }

    @Test
    void equals_NullObject_ReturnsFalse() {
        Player p1 = new Player();
        p1.setId(1L);

        assertNotEquals(p1, null);
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        Player p1 = new Player();
        p1.setId(1L);

        assertNotEquals(p1, "not a player");
    }

    @Test
    void hashCode_NullId_UsesIdentityHashCode() {
        Player p1 = new Player();
        Player p2 = new Player();

        // Both should have identity hash codes (different from each other)
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void setDirty_CanToggleDirtyFlag() {
        Player player = new Player();
        assertFalse(player.isDirty());

        player.setDirty(true);
        assertTrue(player.isDirty());

        player.setDirty(false);
        assertFalse(player.isDirty());
    }
}
