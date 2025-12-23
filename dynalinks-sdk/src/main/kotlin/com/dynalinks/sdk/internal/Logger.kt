package com.dynalinks.sdk.internal

import com.dynalinks.sdk.DynalinksLogLevel
import timber.log.Timber

/**
 * Internal logger for the Dynalinks SDK.
 * Uses Timber for logging with configurable log levels.
 */
internal object Logger {
    private const val TAG = "Dynalinks"

    var logLevel: DynalinksLogLevel = DynalinksLogLevel.ERROR

    /**
     * Initialize Timber with a DebugTree if not already planted.
     * Should be called during SDK configuration.
     */
    fun initialize() {
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
    }

    fun debug(message: String) {
        if (logLevel >= DynalinksLogLevel.DEBUG) {
            Timber.tag(TAG).d(message)
        }
    }

    fun info(message: String) {
        if (logLevel >= DynalinksLogLevel.INFO) {
            Timber.tag(TAG).i(message)
        }
    }

    fun warning(message: String) {
        if (logLevel >= DynalinksLogLevel.WARNING) {
            Timber.tag(TAG).w(message)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (logLevel >= DynalinksLogLevel.ERROR) {
            if (throwable != null) {
                Timber.tag(TAG).e(throwable, message)
            } else {
                Timber.tag(TAG).e(message)
            }
        }
    }
}
