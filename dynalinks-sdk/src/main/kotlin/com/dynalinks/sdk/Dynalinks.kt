package com.dynalinks.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.dynalinks.sdk.internal.APIClient
import com.dynalinks.sdk.internal.DynalinksStorage
import com.dynalinks.sdk.internal.InstallReferrerManager
import com.dynalinks.sdk.internal.Logger
import com.dynalinks.sdk.internal.ReferrerUrlProvider
import com.dynalinks.sdk.internal.Storage
import com.dynalinks.sdk.internal.asReferrerUrlProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Main entry point for the Dynalinks SDK.
 *
 * Use this class to configure the SDK and check for deferred deep links.
 *
 * ## Setup
 *
 * Configure the SDK as early as possible in your app's lifecycle, typically in
 * your Application class:
 *
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Dynalinks.configure(
 *             context = this,
 *             clientAPIKey = "your-client-api-key"
 *         )
 *     }
 * }
 * ```
 *
 * ## Check for Deferred Deep Links
 *
 * Call this on first app launch to check if the user came from a Dynalinks link:
 *
 * ```kotlin
 * lifecycleScope.launch {
 *     try {
 *         val result = Dynalinks.checkForDeferredDeepLink()
 *         if (result.matched) {
 *             val deepLinkValue = result.link?.deepLinkValue
 *             // Navigate based on deepLinkValue
 *         }
 *     } catch (e: DynalinksError) {
 *         // Handle error
 *     }
 * }
 * ```
 *
 * ## Handle App Links
 *
 * When your app is opened via an App Link:
 *
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     lifecycleScope.launch {
 *         val result = Dynalinks.handleAppLink(intent)
 *         if (result.matched) {
 *             // Navigate based on result.link?.deepLinkValue
 *         }
 *     }
 * }
 * ```
 */
object Dynalinks {

    /** The current version of the Dynalinks SDK */
    const val VERSION = "1.0.1"

    private var instance: DynalinksInstance? = null
    private val lock = Any()

    /**
     * Configure the Dynalinks SDK.
     *
     * Call this method as early as possible in your app's lifecycle, typically
     * in your Application's `onCreate()` method.
     *
     * @param context Application context
     * @param clientAPIKey Your project's client API key from the Dynalinks console
     * @param baseURL API base URL (defaults to production)
     * @param logLevel Logging verbosity (defaults to ERROR)
     * @param allowEmulator Allow deferred deep link checks on emulator (defaults to false)
     * @throws IllegalArgumentException if the API key is blank
     */
    @JvmStatic
    @JvmOverloads
    fun configure(
        context: Context,
        clientAPIKey: String,
        baseURL: String = "https://dynalinks.app/api/v1",
        logLevel: DynalinksLogLevel = DynalinksLogLevel.ERROR,
        allowEmulator: Boolean = false
    ) {
        require(clientAPIKey.isNotBlank()) { "Client API key cannot be empty" }

        synchronized(lock) {
            if (instance != null) {
                Logger.debug("SDK already configured, skipping")
                return
            }

            Logger.logLevel = logLevel
            Logger.initialize()
            instance = DynalinksInstance(
                context = context.applicationContext,
                clientAPIKey = clientAPIKey,
                baseURL = baseURL,
                allowEmulator = allowEmulator
            )
            Logger.info("Dynalinks SDK v$VERSION configured")
        }
    }

    /**
     * Check for a deferred deep link using coroutines.
     *
     * This method should be called once on first app launch. It will:
     * 1. Check the Google Play Install Referrer for a Dynalinks URL
     * 2. If found, fetch the link data from the server
     * 3. Return the result
     *
     * The SDK automatically prevents duplicate checks - subsequent calls
     * will return the cached result.
     *
     * @return A [DeepLinkResult] containing the matched link data
     * @throws DynalinksError if not configured, running on emulator, or network fails
     */
    @JvmStatic
    suspend fun checkForDeferredDeepLink(): DeepLinkResult {
        return getInstance().checkForDeferredDeepLink()
    }

