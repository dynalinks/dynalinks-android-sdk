package com.dynalinks.sdk

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Result of a deferred deep link check or app link resolution.
 */
@JsonClass(generateAdapter = true)
data class DeepLinkResult(
    /** Whether a matching link was found */
    val matched: Boolean,

    /** Confidence level of the match */
    val confidence: Confidence? = null,

    /** Match score (0-100) */
    @Json(name = "match_score")
    val matchScore: Int? = null,

    /** Link data if matched */
    val link: LinkData? = null,

    /** Whether this result is from a deferred deep link (Install Referrer) */
    @Json(ignore = true)
    val isDeferred: Boolean = false
) {
    /**
     * Confidence level of a match.
     */
    enum class Confidence {
        @Json(name = "high") HIGH,
        @Json(name = "medium") MEDIUM,
        @Json(name = "low") LOW
    }

    companion object {
        /** Create a result indicating no match was found */
        fun notMatched(isDeferred: Boolean = false) = DeepLinkResult(
            matched = false,
            isDeferred = isDeferred
        )
    }
}
