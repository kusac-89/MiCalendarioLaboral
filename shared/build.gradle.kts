plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.multiplatform.settings)
            // ESTA ES LA PIEZA QUE FALTABA MONTAR:
            implementation(libs.kotlinx.datetime)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
        iosMain.dependencies {}
    }
}

android {
    namespace = "com.example.micalendariolaboral.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
}
