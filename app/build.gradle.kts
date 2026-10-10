plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.keralapsc.prep"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.keralapsc.prep"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("PSC_RELEASE_KEYSTORE_PATH")
            val keystorePassword = System.getenv("PSC_RELEASE_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("PSC_RELEASE_KEY_ALIAS")
            val keyPassword = System.getenv("PSC_RELEASE_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register("validateReleaseSigning") {
    doLast {
        val keystorePath = System.getenv("PSC_RELEASE_KEYSTORE_PATH")
        val keystorePassword = System.getenv("PSC_RELEASE_KEYSTORE_PASSWORD")
        val keyAlias = System.getenv("PSC_RELEASE_KEY_ALIAS")
        val keyPassword = System.getenv("PSC_RELEASE_KEY_PASSWORD")

        val missing = mutableListOf<String>()
        if (keystorePath.isNullOrBlank()) missing.add("PSC_RELEASE_KEYSTORE_PATH")
        if (keystorePassword.isNullOrBlank()) missing.add("PSC_RELEASE_KEYSTORE_PASSWORD")
        if (keyAlias.isNullOrBlank()) missing.add("PSC_RELEASE_KEY_ALIAS")
        if (keyPassword.isNullOrBlank()) missing.add("PSC_RELEASE_KEY_PASSWORD")

        if (missing.isNotEmpty()) {
            throw GradleException(
                "RELEASE SIGNING ERROR: Missing required release signing credentials: ${missing.joinToString(", ")}. " +
                "Production release builds cannot be generated without valid signing credentials."
            )
        }

        val keystoreFile = file(keystorePath)
        if (!keystoreFile.exists()) {
            throw GradleException(
                "RELEASE SIGNING ERROR: Specified keystore file does not exist at path: $keystorePath"
            )
        }
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn("validateReleaseSigning")
}

