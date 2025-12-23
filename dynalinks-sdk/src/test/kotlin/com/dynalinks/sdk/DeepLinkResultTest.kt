package com.dynalinks.sdk

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DeepLinkResultTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(DeepLinkResult::class.java)

    @Test
    fun `notMatched creates result with matched false`() {
        val result = DeepLinkResult.notMatched()
        assertFalse(result.matched)
        assertNull(result.confidence)
        assertNull(result.matchScore)
        assertNull(result.link)
        assertFalse(result.isDeferred)
    }

    @Test
    fun `notMatched with isDeferred true sets isDeferred`() {
        val result = DeepLinkResult.notMatched(isDeferred = true)
        assertFalse(result.matched)
        assertTrue(result.isDeferred)
    }

    @Test
    fun `deserialize matched response`() {
        val json = """
            {
                "matched": true,
                "confidence": "high",
                "match_score": 85,
                "link": {
                    "id": "123e4567-e89b-12d3-a456-426614174000",
                    "name": "Test Link",
                    "path": "test-path",
                    "deep_link_value": "product/123"
                }
            }
        """.trimIndent()

        val result = adapter.fromJson(json)!!

        assertTrue(result.matched)
        assertEquals(DeepLinkResult.Confidence.HIGH, result.confidence)
        assertEquals(85, result.matchScore)
        assertNotNull(result.link)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.link?.id)
        assertEquals("Test Link", result.link?.name)
        assertEquals("test-path", result.link?.path)
        assertEquals("product/123", result.link?.deepLinkValue)
    }

    @Test
    fun `deserialize unmatched response`() {
        val json = """{"matched": false}"""
        val result = adapter.fromJson(json)!!

        assertFalse(result.matched)
        assertNull(result.confidence)
        assertNull(result.matchScore)
        assertNull(result.link)
    }

    @Test
    fun `confidence enum values map correctly`() {
        assertEquals(DeepLinkResult.Confidence.HIGH, parseConfidence("high"))
        assertEquals(DeepLinkResult.Confidence.MEDIUM, parseConfidence("medium"))
        assertEquals(DeepLinkResult.Confidence.LOW, parseConfidence("low"))
    }

    private fun parseConfidence(value: String): DeepLinkResult.Confidence? {
        val json = """{"matched": true, "confidence": "$value"}"""
        return adapter.fromJson(json)?.confidence
    }
}
