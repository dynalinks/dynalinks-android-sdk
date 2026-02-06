# Dynalinks Android SDK

The official Android SDK for [Dynalinks](https://dynalinks.app) - smart deep linking for mobile apps.

## Features

- **Deferred Deep Linking** - Route users to specific content even when installing the app for the first time
- **App Links** - Handle direct app opens via Android App Links
- **Simple Integration** - Single method call to check for deep links
- **Kotlin Coroutines** - Modern async API with coroutine support
- **Java Compatibility** - Callback-based API for Java projects

## Requirements

- Android 5.0+ (API level 21)
- Kotlin 1.9+ or Java 8+

## Installation

### Option 1: JitPack (Recommended)

Add JitPack repository to your root `build.gradle.kts` or `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.dynalinks:dynalinks-android-sdk:1.0.3")
}
```

### Option 2: Local Module

Clone the SDK and add it as a local module:

```kotlin
// settings.gradle.kts
include(":dynalinks-sdk")
project(":dynalinks-sdk").projectDir = file("path/to/DynalinksSDK-Android/dynalinks-sdk")

// app/build.gradle.kts
dependencies {
    implementation(project(":dynalinks-sdk"))
}
```

## Quick Start

### 1. Configure the SDK

Initialize the SDK in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Dynalinks.configure(
            context = this,
            clientAPIKey = "your-client-api-key" // From Dynalinks console
        )
    }
}
```

### 2. Check for Deferred Deep Links

On app launch (typically in your main Activity), check for deferred deep links:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val result = Dynalinks.checkForDeferredDeepLink()
                if (result.matched) {
                    val deepLinkValue = result.link?.deepLinkValue
                    // Navigate to the appropriate screen
                    navigateToDeepLink(deepLinkValue)
                }
            } catch (e: DynalinksError) {
                // Handle error (or ignore for production)
                Log.d("Dynalinks", "No deferred deep link: ${e.message}")
            }
        }
    }
}
```

### 3. Handle App Links

When your app is opened via an App Link:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAppLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleAppLink(it) }
    }

    private fun handleAppLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            lifecycleScope.launch {
                try {
                    val result = Dynalinks.handleAppLink(intent)
                    if (result.matched) {
                        navigateToDeepLink(result.link?.deepLinkValue)
                    }
                } catch (e: DynalinksError) {
                    Log.e("Dynalinks", "Failed to handle App Link", e)
                }
            }
        }
    }
}
```

## Configuration Options

```kotlin
Dynalinks.configure(
    context = applicationContext,
    clientAPIKey = "your-client-api-key",

    // Optional: Custom API URL (for testing)
    baseURL = "https://dynalinks.app/api/v1",

    // Optional: Log level (default: ERROR)
    logLevel = DynalinksLogLevel.DEBUG,

    // Optional: Allow on emulator (default: false)
    allowEmulator = true
)
```

### Log Levels

| Level | Description |
|-------|-------------|
| `NONE` | No logging |
| `ERROR` | Only errors (default) |
| `WARNING` | Warnings and errors |
| `INFO` | Info, warnings, and errors |
| `DEBUG` | All logs including debug |

## API Reference

### Dynalinks

The main entry point for the SDK.

#### `configure()`

Configure the SDK. Call once in `Application.onCreate()`.

```kotlin
Dynalinks.configure(
    context: Context,
    clientAPIKey: String,
    baseURL: String = "https://dynalinks.app/api/v1",
    logLevel: DynalinksLogLevel = DynalinksLogLevel.ERROR,
    allowEmulator: Boolean = false
)
```

#### `checkForDeferredDeepLink()`

Check for a deferred deep link. Call once on first app launch.

```kotlin
// Kotlin (coroutine)
suspend fun checkForDeferredDeepLink(): DeepLinkResult

// Java (callback)
fun checkForDeferredDeepLink(callback: DynalinksCallback<DeepLinkResult>)
```

#### `handleAppLink()`

Handle an App Link that opened the app.

```kotlin
// From Intent
suspend fun handleAppLink(intent: Intent): DeepLinkResult

// From URI
suspend fun handleAppLink(uri: Uri): DeepLinkResult

