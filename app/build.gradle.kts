plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.edwin.grabador"
 compileSdk = 35
 defaultConfig { applicationId = "com.edwin.grabador"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1"; ndk { abiFilters += listOf("arm64-v8a") } }
 signingConfigs {
  create("permanent") {
   storeFile = file(System.getenv("GRABADOR_KEYSTORE_PATH") ?: "signing-placeholder.jks")
   storePassword = System.getenv("GRABADOR_SIGNING_PASSWORD") ?: ""
   keyAlias = "grabador-universitario"
   keyPassword = System.getenv("GRABADOR_SIGNING_PASSWORD") ?: ""
  }
 }
 buildTypes {
  getByName("debug") {
   signingConfig = signingConfigs.getByName("permanent")
  }
 }
 externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}

dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
}
