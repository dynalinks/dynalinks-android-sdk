package com.dynalinks.sdk

import org.junit.Assert.*
import org.junit.Test

class DynalinksErrorTest {

    @Test
    fun `NotConfigured has correct message`() {
        val error = DynalinksError.NotConfigured
        assertEquals(
            "Dynalinks SDK not configured. Call Dynalinks.configure() first.",
            error.message
        )
    }

    @Test
    fun `InvalidAPIKey includes reason in message`() {
        val error = DynalinksError.InvalidAPIKey("Key must be a valid UUID")
        assertEquals("Key must be a valid UUID", error.message)
    }

    @Test
    fun `Emulator has correct message`() {
        val error = DynalinksError.Emulator
        assertEquals("Deferred deep linking not available on emulator.", error.message)
    }

    @Test
    fun `InvalidIntent has correct message`() {
        val error = DynalinksError.InvalidIntent
        assertEquals("Intent does not contain valid deep link data.", error.message)
    }

    @Test
    fun `NetworkError includes cause message`() {
        val cause = RuntimeException("Connection timeout")
        val error = DynalinksError.NetworkError(cause)
        assertEquals("Network request failed: Connection timeout", error.message)
    }

    @Test
    fun `NetworkError handles null cause`() {
        val error = DynalinksError.NetworkError(null)
        assertEquals("Network request failed: unknown error", error.message)
    }

    @Test
    fun `InvalidResponse has correct message`() {
        val error = DynalinksError.InvalidResponse
        assertEquals("Invalid response from server.", error.message)
    }

    @Test
    fun `ServerError includes status code and message`() {
        val error = DynalinksError.ServerError(500, "Internal Server Error")
        assertEquals("Server error (500): Internal Server Error", error.message)
    }

    @Test
    fun `ServerError handles null message`() {
        val error = DynalinksError.ServerError(404, null)
        assertEquals("Server error: 404", error.message)
    }

    @Test
    fun `NoMatch has correct message`() {
        val error = DynalinksError.NoMatch
        assertEquals("No matching deferred deep link found.", error.message)
    }

    @Test
    fun `InstallReferrerUnavailable has correct message`() {
        val error = DynalinksError.InstallReferrerUnavailable
        assertEquals("Install Referrer API is not available.", error.message)
    }

    @Test
    fun `InstallReferrerTimeout has correct message`() {
        val error = DynalinksError.InstallReferrerTimeout
        assertEquals("Install Referrer connection timed out.", error.message)
    }

    @Test
    fun `object errors are singletons`() {
        assertSame(DynalinksError.NotConfigured, DynalinksError.NotConfigured)
        assertSame(DynalinksError.Emulator, DynalinksError.Emulator)
        assertSame(DynalinksError.InvalidIntent, DynalinksError.InvalidIntent)
        assertSame(DynalinksError.InvalidResponse, DynalinksError.InvalidResponse)
        assertSame(DynalinksError.NoMatch, DynalinksError.NoMatch)
    }

    @Test
    fun `errors are instances of Exception`() {
        // Verify that errors can be used as Exception (catches work, message accessible)
        val errors: List<Exception> = listOf(
            DynalinksError.NotConfigured,
            DynalinksError.NetworkError(null),
            DynalinksError.ServerError(500, null)
        )
        assertEquals(3, errors.size)
        errors.forEach { error ->
            assertNotNull(error.message)
        }
    }
}
