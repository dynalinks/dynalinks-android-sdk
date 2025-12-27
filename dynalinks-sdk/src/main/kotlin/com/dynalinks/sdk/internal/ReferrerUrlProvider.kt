package com.dynalinks.sdk.internal

/**
 * Interface for getting install referrer URL.
 * Allows testing without Google Play Install Referrer dependencies.
 */
internal fun interface ReferrerUrlProvider {
    suspend fun getReferrerUrl(): String?
}

/**
 * Extension function to convert InstallReferrerManager to ReferrerUrlProvider.
 */
internal fun InstallReferrerManager.asReferrerUrlProvider(): ReferrerUrlProvider {
    return ReferrerUrlProvider { this.getReferrer()?.url }
}
