# Consumer ProGuard rules
# These rules are automatically applied to apps that use this library

# Keep Moshi's generated adapters for response parsing
-keep class com.dynalinks.sdk.internal.ApiResponse { *; }
-keep class com.dynalinks.sdk.internal.ApiResponse$* { *; }
