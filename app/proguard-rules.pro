# Add project specific ProGuard rules here.

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-keep class kotlin.Metadata { *; }

# Keep data layer classes (DataStore, repositories, models)
-keep class com.alkisstam.taskbar.data.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
