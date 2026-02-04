package com.dynalinks.sdk

import android.util.Base64
import com.dynalinks.sdk.internal.InstallReferrerManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InstallReferrerManagerTest {

    private lateinit var manager: InstallReferrerManager

    @Before
    fun setUp() {
        manager = InstallReferrerManager(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `parseReferrer extracts URL from _url parameter with base64 encoding`() {
        val testUrl = "https://project.dynalinks.app/path"
        val encodedUrl = Base64.encodeToString(testUrl.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
        val referrer = "_url=$encodedUrl"

        // Use reflection to call private method for testing
        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertEquals(testUrl, result)
    }

    @Test
    fun `parseReferrer extracts URL from url parameter (legacy format)`() {
        val testUrl = "https://project.dynalinks.app/path"
        val referrer = "url=${java.net.URLEncoder.encode(testUrl, "UTF-8")}"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertEquals(testUrl, result)
    }

    @Test
    fun `parseReferrer prefers _url over url parameter`() {
        val newFormatUrl = "https://new.dynalinks.app/path"
        val legacyUrl = "https://old.dynalinks.app/path"

        val encodedNewUrl = Base64.encodeToString(newFormatUrl.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
        val encodedLegacyUrl = java.net.URLEncoder.encode(legacyUrl, "UTF-8")
        val referrer = "url=$encodedLegacyUrl&_url=$encodedNewUrl"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertEquals("Should prefer _url parameter over url parameter", newFormatUrl, result)
    }

    @Test
    fun `parseReferrer handles UTM params combined with _url`() {
        val testUrl = "https://project.dynalinks.app/path"
        val encodedUrl = Base64.encodeToString(testUrl.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
        val referrer = "utm_source=email&utm_campaign=test&_url=$encodedUrl"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertEquals(testUrl, result)
    }

    @Test
    fun `parseReferrer returns null for invalid base64 in _url`() {
        val referrer = "_url=invalid-base64!!!"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertNull(result)
    }

    @Test
    fun `parseReferrer returns null for non-HTTP URL in _url`() {
        val nonHttpUrl = "ftp://example.com"
        val encodedUrl = Base64.encodeToString(nonHttpUrl.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
        val referrer = "_url=$encodedUrl"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertNull(result)
    }

    @Test
    fun `parseReferrer returns null for non-HTTP URL in url parameter`() {
        val nonHttpUrl = "ftp://example.com"
        val referrer = "url=${java.net.URLEncoder.encode(nonHttpUrl, "UTF-8")}"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertNull(result)
    }

    @Test
    fun `parseReferrer returns null for empty referrer`() {
        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, "") as String?

        assertNull(result)
    }

    @Test
    fun `parseReferrer returns null for referrer without url or _url`() {
        val referrer = "utm_source=email&utm_campaign=test"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertNull(result)
    }

    @Test
    fun `parseReferrer handles URL with query parameters in _url`() {
        val testUrl = "https://project.dynalinks.app/path?foo=bar&baz=qux"
        val encodedUrl = Base64.encodeToString(testUrl.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
        val referrer = "_url=$encodedUrl"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertEquals(testUrl, result)
    }

    @Test
    fun `parseReferrer handles http scheme (not just https)`() {
        val testUrl = "http://project.dynalinks.app/path"
        val encodedUrl = Base64.encodeToString(testUrl.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
        val referrer = "_url=$encodedUrl"

        val method = InstallReferrerManager::class.java.getDeclaredMethod("parseReferrer", String::class.java)
        method.isAccessible = true
        val result = method.invoke(manager, referrer) as String?

        assertEquals(testUrl, result)
    }
}
