package com.dynalinks.sdk.internal

import com.dynalinks.sdk.DeepLinkResult
import com.dynalinks.sdk.Dynalinks
import com.dynalinks.sdk.DynalinksError
import com.dynalinks.sdk.LinkData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for communicating with the Dynalinks API.
 */
internal class APIClient(
    private val baseUrl: String,
    private val clientApiKey: String,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val requestAdapter = moshi.adapter(AttributeRequest::class.java)
    private val responseAdapter = moshi.adapter(AttributeResponse::class.java)

    private val userAgent = "DynalinksSDK-Android/${Dynalinks.VERSION}"

    /**
     * Attribute a link URL to get link data.
     * POST /api/v1/links/attribute
     *
     * Retries automatically on 5xx errors and network failures with exponential backoff.
     */
    suspend fun attributeLink(url: String, isDeferred: Boolean = false): DeepLinkResult =
        withContext(Dispatchers.IO) {
            executeWithRetry {
                executeAttributeRequest(url, isDeferred)
            }
        }

    private fun executeAttributeRequest(url: String, isDeferred: Boolean): DeepLinkResult {
        Logger.debug("Attributing link: $url")

        val requestBody = AttributeRequest(url = url, platform = "android", isDeferred = isDeferred)
        val jsonBody = requestAdapter.toJson(requestBody)

        Logger.debug("Request URL: $baseUrl/links/attribute")
        Logger.debug("Authorization: Bearer ${clientApiKey.take(8)}...")
        Logger.debug("Request body: $jsonBody")

        val request = Request.Builder()
            .url("$baseUrl/links/attribute")
            .header("Authorization", "Bearer $clientApiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Logger.error("Server error: ${response.code} - $errorBody")
            throw DynalinksError.ServerError(response.code, errorBody)
        }

        val body = response.body?.string()
            ?: throw DynalinksError.InvalidResponse

        Logger.debug("Response: $body")

        val attributeResponse = try {
            responseAdapter.fromJson(body)
                ?: throw DynalinksError.InvalidResponse
        } catch (e: JsonDataException) {
            Logger.error("Invalid JSON response: missing required field", e)
            throw DynalinksError.InvalidResponse
        }

        return if (attributeResponse.matched && attributeResponse.link != null) {
            // For unnamed links (deepLinkValue is null), parse query params from the URL
            val enrichedLink = if (attributeResponse.link.deepLinkValue == null) {
                enrichLinkDataFromQueryParams(url, attributeResponse.link)
            } else {
                attributeResponse.link
            }

            DeepLinkResult(
                matched = true,
                confidence = attributeResponse.confidence,
                matchScore = attributeResponse.matchScore,
                link = enrichedLink,
                isDeferred = isDeferred
            )
        } else {
            DeepLinkResult.notMatched(isDeferred)
        }
    }

    /**
     * Enrich LinkData with query parameters for unnamed links.
     * Parses query params from the Dynalinks URL and maps them to LinkData fields.
     */
    private fun enrichLinkDataFromQueryParams(url: String, linkData: LinkData): LinkData {
        val uri = try {
            java.net.URI(url)
        } catch (e: Exception) {
            Logger.debug("Failed to parse URL for query params: ${e.message}")
            return linkData
        }

        val query = uri.query ?: return linkData

        // Parse query string into a map
        val queryParams = query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
                } else null
            }
            .toMap()

        if (queryParams.isEmpty()) return linkData

        Logger.debug("Enriching unnamed link with query params: $queryParams")

        return linkData.copy(
            url = queryParams["link"] ?: linkData.url,
            socialTitle = queryParams["st"] ?: linkData.socialTitle,
            socialDescription = queryParams["sd"] ?: linkData.socialDescription,
            socialImageUrl = queryParams["si"] ?: linkData.socialImageUrl,
            enableForcedRedirect = queryParams["efr"]?.toBooleanStrictOrNull() ?: linkData.enableForcedRedirect,
            androidFallbackUrl = queryParams["afl"] ?: linkData.androidFallbackUrl,
            iosFallbackUrl = queryParams["ifl"] ?: linkData.iosFallbackUrl,
            referrer = queryParams["referrer"] ?: linkData.referrer,
            iosDeferredDeepLinkingEnabled = queryParams["ide"]?.toBooleanStrictOrNull()
                ?: linkData.iosDeferredDeepLinkingEnabled
        )
    }

    /**
     * Execute a block with retry logic for transient failures.
     * Retries on 5xx server errors and network failures with exponential backoff.
     * Does NOT retry on 4xx client errors.
     */
    private suspend fun <T> executeWithRetry(block: () -> T): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: DynalinksError.ServerError) {
                if (e.statusCode in 500..599) {
                    Logger.warning("Server error (attempt ${attempt + 1}/$maxRetries): ${e.statusCode}")
                    lastException = e
                    if (attempt < maxRetries - 1) {
                        delay(RETRY_DELAYS.getOrElse(attempt) { RETRY_DELAYS.last() })
                    }
                } else {
                    throw e // Don't retry 4xx errors
                }
            } catch (e: IOException) {
                Logger.warning("Network error (attempt ${attempt + 1}/$maxRetries): ${e.message}")
                lastException = DynalinksError.NetworkError(e)
                if (attempt < maxRetries - 1) {
                    delay(RETRY_DELAYS.getOrElse(attempt) { RETRY_DELAYS.last() })
                }
            } catch (e: DynalinksError) {
                throw e // Don't retry other DynalinksErrors
            } catch (e: Exception) {
                Logger.error("Unexpected error", e)
                throw DynalinksError.NetworkError(e)
            }
        }

        throw lastException ?: DynalinksError.NetworkError(null)
    }

    companion object {
        private const val DEFAULT_MAX_RETRIES = 3
        private val RETRY_DELAYS = listOf(1000L, 2000L, 4000L) // Exponential backoff: 1s, 2s, 4s
    }
}

@JsonClass(generateAdapter = true)
internal data class AttributeRequest(
    val url: String,
    val platform: String,
    @Json(name = "is_deferred")
    val isDeferred: Boolean = false
)

@JsonClass(generateAdapter = true)
internal data class AttributeResponse(
    val matched: Boolean,
    val confidence: DeepLinkResult.Confidence? = null,
    @Json(name = "match_score")
    val matchScore: Int? = null,
    val link: LinkData? = null
)
