# Dynalinks Example App

A sample Android app demonstrating how to integrate the Dynalinks SDK for deferred deep linking and App Links.

## Setup

### 1. Configure API Credentials

Add your Dynalinks credentials to `local.properties` in the project root (this file is gitignored):

```properties
# Dynalinks SDK configuration
dynalinks.apiKey=your-client-api-key-here
dynalinks.baseUrl=https://dynalinks.app/api/v1
```

| Property | Description | Default |
|----------|-------------|---------|
| `dynalinks.apiKey` | Your project's client API key from the Dynalinks console | (required) |
| `dynalinks.baseUrl` | API base URL | `https://dynalinks.app/api/v1` |

### 2. Configure App Links Domain

Update `AndroidManifest.xml` to use your Dynalinks project domain:

```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="https"
        android:host="your-project.dynalinks.app" />
</intent-filter>
```

### 3. Build and Run

1. Open the project in Android Studio
2. Sync Gradle
3. Select "Example App" run configuration
4. Run on device or emulator

## Testing

### App Links

Test App Links by opening a Dynalinks URL:

```bash
adb shell am start -a android.intent.action.VIEW -d "https://your-project.dynalinks.app/your-path"
```

### Deferred Deep Links

1. Click a Dynalinks URL in a browser (don't have the app installed)
2. Install the app from Play Store (or sideload for testing)
3. Open the app - it will check for deferred deep links automatically

## Features Demonstrated

- **SDK Configuration** - `ExampleApplication.kt` shows how to initialize the SDK
- **App Link Handling** - `MainActivity.kt` handles incoming App Links in `onCreate()` and `onNewIntent()`
- **Deferred Deep Link Check** - Called on first app launch to retrieve pending deep links
- **Result Display** - Shows matched link data in the UI

## Troubleshooting

### "No API key configured" warning

Add `dynalinks.apiKey` to `local.properties` and rebuild.

### App Links not working

1. Verify your domain's `assetlinks.json` is accessible at `https://your-domain/.well-known/assetlinks.json`
2. Check that `android:autoVerify="true"` is set in the intent filter
3. Run `adb shell pm get-app-links com.dynalinks.example` to check verification status

### 401 Invalid API Key

Verify the API key in `local.properties` matches your project's client API key in the Dynalinks console.
