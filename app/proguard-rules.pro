# Proguard & R8 Rules for TestPilot Production Release

# Preserve Jetpack Compose & runtime annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }

# Keep domain models for serialization and state preservation
-keep class com.nova.a12tester.domain.** { *; }
-keepclassmembers class com.nova.a12tester.domain.** { *; }

# Preserve Native C++ methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Firebase & Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# AdMob
-keep class com.google.ads.** { *; }
-keep public class com.google.android.gms.ads.** {
   public *;
}
