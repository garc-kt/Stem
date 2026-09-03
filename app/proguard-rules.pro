# Add project specific ProGuard rules here.

# Manifest-declared components: Android instantiates these via reflection using the class name
# string from AndroidManifest.xml, so each needs its class (and no-arg constructor) kept intact.
# This replaces three blanket `-keep class com.stem.{service,ui,app}.** { *; }` rules that also
# preserved every Composable, helper, and DTO in those packages — working directly against
# isMinifyEnabled/isShrinkResources.
-keep class com.stem.app.StemApplication
-keep class com.stem.ui.navigation.MainActivity
-keep class com.stem.ui.navigation.ProcessTextActivity
-keep class com.stem.service.StemAccessibilityService
-keep class com.stem.service.StemTileService

# ViewModels instantiated via reflection by the default Compose viewModel() factory
# (androidx.lifecycle.ViewModelProvider.NewInstanceFactory), which needs the no-arg constructor.
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Kotlin Serialization & Coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# -----------------------------------------------------------------------------
# R8 Aggressive Optimizations & Minification
# -----------------------------------------------------------------------------
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Strip Android logging in release builds to eliminate dead strings and method calls
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Strip Kotlin runtime null check assertion intrinsics for smaller dex size
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
}

