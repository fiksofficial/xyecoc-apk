-keep class com.xyecoc.mail.data.model.** { *; }
-keepclassmembers class com.xyecoc.mail.data.model.** { *; }
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keepattributes Signature
-keepattributes *Annotation*

# Remove all android.util.Log calls
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
    public static int wtf(...);
}

# Keep our data models for GSON
-keep class com.xyecoc.mail.data.model.** { *; }

# Keep Coil from being shrunk incorrectly
-keep class coil.** { *; }
