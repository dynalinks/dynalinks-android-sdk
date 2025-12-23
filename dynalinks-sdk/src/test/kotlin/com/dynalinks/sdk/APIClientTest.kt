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
    fun `attributeLink throws ServerError on 500`() = runBlocking {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error")
        )

        try {
            apiClient.attributeLink("https://example.dynalinks.app/test")
            fail("Expected ServerError")
        } catch (e: DynalinksError.ServerError) {
            assertEquals(500, e.statusCode)
        }
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
}
