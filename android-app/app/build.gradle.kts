import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Production API (Render, auto-deployed from GitHub main).
val LIVE_API_URL = "https://finai-smartai.onrender.com/"
val useLiveApi = (findProperty("finai.useLiveApi") as String?)?.toBoolean() ?: false

android {
    namespace = "com.example.fin_ai"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.fin_ai"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // By default reaches `npm run dev` on the host through the adb reverse tunnel:
            //   adb reverse tcp:3000 tcp:3000
            // Preferred over the 10.0.2.2 NAT alias because it also works on a
            // physical device over USB, and survives flaky emulator NAT.
            // Set finai.useLiveApi=true in gradle.properties to use the deployed site instead.
            buildConfigField("String", "API_BASE_URL", "\"${if (useLiveApi) LIVE_API_URL else "http://localhost:3000/"}\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$LIVE_API_URL\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ---------------------------------------------------------------------------
// Pressing Run in Android Studio does not recreate the `adb reverse` tunnel that
// the localhost API base URL depends on, and that tunnel dies on every emulator
// restart or `adb kill-server`. Recreate it as part of the debug build so the app
// reaches `npm run dev` with no manual step.
//
// Failure is deliberately non-fatal: no device attached yet is not a build error,
// and ApiClient falls back to the 10.0.2.2 NAT alias on its own regardless.
// ---------------------------------------------------------------------------
// Resolved at configuration time to a plain String: the configuration cache cannot
// serialise script-object references captured by an onlyIf/doLast closure.
val adbPath: String? = run {
    val props = Properties()
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { props.load(it) }
    val sdkDir = props.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
    val exe = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
    sdkDir?.let { File(it, "platform-tools/$exe") }?.takeIf { it.exists() }?.absolutePath
}

val adbReverseTunnel = tasks.register<Exec>("adbReverseTunnel") {
    description = "Forwards device port 3000 to the host dev server."
    enabled = adbPath != null
    isIgnoreExitValue = true
    commandLine(adbPath ?: "adb", "reverse", "tcp:3000", "tcp:3000")
}

afterEvaluate {
    listOf("assembleDebug", "installDebug").forEach { taskName ->
        tasks.findByName(taskName)?.finalizedBy(adbReverseTunnel)
    }
}
