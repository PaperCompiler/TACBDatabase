package de.papercompiler.tacbdatabase.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HomeTest {

    @Test
    void constructor_WithAllFields_SetsFieldsAndMarksDirty() {
        UUID uuid = UUID.randomUUID();
        String name = "Home1";
        String world = "world";
        double x = 100.5;
        double y = 64.0;
        double z = -200.3;
        float yaw = 90.0f;
        float pitch = 0.0f;

        Home home = new Home(uuid, name, world, x, y, z, yaw, pitch);

        assertEquals(uuid, home.getUuid());
        assertEquals(name, home.getName());
        assertEquals(world, home.getWorld());
        assertEquals(x, home.getX());
        assertEquals(y, home.getY());
        assertEquals(z, home.getZ());
        assertEquals(yaw, home.getYaw());
        assertEquals(pitch, home.getPitch());
        assertNotNull(home.getCreatedAt());
        assertNotNull(home.getUpdatedAt());
        assertTrue(home.isDirty());
    }

    @Test
    void noArgConstructor_CreatesEmptyHome() {
        Home home = new Home();

        assertNull(home.getId());
        assertNull(home.getUuid());
        assertNull(home.getName());
        assertNull(home.getWorld());
        assertEquals(0.0, home.getX());
        assertEquals(0.0, home.getY());
        assertEquals(0.0, home.getZ());
        assertEquals(0.0f, home.getYaw());
        assertEquals(0.0f, home.getPitch());
        assertFalse(home.isDirty());
    }

    @Test
    void parameterizedConstructor_SetsDirtyFlag() {
        Home home = new Home(UUID.randomUUID(), "Home1", "world", 100, 64, -200, 90, 0);

        assertTrue(home.isDirty());
    }

    @Test
    void setX_UpdatesAndMarksDirty() {
        Home home = new Home();
        home.setDirty(false);

        home.setX(50.0);

        assertEquals(50.0, home.getX());
        assertTrue(home.isDirty());
    }

    @Test
    void setY_UpdatesAndMarksDirty() {
        Home home = new Home();
        home.setDirty(false);

        home.setY(70.0);

        assertEquals(70.0, home.getY());
        assertTrue(home.isDirty());
    }

    @Test
    void setZ_UpdatesAndMarksDirty() {
        Home home = new Home();
        home.setDirty(false);

        home.setZ(-30.0);

        assertEquals(-30.0, home.getZ());
        assertTrue(home.isDirty());
    }

    @Test
    void setYaw_UpdatesAndMarksDirty() {
        Home home = new Home();
        home.setDirty(false);

        home.setYaw(180.0f);

        assertEquals(180.0f, home.getYaw());
        assertTrue(home.isDirty());
    }

    @Test
    void setPitch_UpdatesAndMarksDirty() {
        Home home = new Home();
        home.setDirty(false);

        home.setPitch(45.0f);

        assertEquals(45.0f, home.getPitch());
        assertTrue(home.isDirty());
    }

    @Test
    void equals_SameId_ReturnsTrue() {
        Home h1 = new Home();
        h1.setId(1L);

        Home h2 = new Home();
        h2.setId(1L);

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        Home h1 = new Home();
        h1.setId(1L);

        Home h2 = new Home();
        h2.setId(2L);

        assertNotEquals(h1, h2);
    }

    @Test
    void equals_NullId_ReturnsFalse() {
        Home h1 = new Home();
        Home h2 = new Home();

        assertNotEquals(h1, h2);
    }

    @Test
    void setDirty_CanToggleDirtyFlag() {
        Home home = new Home();
        assertFalse(home.isDirty());

        home.setDirty(true);
        assertTrue(home.isDirty());

        home.setDirty(false);
        assertFalse(home.isDirty());
    }
}
