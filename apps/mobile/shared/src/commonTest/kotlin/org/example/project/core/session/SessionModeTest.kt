package org.example.project.core.session

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionModeTest {

    @Test
    fun resolveSeed() {
        assertEquals(SessionMode.SEED, SessionMode.resolve("seed"))
        assertEquals(SessionMode.SEED, SessionMode.resolve("SEED"))
        assertEquals(SessionMode.SEED, SessionMode.resolve("Seed"))
    }

    @Test
    fun resolveGuest() {
        assertEquals(SessionMode.GUEST, SessionMode.resolve("guest"))
        assertEquals(SessionMode.GUEST, SessionMode.resolve("GUEST"))
    }

    @Test
    fun resolveDefaultToSeed() {
        assertEquals(SessionMode.SEED, SessionMode.resolve(null))
        assertEquals(SessionMode.SEED, SessionMode.resolve(""))
        assertEquals(SessionMode.SEED, SessionMode.resolve("unknown"))
    }

    @Test
    fun nameReturnsLowercase() {
        assertEquals("seed", SessionMode.SEED.name())
        assertEquals("guest", SessionMode.GUEST.name())
    }
}
