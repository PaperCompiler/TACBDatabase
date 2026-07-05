package de.papercompiler.tacbdatabase.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GuildTest {

    @Test
    void constructor_WithNameTagAndOwner_SetsFieldsAndMarksDirty() {
        String name = "TestGuild";
        String tag = "TG";
        UUID ownerUuid = UUID.randomUUID();

        Guild guild = new Guild(name, tag, ownerUuid);

        assertEquals(name, guild.getName());
        assertEquals(tag, guild.getTag());
        assertEquals(ownerUuid, guild.getOwnerUuid());
        assertNotNull(guild.getCreatedAt());
        assertNotNull(guild.getUpdatedAt());
        assertTrue(guild.isDirty());
    }

    @Test
    void noArgConstructor_CreatesEmptyGuild() {
        Guild guild = new Guild();

        assertNull(guild.getId());
        assertNull(guild.getName());
        assertNull(guild.getTag());
        assertNull(guild.getOwnerUuid());
        assertNull(guild.getDescription());
        assertFalse(guild.isDirty());
    }

    @Test
    void parameterizedConstructor_SetsDirtyFlag() {
        Guild guild = new Guild("Test", "T", UUID.randomUUID());

        assertTrue(guild.isDirty());
    }

    @Test
    void setName_UpdatesNameAndMarksDirty() {
        Guild guild = new Guild();
        guild.setDirty(false);

        guild.setName("NewGuild");

        assertEquals("NewGuild", guild.getName());
        assertTrue(guild.isDirty());
    }

    @Test
    void setTag_UpdatesTagAndMarksDirty() {
        Guild guild = new Guild();
        guild.setDirty(false);

        guild.setTag("NG");

        assertEquals("NG", guild.getTag());
        assertTrue(guild.isDirty());
    }

    @Test
    void setDescription_UpdatesAndMarksDirty() {
        Guild guild = new Guild();
        guild.setDirty(false);

        guild.setDescription("A test guild");

        assertEquals("A test guild", guild.getDescription());
        assertTrue(guild.isDirty());
    }

    @Test
    void equals_SameId_ReturnsTrue() {
        Guild g1 = new Guild();
        g1.setId(1L);

        Guild g2 = new Guild();
        g2.setId(1L);

        assertEquals(g1, g2);
        assertEquals(g1.hashCode(), g2.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        Guild g1 = new Guild();
        g1.setId(1L);

        Guild g2 = new Guild();
        g2.setId(2L);

        assertNotEquals(g1, g2);
    }

    @Test
    void equals_NullId_ReturnsFalse() {
        Guild g1 = new Guild();
        Guild g2 = new Guild();

        assertNotEquals(g1, g2);
    }

    @Test
    void setDirty_CanToggleDirtyFlag() {
        Guild guild = new Guild();
        assertFalse(guild.isDirty());

        guild.setDirty(true);
        assertTrue(guild.isDirty());

        guild.setDirty(false);
        assertFalse(guild.isDirty());
    }
}
