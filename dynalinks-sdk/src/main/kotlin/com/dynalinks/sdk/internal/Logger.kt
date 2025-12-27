package com.dynalinks.sdk.internal

import android.util.Log
import com.dynalinks.sdk.DynalinksLogLevel

/**
 * Internal logger for the Dynalinks SDK.
 * Uses Android's built-in Log class to avoid forcing third-party
 * logging libraries on host apps.
 */
internal object Logger {
    private const val TAG = "Dynalinks"

    var logLevel: DynalinksLogLevel = DynalinksLogLevel.ERROR

    /**
     * Initialize the logger. No-op for Android Log.
     */
    fun initialize() {
        // No initialization needed for Android Log
    }

    fun debug(message: String) {
        if (logLevel >= DynalinksLogLevel.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun info(message: String) {
        if (logLevel >= DynalinksLogLevel.INFO) {
            Log.i(TAG, message)
        }
    }

    fun warning(message: String) {
        if (logLevel >= DynalinksLogLevel.WARNING) {
            Log.w(TAG, message)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (logLevel >= DynalinksLogLevel.ERROR) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }
}
