package com.dynalinks.sdk

/**
 * Log level for the Dynalinks SDK.
 */
enum class DynalinksLogLevel {
    /** No logging */
    NONE,

    /** Only errors */
    ERROR,

    /** Warnings and errors */
    WARNING,

    /** Info, warnings, and errors */
    INFO,

    /** All logs including debug */
    DEBUG
}
