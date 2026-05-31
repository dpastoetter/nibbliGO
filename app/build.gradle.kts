import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nibbli.nibbligo"
    compileSdk = 35

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    val hfClientId = localProperties.getProperty("hf.oauth.clientId", "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val hfRedirectUri = localProperties.getProperty(
        "hf.oauth.redirectUri",
        "nibbli://oauth/huggingface",
    ).replace("\\", "\\\\").replace("\"", "\\\"")

    defaultConfig {
        applicationId = "com.nibbli.nibbligo"
        minSdk = 31
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.4"
        testInstrumentationRunner = "com.nibbli.nibbligo.HiltTestRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "nibbli"
        buildConfigField("String", "HF_OAUTH_CLIENT_ID", "\"$hfClientId\"")
        buildConfigField("String", "HF_OAUTH_REDIRECT_URI", "\"$hfRedirectUri\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:storage"))
    implementation(project(":core:runtime"))
    implementation(project(":core:agent"))
    implementation(project(":core:runtime-litert"))
    implementation(project(":core:litert-engine"))
    implementation(project(":core:hf-download"))
    implementation(project(":core:mcp"))
    implementation(project(":core:pet-llm"))
    implementation(libs.openid.appauth)
    implementation(project(":feature:pet"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:image"))
    implementation(project(":feature:audio"))
    implementation(project(":feature:actions"))
    implementation(project(":feature:models"))
    implementation(project(":feature:benchmark"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
