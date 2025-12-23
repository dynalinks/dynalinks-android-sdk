package com.dynalinks.sdk

/**
 * Errors that can occur when using the Dynalinks SDK.
 */
sealed class DynalinksError : Exception() {

    /**
     * SDK has not been configured. Call [Dynalinks.configure] first.
     */
    object NotConfigured : DynalinksError() {
        private fun readResolve(): Any = NotConfigured
        override val message: String = "Dynalinks SDK not configured. Call Dynalinks.configure() first."
    }

    /**
     * Invalid API key format.
     */
    data class InvalidAPIKey(override val message: String) : DynalinksError()

    /**
     * Deferred deep linking is not available on emulator.
     */
    object Emulator : DynalinksError() {
        private fun readResolve(): Any = Emulator
        override val message: String = "Deferred deep linking not available on emulator."
    }

    /**
     * Intent does not contain valid deep link data.
     */
    object InvalidIntent : DynalinksError() {
        private fun readResolve(): Any = InvalidIntent
        override val message: String = "Intent does not contain valid deep link data."
    }

    /**
     * Network request failed.
     */
    data class NetworkError(val underlyingCause: Throwable?) : DynalinksError() {
        override val message: String = "Network request failed: ${underlyingCause?.message ?: "unknown error"}"
        override val cause: Throwable? get() = underlyingCause
    }

    /**
     * Server returned an invalid response.
     */
    object InvalidResponse : DynalinksError() {
        private fun readResolve(): Any = InvalidResponse
        override val message: String = "Invalid response from server."
    }

    /**
     * Server returned an error status code.
     */
    data class ServerError(val statusCode: Int, val serverMessage: String?) : DynalinksError() {
        override val message: String = serverMessage?.let { "Server error ($statusCode): $it" }
            ?: "Server error: $statusCode"
    }

    /**
     * No matching deferred deep link was found.
     */
    object NoMatch : DynalinksError() {
        private fun readResolve(): Any = NoMatch
        override val message: String = "No matching deferred deep link found."
    }

    /**
     * Install Referrer API is not available.
     */
    object InstallReferrerUnavailable : DynalinksError() {
        private fun readResolve(): Any = InstallReferrerUnavailable
        override val message: String = "Install Referrer API is not available."
    }

    /**
     * Install Referrer connection timed out.
     */
    object InstallReferrerTimeout : DynalinksError() {
        private fun readResolve(): Any = InstallReferrerTimeout
        override val message: String = "Install Referrer connection timed out."
    }
}
