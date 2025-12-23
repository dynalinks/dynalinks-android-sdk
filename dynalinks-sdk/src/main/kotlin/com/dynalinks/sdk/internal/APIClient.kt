package com.dynalinks.sdk.internal

import com.dynalinks.sdk.DeepLinkResult
import com.dynalinks.sdk.DynalinksError
import com.dynalinks.sdk.LinkData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
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
    private val clientApiKey: String
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

    /**
     * Attribute a link URL to get link data.
     * POST /api/v1/links/attribute
     */
    suspend fun attributeLink(url: String, isDeferred: Boolean = false): DeepLinkResult =
        withContext(Dispatchers.IO) {
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
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Logger.error("Server error: ${response.code} - $errorBody")
                    throw DynalinksError.ServerError(response.code, errorBody)
                }

                val body = response.body?.string()
                    ?: throw DynalinksError.InvalidResponse

                Logger.debug("Response: $body")

                val attributeResponse = responseAdapter.fromJson(body)
                    ?: throw DynalinksError.InvalidResponse

                if (attributeResponse.matched && attributeResponse.link != null) {
                    DeepLinkResult(
                        matched = true,
                        confidence = attributeResponse.confidence,
                        matchScore = attributeResponse.matchScore,
                        link = attributeResponse.link,
                        isDeferred = isDeferred
                    )
                } else {
                    DeepLinkResult.notMatched(isDeferred)
                }
            } catch (e: DynalinksError) {
                throw e
            } catch (e: IOException) {
                Logger.error("Network error", e)
                throw DynalinksError.NetworkError(e)
            } catch (e: Exception) {
                Logger.error("Unexpected error", e)
                throw DynalinksError.NetworkError(e)
            }
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
