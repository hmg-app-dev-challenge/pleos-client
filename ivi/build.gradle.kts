import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.chapark.ivi"
    compileSdk = 35
    testBuildType = providers.gradleProperty("iviTestBuildType").getOrElse("debug")
    useLibrary("android.car", false)
    defaultConfig {
        applicationId = "com.chapark.ivi"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "0.5.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseProperties=rootProject.file(".local-signing/release.properties")
    if(releaseProperties.exists()) {
        val keys=Properties().apply { releaseProperties.inputStream().use { load(it) } }
        signingConfigs.create("localRelease") {
            storeFile=rootProject.file(".local-signing/chapark-release.jks")
            storePassword=keys.getProperty("storePassword")
            keyAlias="chapark"
            keyPassword=keys.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("debug") {
            val tokenFile=rootProject.file(".local-server/token.txt")
            val token=if(tokenFile.exists())tokenFile.readText().trim() else ""
            buildConfigField("String","LOCAL_API_TOKEN","\"$token\"")
        }
        getByName("release") {
            buildConfigField("String","LOCAL_API_TOKEN","\"\"")
            if(releaseProperties.exists())signingConfig=signingConfigs.getByName("localRelease")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":shared"))
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
