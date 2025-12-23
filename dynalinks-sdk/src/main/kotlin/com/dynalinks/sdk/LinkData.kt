package com.dynalinks.sdk

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data about a matched deep link.
 */
@JsonClass(generateAdapter = true)
data class LinkData(
    /** Unique identifier for the link (UUID) */
    val id: String,

    /** Link name */
    val name: String? = null,

    /** Path component of the link */
    val path: String? = null,

    /** Shortened path for the link */
    @Json(name = "shortened_path")
    val shortenedPath: String? = null,

    /** Original URL the link points to */
    val url: String? = null,

    /** Full Dynalinks URL */
    @Json(name = "full_url")
    val fullUrl: String? = null,

    /** Deep link value for routing in app */
    @Json(name = "deep_link_value")
    val deepLinkValue: String? = null,

    /** Android fallback URL */
    @Json(name = "android_fallback_url")
    val androidFallbackUrl: String? = null,

    /** iOS fallback URL */
    @Json(name = "ios_fallback_url")
    val iosFallbackUrl: String? = null,

    /** Whether forced redirect is enabled */
    @Json(name = "enable_forced_redirect")
    val enableForcedRedirect: Boolean? = null,

    /** Social sharing title */
    @Json(name = "social_title")
    val socialTitle: String? = null,

    /** Social sharing description */
    @Json(name = "social_description")
    val socialDescription: String? = null,

    /** Social sharing image URL */
    @Json(name = "social_image_url")
    val socialImageUrl: String? = null,

    /** Number of clicks */
    val clicks: Int? = null
)
