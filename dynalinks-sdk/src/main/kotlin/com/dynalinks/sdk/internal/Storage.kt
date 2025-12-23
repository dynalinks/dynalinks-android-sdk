package com.dynalinks.sdk.internal

import android.content.Context
import android.content.SharedPreferences
import com.dynalinks.sdk.DeepLinkResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Persistent storage for the Dynalinks SDK using SharedPreferences.
 */
internal class Storage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val resultAdapter = moshi.adapter(DeepLinkResult::class.java)

    /**
     * Whether we have already checked for a deferred deep link.
     */
    var hasCheckedForDeferredDeepLink: Boolean
        get() = prefs.getBoolean(KEY_HAS_CHECKED, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_CHECKED, value).apply()

    /**
     * Cached result from a previous deferred deep link check.
     */
    var cachedResult: DeepLinkResult?
        get() {
            val json = prefs.getString(KEY_CACHED_RESULT, null) ?: return null
            return try {
                resultAdapter.fromJson(json)
            } catch (e: Exception) {
                Logger.error("Failed to parse cached result", e)
                null
            }
        }
        set(value) {
            if (value != null) {
                val json = resultAdapter.toJson(value)
                prefs.edit().putString(KEY_CACHED_RESULT, json).apply()
            } else {
                prefs.edit().remove(KEY_CACHED_RESULT).apply()
            }
        }

    /**
     * Reset all stored state.
     */
    fun reset() {
        prefs.edit().clear().apply()
        Logger.info("Storage reset")
    }

    companion object {
        private const val PREFS_NAME = "dynalinks_sdk"
        private const val KEY_HAS_CHECKED = "has_checked_for_deferred_deep_link"
        private const val KEY_CACHED_RESULT = "cached_result"
    }
}
