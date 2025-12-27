package com.dynalinks.sdk

import android.net.Uri
import com.dynalinks.sdk.internal.APIClient
import com.dynalinks.sdk.internal.DynalinksStorage
import com.dynalinks.sdk.internal.ReferrerUrlProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive tests for the Dynalinks SDK.
 * Tests DynalinksInstance directly to avoid test hooks in production code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DynalinksTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var storage: FakeStorage
    private lateinit var apiClient: APIClient

    private var referrerUrl: String? = null

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        storage = FakeStorage()
        referrerUrl = null

        apiClient = APIClient(
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/"),
            clientApiKey = "test-api-key"
        )

        Dynalinks.clearInstance()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        Dynalinks.clearInstance()
    }

    private fun createInstance(
        allowEmulator: Boolean = true,
        isEmulator: Boolean = false
    ): DynalinksInstance {
        return DynalinksInstance(
            apiClient = apiClient,
            storage = storage,
            referrerUrlProvider = ReferrerUrlProvider { referrerUrl },
            allowEmulator = allowEmulator,
            emulatorChecker = { isEmulator }
        )
    }

    // MARK: - Configuration Tests (Dynalinks singleton)

    @Test
    fun `checkForDeferredDeepLink throws NotConfigured when not configured`() = runBlocking {
        try {
            Dynalinks.checkForDeferredDeepLink()
            fail("Expected NotConfigured error")
        } catch (e: DynalinksError.NotConfigured) {
            // Expected
        }
    }

    @Test
    fun `handleAppLink throws NotConfigured when not configured`() = runBlocking {
        try {
            Dynalinks.handleAppLink(Uri.parse("https://example.com/path"))
            fail("Expected NotConfigured error")
        } catch (e: DynalinksError.NotConfigured) {
            // Expected
        }
    }

    // MARK: - Deferred Deep Link Tests

    @Test
    fun `checkForDeferredDeepLink returns no match when no referrer`() = runBlocking {
        referrerUrl = null
        val instance = createInstance()

        val result = instance.checkForDeferredDeepLink()

        assertFalse(result.matched)
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `checkForDeferredDeepLink returns matched link`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "link-123"}}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result = instance.checkForDeferredDeepLink()

        assertTrue(result.matched)
        assertEquals("link-123", result.link?.id)
    }

    @Test
    fun `checkForDeferredDeepLink returns no match on second call`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result1 = instance.checkForDeferredDeepLink()
        assertFalse(result1.matched)

        val result2 = instance.checkForDeferredDeepLink()
        assertFalse(result2.matched)

        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `checkForDeferredDeepLink returns cached result on second call`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "cached-123"}}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result1 = instance.checkForDeferredDeepLink()
        assertTrue(result1.matched)
        assertEquals("cached-123", result1.link?.id)

        val result2 = instance.checkForDeferredDeepLink()
        assertTrue(result2.matched)
        assertEquals("cached-123", result2.link?.id)

        assertEquals(1, mockWebServer.requestCount)
    }

    // MARK: - Reset Tests

    @Test
    fun `reset allows new check`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "after-reset"}}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result1 = instance.checkForDeferredDeepLink()
        assertFalse(result1.matched)

        instance.reset()

        val result2 = instance.checkForDeferredDeepLink()
        assertTrue(result2.matched)
        assertEquals("after-reset", result2.link?.id)

        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `reset clears cached result`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "to-be-cleared"}}""")
        )
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result1 = instance.checkForDeferredDeepLink()
        assertTrue(result1.matched)

        instance.reset()

        val result2 = instance.checkForDeferredDeepLink()
        assertFalse(result2.matched)
    }

    // MARK: - Error Propagation Tests

    @Test
    fun `checkForDeferredDeepLink propagates server error`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("Invalid API key")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        try {
            instance.checkForDeferredDeepLink()
            fail("Expected server error")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(401, e.statusCode)
        }
    }

    @Test
    fun `checkForDeferredDeepLink propagates rate limit error`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(429)
            .setBody("Rate limit exceeded")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        try {
            instance.checkForDeferredDeepLink()
            fail("Expected server error")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(429, e.statusCode)
        }
    }

    // MARK: - App Link Tests

    @Test
    fun `handleAppLink returns matched link`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "app-link-123", "path": "/test"}}""")
        )

        val instance = createInstance()
        val uri = Uri.parse("https://project.dynalinks.app/test")

        val result = instance.handleAppLink(uri)

        assertTrue(result.matched)
        assertEquals("app-link-123", result.link?.id)
        assertEquals("/test", result.link?.path)
    }

    @Test
    fun `handleAppLink returns no match`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        val instance = createInstance()
        val uri = Uri.parse("https://project.dynalinks.app/nonexistent")

        val result = instance.handleAppLink(uri)

        assertFalse(result.matched)
        assertNull(result.link)
    }

    @Test
    fun `handleAppLink marks as checked skips deferred check`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "app-link-first"}}""")
        )

        referrerUrl = "https://example.dynalinks.app/deferred"
        val instance = createInstance()

        val uri = Uri.parse("https://project.dynalinks.app/path")
        val appLinkResult = instance.handleAppLink(uri)
        assertTrue(appLinkResult.matched)

        val deferredResult = instance.checkForDeferredDeepLink()
        assertTrue(deferredResult.matched)
        assertEquals("app-link-first", deferredResult.link?.id)

        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `handleAppLink caches result`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "cached-app-link"}}""")
        )

        val instance = createInstance()
        val uri = Uri.parse("https://project.dynalinks.app/cached")

        instance.handleAppLink(uri)

        assertTrue(storage.hasCheckedForDeferredDeepLink)
        assertNotNull(storage.cachedResult)
        assertEquals("cached-app-link", storage.cachedResult?.link?.id)
    }

    @Test
    fun `handleAppLink propagates server error`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("Invalid API key")
        )

        val instance = createInstance()
        val uri = Uri.parse("https://example.com/path")

        try {
            instance.handleAppLink(uri)
            fail("Expected server error")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(401, e.statusCode)
        }
    }

    // MARK: - Emulator Detection Tests

    @Test
    fun `checkForDeferredDeepLink throws Emulator when on emulator`() = runBlocking {
        val instance = createInstance(allowEmulator = false, isEmulator = true)

        try {
            instance.checkForDeferredDeepLink()
            fail("Expected Emulator error")
        } catch (e: DynalinksError.Emulator) {
            // Expected
        }
    }

    @Test
    fun `checkForDeferredDeepLink works when allowEmulator is true`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance(allowEmulator = true, isEmulator = true)

        val result = instance.checkForDeferredDeepLink()
        assertFalse(result.matched)
    }

    @Test
    fun `checkForDeferredDeepLink works when not on emulator`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance(allowEmulator = false, isEmulator = false)

        val result = instance.checkForDeferredDeepLink()
        assertFalse(result.matched)
    }

    // MARK: - Full Flow Tests

    @Test
    fun `fullFlow matchFound highConfidence`() = runBlocking {
        val responseJson = """
            {
                "matched": true,
                "confidence": "high",
                "match_score": 92,
                "link": {
                    "id": "e2e-link-123",
                    "name": "E2E Test Link",
                    "path": "/e2e/test",
                    "deep_link_value": "/e2e/test"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(responseJson)
        )

        referrerUrl = "https://example.dynalinks.app/e2e/test"
        val instance = createInstance()

        val result = instance.checkForDeferredDeepLink()

        assertTrue(result.matched)
        assertEquals(DeepLinkResult.Confidence.HIGH, result.confidence)
        assertEquals(92, result.matchScore)
        assertEquals("e2e-link-123", result.link?.id)
        assertEquals("E2E Test Link", result.link?.name)
        assertEquals("/e2e/test", result.link?.deepLinkValue)

        assertTrue(storage.hasCheckedForDeferredDeepLink)
        assertNotNull(storage.cachedResult)
    }

    @Test
    fun `fullFlow noMatch`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result = instance.checkForDeferredDeepLink()

        assertFalse(result.matched)
        assertNull(result.link)

        assertTrue(storage.hasCheckedForDeferredDeepLink)
        assertNull(storage.cachedResult)
    }

    @Test
    fun `fullFlow mediumConfidence`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "confidence": "medium", "match_score": 65, "link": {"id": "medium-123"}}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result = instance.checkForDeferredDeepLink()

        assertTrue(result.matched)
        assertEquals(DeepLinkResult.Confidence.MEDIUM, result.confidence)
        assertEquals(65, result.matchScore)
    }

    @Test
    fun `fullFlow lowConfidence`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "confidence": "low", "match_score": 35, "link": {"id": "low-123"}}""")
        )

        referrerUrl = "https://example.dynalinks.app/test"
        val instance = createInstance()

        val result = instance.checkForDeferredDeepLink()

        assertTrue(result.matched)
        assertEquals(DeepLinkResult.Confidence.LOW, result.confidence)
        assertEquals(35, result.matchScore)
    }

    // MARK: - Test Fakes

    private class FakeStorage : DynalinksStorage {
        override var hasCheckedForDeferredDeepLink: Boolean = false
        override var cachedResult: DeepLinkResult? = null

        override fun reset() {
            hasCheckedForDeferredDeepLink = false
            cachedResult = null
        }
    }
}
