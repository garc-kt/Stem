# Add project specific ProGuard rules here.

-keep class com.stem.service.** { *; }
-keep class com.stem.ui.** { *; }
-keep class com.stem.app.** { *; }

# Kotlin Serialization & Coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
