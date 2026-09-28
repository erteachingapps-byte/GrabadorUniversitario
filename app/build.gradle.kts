plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.edwin.grabador"
 compileSdk = 35
 defaultConfig { applicationId = "com.edwin.grabador"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1" }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}

dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
}
