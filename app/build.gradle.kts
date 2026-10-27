plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Assinatura de teste estável: injetada pelo GitHub Actions (segredos). Localmente cai na chave debug.
val testKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
val gitSha: String = System.getenv("GITHUB_SHA")?.take(7) ?: "local"

android {
    namespace = "br.app.motorista"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.app.motorista"
        // Android mínimo ainda não confirmado pelo usuário; valor provisório.
        minSdk = 26
        targetSdk = 36
        // versionCode cresce a cada execução do CI para permitir atualizar sobre a versão anterior.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "0.1.0"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
    }

    signingConfigs {
        if (testKeystorePath != null) {
            create("test") {
                storeFile = file(testKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("test") ?: signingConfigs.getByName("debug")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // Testes de layout do balão (Compose) rodam na JVM com Robolectric, sem emulador.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
