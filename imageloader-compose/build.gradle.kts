plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.imageloader.compose"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    // Compose compiler plugin + buildFeatures enabled when AsyncImage lands (L6).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":imageloader-core"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
