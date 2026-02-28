# Strip all debug logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep Retrofit and Gson annotations/structure
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Data Models from being obfuscated to prevent JSON parsing errors
-keep class com.cipher.security.data.entity.** { *; }
-keep class com.cipher.security.domain.model.** { *; }

-dontwarn okio.**
-dontwarn retrofit2.**
