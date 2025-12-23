package com.dynalinks.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dynalinks.example.databinding.ActivityMainBinding
import com.dynalinks.sdk.DeepLinkResult
import com.dynalinks.sdk.Dynalinks
import com.dynalinks.sdk.DynalinksError
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for deferred deep link on first launch
        checkForDeferredDeepLink()

        // Handle App Link if app was opened via one
        handleAppLink(intent)

        // Set up button actions
        binding.checkDeferredButton.setOnClickListener {
            Dynalinks.reset() // Reset to allow checking again
            checkForDeferredDeepLink()
        }

        binding.resetButton.setOnClickListener {
            Dynalinks.reset()
            updateUI(null, "SDK state reset")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleAppLink(it) }
    }

    private fun checkForDeferredDeepLink() {
        updateUI(null, "Checking for deferred deep link...")

        lifecycleScope.launch {
            try {
                val result = Dynalinks.checkForDeferredDeepLink()
                handleResult(result, "Deferred Deep Link")
            } catch (e: DynalinksError) {
                updateUI(null, "Error: ${e.message}")
                Log.e(TAG, "Deferred deep link error", e)
            } catch (e: Exception) {
                updateUI(null, "Unexpected error: ${e.message}")
                Log.e(TAG, "Unexpected error", e)
            }
        }
    }

    private fun handleAppLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            updateUI(null, "Handling App Link: ${intent.data}")

            lifecycleScope.launch {
                try {
                    val result = Dynalinks.handleAppLink(intent)
                    handleResult(result, "App Link")
                } catch (e: DynalinksError) {
                    updateUI(null, "Error: ${e.message}")
                    Log.e(TAG, "App Link error", e)
                }
            }
        }
    }

    private fun handleResult(result: DeepLinkResult, source: String) {
        if (result.matched) {
            val link = result.link
            val details = buildString {
                appendLine("Source: $source")
                appendLine("Matched: ${result.matched}")
                appendLine("Deferred: ${result.isDeferred}")
                appendLine("Confidence: ${result.confidence}")
                appendLine("Score: ${result.matchScore}")
                appendLine()
                appendLine("Link Details:")
                appendLine("  ID: ${link?.id}")
                appendLine("  Path: ${link?.path}")
                appendLine("  Deep Link Value: ${link?.deepLinkValue}")
                appendLine("  Full URL: ${link?.fullUrl}")
                appendLine("  Name: ${link?.name}")
            }
            updateUI(link?.deepLinkValue, details)
            Log.i(TAG, "Deep link matched: ${link?.deepLinkValue}")
        } else {
            updateUI(null, "No matching deep link found\n\nSource: $source\nDeferred: ${result.isDeferred}")
            Log.i(TAG, "No deep link match")
        }
    }

    private fun updateUI(deepLinkValue: String?, details: String) {
        binding.deepLinkValue.text = deepLinkValue ?: "(none)"
        binding.resultDetails.text = details
    }

    companion object {
        private const val TAG = "DynalinksExample"
    }
}
