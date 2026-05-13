# Add project specific ProGuard rules here.

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Keep Compose-related classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep DataStore generated classes
-keep class com.taskbar.app.data.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Hilt entry points
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
