package de.papercompiler.tacbdatabase.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EconomyTest {

    @Test
    void constructor_WithUuidBalanceAndCurrency_SetsFieldsAndMarksDirty() {
        UUID uuid = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("100.50");
        String currency = "USD";

        Economy economy = new Economy(uuid, balance, currency);

        assertEquals(uuid, economy.getUuid());
        assertEquals(balance, economy.getBalance());
        assertEquals(currency, economy.getCurrency());
        assertNotNull(economy.getCreatedAt());
        assertNotNull(economy.getUpdatedAt());
        assertTrue(economy.isDirty());
    }

    @Test
    void noArgConstructor_CreatesEmptyEconomy() {
        Economy economy = new Economy();

        assertNull(economy.getId());
        assertNull(economy.getUuid());
        assertNull(economy.getBalance());
        assertNull(economy.getCurrency());
        assertFalse(economy.isDirty());
    }

    @Test
    void parameterizedConstructor_SetsDirtyFlag() {
        Economy economy = new Economy(UUID.randomUUID(), new BigDecimal("100"), "USD");

        assertTrue(economy.isDirty());
    }

    @Test
    void setBalance_UpdatesAndMarksDirty() {
        Economy economy = new Economy();
        economy.setDirty(false);

        BigDecimal newBalance = new BigDecimal("250.75");
        economy.setBalance(newBalance);

        assertEquals(newBalance, economy.getBalance());
        assertTrue(economy.isDirty());
    }

    @Test
    void setCurrency_UpdatesAndMarksDirty() {
        Economy economy = new Economy();
        economy.setDirty(false);

        economy.setCurrency("EUR");

        assertEquals("EUR", economy.getCurrency());
        assertTrue(economy.isDirty());
    }

    @Test
    void equals_SameId_ReturnsTrue() {
        Economy e1 = new Economy();
        e1.setId(1L);

        Economy e2 = new Economy();
        e2.setId(1L);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        Economy e1 = new Economy();
        e1.setId(1L);

        Economy e2 = new Economy();
        e2.setId(2L);

        assertNotEquals(e1, e2);
    }

    @Test
    void equals_NullId_ReturnsFalse() {
        Economy e1 = new Economy();
        Economy e2 = new Economy();

        assertNotEquals(e1, e2);
    }

    @Test
    void setDirty_CanToggleDirtyFlag() {
        Economy economy = new Economy();
        assertFalse(economy.isDirty());

        economy.setDirty(true);
        assertTrue(economy.isDirty());

        economy.setDirty(false);
        assertFalse(economy.isDirty());
    }
}
