package com.dynalinks.sdk.internal

import com.dynalinks.sdk.DeepLinkResult

/**
 * Interface for SDK storage operations.
 * Allows testing without SharedPreferences dependencies.
 */
internal interface DynalinksStorage {
    var hasCheckedForDeferredDeepLink: Boolean
    var cachedResult: DeepLinkResult?
    fun reset()
}
