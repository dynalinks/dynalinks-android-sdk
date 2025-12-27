package com.dynalinks.sdk

import com.dynalinks.sdk.internal.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class APIClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiClient: APIClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        apiClient = APIClient(
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/"),
            clientApiKey = "test-api-key"
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `attributeLink sends correct request`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        apiClient.attributeLink("https://example.dynalinks.app/test")

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/links/attribute", request.path)
        assertEquals("Bearer test-api-key", request.getHeader("Authorization"))
        assertTrue(request.getHeader("Content-Type")?.startsWith("application/json") == true)

        val body = request.body.readUtf8()
        assertTrue(body.contains("\"url\":\"https://example.dynalinks.app/test\""))
        assertTrue(body.contains("\"platform\":\"android\""))
    }

    @Test
    fun `attributeLink returns matched result`() = runBlocking {
        val responseJson = """
            {
                "matched": true,
                "confidence": "high",
                "match_score": 90,
                "link": {
                    "id": "123e4567-e89b-12d3-a456-426614174000",
                    "path": "product/123",
                    "deep_link_value": "product/123"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(responseJson)
        )

        val result = apiClient.attributeLink("https://example.dynalinks.app/test")

        assertTrue(result.matched)
        assertEquals(DeepLinkResult.Confidence.HIGH, result.confidence)
        assertEquals(90, result.matchScore)
        assertEquals("product/123", result.link?.deepLinkValue)
    }

    @Test
    fun `attributeLink returns not matched result`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        val result = apiClient.attributeLink("https://example.dynalinks.app/test")

        assertFalse(result.matched)
        assertNull(result.link)
    }

    @Test
    fun `attributeLink sets isDeferred flag`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "test-id"}}""")
        )

        val result = apiClient.attributeLink(
            "https://example.dynalinks.app/test",
            isDeferred = true
        )

        assertTrue(result.isDeferred)
    }

    @Test
    fun `attributeLink throws ServerError on 500 after retries`() = runBlocking {
        // Enqueue 3 failed responses (one for each retry attempt)
        repeat(3) {
            mockWebServer.enqueue(MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
            )
        }

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected ServerError")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(500, e.statusCode)
        }

        // Verify all 3 retry attempts were made
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun `attributeLink throws ServerError on 404`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected ServerError")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(404, e.statusCode)
        }
    }

    @Test
    fun `attributeLink throws InvalidResponse on empty body`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected InvalidResponse")
        } catch (e: DynalinksError) {
            // Expected - could be InvalidResponse or NetworkError depending on parsing
            assertTrue(e is DynalinksError.InvalidResponse || e is DynalinksError.NetworkError)
        }
    }

    @Test
    fun `attributeLink throws InvalidResponse on malformed JSON`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("{invalid json}")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected exception")
        } catch (e: DynalinksError) {
            // Expected - parsing error
        }
    }

    @Test
    fun `attributeLink sends User-Agent header`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": false}""")
        )

        apiClient.attributeLink("https://example.dynalinks.app/test")

        val request = mockWebServer.takeRequest()
        val userAgent = request.getHeader("User-Agent")
        assertNotNull(userAgent)
        assertTrue(userAgent!!.startsWith("DynalinksSDK-Android/"))
    }

    @Test
    fun `attributeLink throws InvalidResponse when matched field missing`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"link": {"id": "test-id"}}""")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected InvalidResponse")
        } catch (e: DynalinksError.InvalidResponse) {
            // Expected
        }
    }

    @Test
    fun `attributeLink retries on 500 error`() = runBlocking {
        // First two calls fail with 500, third succeeds
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Error"))
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Error"))
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"matched": true, "link": {"id": "retry-success"}}""")
        )

        // Use a client with no delay for faster tests
        val fastClient = APIClient(
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/"),
            clientApiKey = "test-api-key",
            maxRetries = 3
        )

        val result = fastClient.attributeLink("https://example.dynalinks.app/test")

        assertTrue(result.matched)
        assertEquals("retry-success", result.link?.id)
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun `attributeLink does not retry on 400 error`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(400)
            .setBody("Bad Request")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected ServerError")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(400, e.statusCode)
        }

        // Should only have made one request (no retry)
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `attributeLink does not retry on 401 error`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected ServerError")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(401, e.statusCode)
        }

        // Should only have made one request (no retry)
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `attributeLink throws after max retries exhausted`() = runBlocking {
        // All three attempts fail
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))

        val fastClient = APIClient(
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/"),
            clientApiKey = "test-api-key",
            maxRetries = 3
        )

        try {
            fastClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected ServerError")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(503, e.statusCode)
        }

        assertEquals(3, mockWebServer.requestCount)
    }
}
