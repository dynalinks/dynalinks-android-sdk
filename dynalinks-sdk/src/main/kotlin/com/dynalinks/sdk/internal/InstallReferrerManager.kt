package com.dynalinks.sdk.internal

import android.content.Context
import android.util.Base64
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLDecoder
import kotlin.coroutines.resume

/**
 * Manager for Google Play Install Referrer API.
 *
 * Handles connection to the Install Referrer service and parsing of referrer data.
 */
internal class InstallReferrerManager(private val context: Context) {

    /**
     * Result from the Install Referrer API.
     */
    data class ReferrerResult(
        /** The Dynalinks URL extracted from the referrer, if present */
        val url: String?,
        /** Raw referrer details from Google Play */
        val details: ReferrerDetails
    )

    /**
     * Get install referrer data with a timeout.
     *
     * @param timeoutMs Maximum time to wait for the referrer (default 5 seconds)
     * @return ReferrerResult if successful and contains a Dynalinks URL, null otherwise
     */
    suspend fun getReferrer(timeoutMs: Long = DEFAULT_TIMEOUT_MS): ReferrerResult? {
        Logger.debug("Getting install referrer (timeout: ${timeoutMs}ms)")

        return withTimeoutOrNull(timeoutMs) {
            getReferrerInternal()
        }
    }

    private suspend fun getReferrerInternal(): ReferrerResult? = suspendCancellableCoroutine { cont ->
        val client = InstallReferrerClient.newBuilder(context).build()

        cont.invokeOnCancellation {
            try {
                client.endConnection()
            } catch (e: Exception) {
                Logger.debug("Error ending connection on cancellation: ${e.message}")
            }
        }

        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val details = client.installReferrer
                            val referrer = details.installReferrer
                            Logger.debug("Install referrer: $referrer")

                            val url = parseReferrer(referrer)
                            if (url != null) {
                                Logger.info("Found Dynalinks URL in referrer: $url")
                                cont.resume(ReferrerResult(url, details))
                            } else {
                                Logger.debug("No Dynalinks URL in referrer")
                                cont.resume(null)
                            }
                        } catch (e: Exception) {
                            Logger.error("Error getting referrer details", e)
                            cont.resume(null)
                        } finally {
                            try {
                                client.endConnection()
                            } catch (e: Exception) {
                                Logger.debug("Error ending connection: ${e.message}")
                            }
                        }
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Logger.warning("Install Referrer API not supported")
                        cont.resume(null)
                        client.endConnection()
                    }

                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Logger.warning("Install Referrer service unavailable")
                        cont.resume(null)
                        client.endConnection()
                    }

                    else -> {
                        Logger.warning("Unknown Install Referrer response code: $responseCode")
                        cont.resume(null)
                        client.endConnection()
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Logger.debug("Install Referrer service disconnected")
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        })
    }

    /**
     * Parse the referrer string to extract the URL.
     *
     * Tries _url parameter first (base64-encoded, new format), then falls back to
     * url parameter (URL-encoded, legacy format) for backward compatibility.
     */
    private fun parseReferrer(referrer: String?): String? {
        if (referrer.isNullOrBlank()) return null

        return try {
            val params = referrer.split("&")
                .asSequence()
                .map { it.split("=", limit = 2) }
                .filter { it.size == 2 }
                .toList()

            // Try _url parameter first (base64-encoded, new format)
            params.firstOrNull { (key, _) -> key == "_url" }
                ?.let { (_, value) ->
                    try {
                        val decoded = String(Base64.decode(value, Base64.URL_SAFE or Base64.NO_PADDING))
                        if (decoded.startsWith("https://") || decoded.startsWith("http://")) {
                            Logger.debug("Found URL in _url parameter (base64): $decoded")
                            return decoded
                        }
                    } catch (e: Exception) {
                        Logger.debug("Failed to decode _url parameter: ${e.message}")
                    }
                }

            // Fall back to url parameter (URL-encoded, legacy format)
            params.firstOrNull { (key, _) -> key == "url" }
                ?.let { (_, value) ->
                    val decoded = URLDecoder.decode(value, "UTF-8")
                    if (decoded.startsWith("https://") || decoded.startsWith("http://")) {
                        Logger.debug("Found URL in url parameter: $decoded")
                        return decoded
                    }
                }

            null
        } catch (e: Exception) {
            Logger.error("Error parsing referrer: ${e.message}")
            null
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
