package com.dynalinks.example

import android.app.Application
import com.dynalinks.sdk.Dynalinks
import com.dynalinks.sdk.DynalinksLogLevel

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure Dynalinks SDK
        // API key and base URL are read from local.properties (gitignored)
        // Add to local.properties:
        //   dynalinks.apiKey=your-api-key-here
        //   dynalinks.baseUrl=https://dynalinks.app/api/v1
        val apiKey = BuildConfig.DYNALINKS_API_KEY
        val baseUrl = BuildConfig.DYNALINKS_BASE_URL

        if (apiKey.isBlank()) {
            android.util.Log.w("DynalinksExample", "No API key configured. Add dynalinks.apiKey to local.properties")
            return
        }

        Dynalinks.configure(
            context = this,
            clientAPIKey = apiKey,
            baseURL = baseUrl,
            logLevel = DynalinksLogLevel.DEBUG,
            allowEmulator = true
        )
    }
}