// Java (callback)
fun handleAppLink(intent: Intent, callback: DynalinksCallback<DeepLinkResult>)
fun handleAppLink(uri: Uri, callback: DynalinksCallback<DeepLinkResult>)
```

### DeepLinkResult

Result of a deep link check.

| Property | Type | Description |
|----------|------|-------------|
| `matched` | `Boolean` | Whether a matching link was found |
| `confidence` | `Confidence?` | Match confidence (HIGH, MEDIUM, LOW) |
| `matchScore` | `Int?` | Match score (0-100) |
| `link` | `LinkData?` | Link data if matched |
| `isDeferred` | `Boolean` | Whether this is from a deferred deep link |

### LinkData

Data about the matched link.

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique link identifier (UUID) |
| `name` | `String?` | Link name |
| `path` | `String?` | Link path |
| `deepLinkValue` | `String?` | Value for in-app routing |
| `fullUrl` | `String?` | Complete Dynalinks URL |
| `iosDeferredDeepLinkingEnabled` | `Boolean?` | Whether iOS deferred deep linking is enabled |
| `referrer` | `String?` | Referrer tracking parameter for attribution |
| `providerToken` | `String?` | Apple Search Ads attribution token (pt) |
| `campaignToken` | `String?` | Campaign identifier for attribution (ct) |
| `socialTitle` | `String?` | Social sharing title |
| `socialDescription` | `String?` | Social sharing description |
| `socialImageUrl` | `String?` | Social sharing image URL |

### DynalinksError

Errors that can occur when using the SDK.

| Error | Description |
|-------|-------------|
| `NotConfigured` | SDK not configured, call `configure()` first |
| `InvalidAPIKey` | Invalid API key format |
| `Emulator` | Running on emulator with `allowEmulator = false` |
| `InvalidIntent` | Intent doesn't contain App Link data |
| `NetworkError` | Network request failed |
| `InvalidResponse` | Server returned invalid response |
| `ServerError` | Server returned error status code |
| `NoMatch` | No matching deep link found |

## Java Usage

For Java projects, use the callback-based API:

```java
// Configure
Dynalinks.configure(
    getApplicationContext(),
    "your-client-api-key",
    "https://dynalinks.app/api/v1",
    DynalinksLogLevel.ERROR,
    false
);

// Check for deferred deep link
Dynalinks.checkForDeferredDeepLink(new DynalinksCallback<DeepLinkResult>() {
    @Override
    public void onSuccess(DeepLinkResult result) {
        if (result.getMatched()) {
            String deepLinkValue = result.getLink().getDeepLinkValue();
            // Navigate to content
        }
    }

    @Override
    public void onError(DynalinksError error) {
        Log.e("Dynalinks", "Error: " + error.getMessage());
    }
});
```

## App Links Setup

To enable App Links, add the following to your `AndroidManifest.xml`:

```xml
<activity android:name=".MainActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="your-project.dynalinks.app" />
    </intent-filter>
</activity>
```

The `assetlinks.json` file is automatically served by Dynalinks for your project domain.

## Testing

### Debug Logging

Enable debug logging during development:

```kotlin
Dynalinks.configure(
    context = this,
    clientAPIKey = "your-api-key",
    logLevel = DynalinksLogLevel.DEBUG
)
```

### Emulator Testing

By default, deferred deep linking is disabled on emulators. To test:

```kotlin
Dynalinks.configure(
    context = this,
    clientAPIKey = "your-api-key",
    allowEmulator = true
)
```

### Reset State

To test deferred deep linking multiple times:

```kotlin
Dynalinks.reset() // Clears cached state
```

## Attribution Tracking

The SDK provides attribution data for campaign tracking and analytics:

```kotlin
lifecycleScope.launch {
    val result = Dynalinks.checkForDeferredDeepLink()
    if (result.matched) {
        result.link?.let { link ->
            // Track attribution data for analytics
            link.referrer?.let { referrer ->
                Log.d("Dynalinks", "Referrer: $referrer") // e.g., "utm_source=facebook&utm_campaign=summer"
            }

            link.providerToken?.let { token ->
                Log.d("Dynalinks", "Apple Search Ads token: $token") // pt parameter
            }

            link.campaignToken?.let { campaign ->
                Log.d("Dynalinks", "Campaign: $campaign") // ct parameter
            }

            // Send to your analytics platform
            analytics.track("deep_link_opened", mapOf(
                "referrer" to link.referrer,
                "provider_token" to link.providerToken,
                "campaign" to link.campaignToken,
                "deep_link" to link.deepLinkValue
            ))
        }
    }
}
```

## License

MIT License - see [LICENSE](LICENSE) for details.
