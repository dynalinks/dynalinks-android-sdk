# Add project specific ProGuard rules here.

# Keep public API classes
-keep public class com.dynalinks.sdk.Dynalinks { *; }
-keep public class com.dynalinks.sdk.DeepLinkResult { *; }
-keep public class com.dynalinks.sdk.DeepLinkResult$* { *; }
-keep public class com.dynalinks.sdk.LinkData { *; }
-keep public class com.dynalinks.sdk.DynalinksError { *; }
-keep public class com.dynalinks.sdk.DynalinksError$* { *; }
-keep public class com.dynalinks.sdk.DynalinksLogLevel { *; }
-keep public interface com.dynalinks.sdk.DynalinksCallback { *; }

# Keep Moshi adapters
-keep class com.dynalinks.sdk.internal.**JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
