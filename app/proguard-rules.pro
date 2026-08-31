# Add project specific ProGuard rules here.

-keep class com.veggiebit.sprout.features.overlay.service.** { *; }
-keep class com.veggiebit.sprout.features.selection.ui.** { *; }
-keep class com.veggiebit.sprout.app.** { *; }

# Kotlin Serialization & Coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
