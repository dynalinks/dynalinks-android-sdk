package com.dynalinks.sdk

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class LinkDataTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(LinkData::class.java)

    @Test
    fun `deserialize full link data`() {
        val json = """
            {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "name": "Product Link",
                "path": "product/123",
                "shortened_path": "abc123",
                "url": "https://example.com/product/123",
                "full_url": "https://project.dynalinks.app/product/123",
                "deep_link_value": "product/123",
                "android_fallback_url": "https://play.google.com/store/apps/details?id=com.example",
                "ios_fallback_url": "https://apps.apple.com/app/id123456",
                "enable_forced_redirect": true,
                "social_title": "Check out this product",
                "social_description": "Amazing product description",
                "social_image_url": "https://example.com/image.jpg",
                "clicks": 42,
                "ios_deferred_deep_linking_enabled": true,
                "referrer": "utm_source=facebook&utm_campaign=summer",
                "provider_token": "12345678",
                "campaign_token": "summer_sale"
            }
        """.trimIndent()

        val linkData = adapter.fromJson(json)!!

        assertEquals("123e4567-e89b-12d3-a456-426614174000", linkData.id)
        assertEquals("Product Link", linkData.name)
        assertEquals("product/123", linkData.path)
        assertEquals("abc123", linkData.shortenedPath)
        assertEquals("https://example.com/product/123", linkData.url)
        assertEquals("https://project.dynalinks.app/product/123", linkData.fullUrl)
        assertEquals("product/123", linkData.deepLinkValue)
        assertEquals("https://play.google.com/store/apps/details?id=com.example", linkData.androidFallbackUrl)
        assertEquals("https://apps.apple.com/app/id123456", linkData.iosFallbackUrl)
        assertEquals(true, linkData.enableForcedRedirect)
        assertEquals("Check out this product", linkData.socialTitle)
        assertEquals("Amazing product description", linkData.socialDescription)
        assertEquals("https://example.com/image.jpg", linkData.socialImageUrl)
        assertEquals(42, linkData.clicks)
        assertEquals(true, linkData.iosDeferredDeepLinkingEnabled)
        assertEquals("utm_source=facebook&utm_campaign=summer", linkData.referrer)
        assertEquals("12345678", linkData.providerToken)
        assertEquals("summer_sale", linkData.campaignToken)
    }

    @Test
    fun `deserialize minimal link data`() {
        val json = """{"id": "123e4567-e89b-12d3-a456-426614174000"}"""
        val linkData = adapter.fromJson(json)!!

        assertEquals("123e4567-e89b-12d3-a456-426614174000", linkData.id)
        assertNull(linkData.name)
        assertNull(linkData.path)
        assertNull(linkData.deepLinkValue)
    }

    @Test
    fun `serialize link data`() {
        val linkData = LinkData(
            id = "123e4567-e89b-12d3-a456-426614174000",
            name = "Test Link",
            deepLinkValue = "test/value"
        )

        val json = adapter.toJson(linkData)

        assertTrue(json.contains("\"id\":\"123e4567-e89b-12d3-a456-426614174000\""))
        assertTrue(json.contains("\"name\":\"Test Link\""))
        assertTrue(json.contains("\"deep_link_value\":\"test/value\""))
    }
}