    /**
     * Check for a deferred deep link with a callback.
     *
     * Convenience method for Java interoperability or non-coroutine contexts.
     *
     * @param callback Callback with the result or error
     */
    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun checkForDeferredDeepLink(callback: DynalinksCallback<DeepLinkResult>) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = checkForDeferredDeepLink()
                callback.onSuccess(result)
            } catch (e: DynalinksError) {
                callback.onError(e)
            } catch (e: Exception) {
                callback.onError(DynalinksError.NetworkError(e))
            }
        }
    }

    /**
     * Handle an App Link from an Intent using coroutines.
     *
     * Call this when your Activity receives an Intent with App Link data.
     *
     * @param intent The Intent that started the Activity
     * @return A [DeepLinkResult] with the resolved link data
     * @throws DynalinksError.InvalidIntent if the intent doesn't contain link data
     * @throws DynalinksError.NotConfigured if the SDK is not configured
     */
    @JvmStatic
    suspend fun handleAppLink(intent: Intent): DeepLinkResult {
        val uri = intent.data ?: throw DynalinksError.InvalidIntent
        return handleAppLink(uri)
    }

    /**
     * Handle an App Link from a URI using coroutines.
     *
     * @param uri The App Link URI
     * @return A [DeepLinkResult] with the resolved link data
     * @throws DynalinksError.NotConfigured if the SDK is not configured
     */
    @JvmStatic
    suspend fun handleAppLink(uri: Uri): DeepLinkResult {
        return getInstance().handleAppLink(uri)
    }

    /**
     * Handle an App Link from an Intent with a callback.
     *
     * @param intent The Intent that started the Activity
     * @param callback Callback with the result or error
     */
    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun handleAppLink(intent: Intent, callback: DynalinksCallback<DeepLinkResult>) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = handleAppLink(intent)
                callback.onSuccess(result)
            } catch (e: DynalinksError) {
                callback.onError(e)
            } catch (e: Exception) {
                callback.onError(DynalinksError.NetworkError(e))
            }
        }
    }

    /**
     * Handle an App Link from a URI with a callback.
     *
     * @param uri The App Link URI
     * @param callback Callback with the result or error
     */
    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun handleAppLink(uri: Uri, callback: DynalinksCallback<DeepLinkResult>) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = handleAppLink(uri)
                callback.onSuccess(result)
            } catch (e: DynalinksError) {
                callback.onError(e)
            } catch (e: Exception) {
                callback.onError(DynalinksError.NetworkError(e))
            }
        }
    }

    /**
     * Reset the SDK state.
     *
     * This clears the cached result and allows `checkForDeferredDeepLink`
     * to be called again. Useful for testing.
     *
     * **Warning:** Do not use in production. This is intended for testing only.
     */
    @JvmStatic
    fun reset() {
        instance?.reset()
        Logger.info("SDK state reset")
    }

    private fun getInstance(): DynalinksInstance {
        return instance ?: throw DynalinksError.NotConfigured
    }

    /**
     * Internal method to clear the instance for testing.
     */
    internal fun clearInstance() {
        synchronized(lock) {
            instance = null
        }
    }
}

/**
 * Internal implementation of the Dynalinks SDK.
 */
internal class DynalinksInstance(
    private val apiClient: APIClient,
    private val storage: DynalinksStorage,
    private val referrerUrlProvider: ReferrerUrlProvider,
    private val allowEmulator: Boolean,
    private val emulatorChecker: () -> Boolean = ::defaultEmulatorCheck
) {
    /**
     * Production constructor.
     */
    constructor(
        context: Context,
        clientAPIKey: String,
        baseURL: String,
        allowEmulator: Boolean
    ) : this(
        apiClient = APIClient(baseURL, clientAPIKey),
        storage = Storage(context),
        referrerUrlProvider = InstallReferrerManager(context).asReferrerUrlProvider(),
        allowEmulator = allowEmulator
    )

    suspend fun checkForDeferredDeepLink(): DeepLinkResult {
        // Return cached result if already checked
        if (storage.hasCheckedForDeferredDeepLink) {
            Logger.debug("Already checked for deferred deep link")
            storage.cachedResult?.let {
                Logger.info("Returning cached result")
                return it
            }
            Logger.info("Previously checked, no match found")
            return DeepLinkResult.notMatched(isDeferred = true)
        }

        // Check if running on emulator
        if (!allowEmulator && emulatorChecker()) {
            Logger.info("Skipping deferred deep link check on emulator")
            storage.hasCheckedForDeferredDeepLink = true
            throw DynalinksError.Emulator
        }

        // Get install referrer URL
        val referrerUrl = referrerUrlProvider.getReferrerUrl()
        if (referrerUrl == null) {
            Logger.info("No Dynalinks referrer found")
            storage.hasCheckedForDeferredDeepLink = true
            return DeepLinkResult.notMatched(isDeferred = true)
        }

        // Attribute the URL to get link data
        val result = try {
            apiClient.attributeLink(referrerUrl, isDeferred = true)
        } catch (e: Exception) {
            Logger.error("Failed to attribute link", e)
            storage.hasCheckedForDeferredDeepLink = true
            throw e
        }

        // Mark as checked and cache result
        storage.hasCheckedForDeferredDeepLink = true
        if (result.matched) {
            storage.cachedResult = result
            Logger.info("Deferred deep link found: ${result.link?.deepLinkValue}")
        } else {
            Logger.info("No match found for referrer URL")
        }

        return result
    }

    suspend fun handleAppLink(uri: Uri): DeepLinkResult {
        Logger.debug("Handling App Link: $uri")

        // Skip deferred deep link check since we have a direct link
        storage.hasCheckedForDeferredDeepLink = true

        // Attribute the URL
        val result = apiClient.attributeLink(uri.toString(), isDeferred = false)

        if (result.matched) {
            storage.cachedResult = result
            Logger.info("App Link resolved: ${result.link?.path}")
        } else {
            Logger.info("App Link not matched")
        }

        return result
    }

    fun reset() {
        storage.reset()
    }
}

/**
 * Default emulator detection function.
 */
private fun defaultEmulatorCheck(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT)
}
