plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        // Instrument project code only; do not instrument Android/JDK/dependencies.
        includes = listOf("ar.didgetaller.*")
    }
}

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Reports debug JVM coverage without enforcing a coverage threshold."
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files(
        layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
        layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    ).asFileTree.matching {
        include("ar/didgetaller/**/*.class")
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.class", "**/Manifest*.class", "**/*\$WhenMappings.class")
        // Keep lambda/ComposableSingletons classes: they can contain handwritten code.
        // JaCoCo's Kotlin/Compose bytecode filters handle compiler scaffolding.
    })
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/debug/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/debug/coverage.xml"))
    }
    doFirst {
        check(classDirectories.files.any { it.extension == "class" }) { "No production class files found for coverage." }
        check(executionData.files.all { it.isFile && it.length() > 0 }) { "Missing JVM coverage execution data." }
    }
}

android {
    namespace = "ar.didgetaller"
    compileSdk = 35
    ndkVersion = "27.0.12077973"
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" }
    }

    defaultConfig {
        applicationId = "ar.didgetaller"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "3.2.6-beta1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // API 37 removed InputManager.getInstance, used reflectively by older Espresso.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
