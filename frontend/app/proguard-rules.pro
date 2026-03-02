# Strip all debug logging in release
# Temporarily commenting out log stripping to diagnose crashes 
# -assumenosideeffects class android.util.Log {
#     public static boolean isLoggable(java.lang.String, int);
#     public static int v(...);
#     public static int i(...);
#     public static int w(...);
#     public static int d(...);
#     public static int e(...);
# }

# Keep Retrofit and Gson annotations/structure
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Data Models from being obfuscated to prevent JSON parsing errors
-keep class com.cipher.security.data.entity.** { *; }
-keep class com.cipher.security.domain.model.** { *; }
-keep class com.cipher.security.api.model.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

-dontwarn okio.**
-dontwarn retrofit2.**
