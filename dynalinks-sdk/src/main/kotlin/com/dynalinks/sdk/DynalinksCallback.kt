package com.dynalinks.sdk

/**
 * Callback interface for async operations.
 *
 * Use this for Java interoperability or when coroutines are not available.
 */
interface DynalinksCallback<T> {
    /**
     * Called when the operation completes successfully.
     */
    fun onSuccess(result: T)

    /**
     * Called when the operation fails.
     */
    fun onError(error: DynalinksError)
}
