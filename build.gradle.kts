plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied: AGP 9 compiles Kotlin itself. Kept on the classpath to pin KGP to the catalog
    // version, or AGP would use the older Kotlin it bundles.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.layout.buildDirectory)
    }
}
