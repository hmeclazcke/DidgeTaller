import java.util.Properties

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

val productionCoverageClasses=files(
    layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
    layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
).asFileTree.matching {
    include("ar/didgetaller/**/*.class")
    exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.class", "**/Manifest*.class", "**/*\$WhenMappings.class")
    // Retain handwritten lambdas and Compose code; JaCoCo filters compiler scaffolding.
}

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Reports debug JVM coverage without enforcing a coverage threshold."
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(productionCoverageClasses)
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

val verifyEmulatorCoverageData=tasks.register("verifyEmulatorCoverageData") {
    doLast {
        val data=rootProject.file("build/reports/quality/instrumentation.ec")
        check(data.isFile && data.length()>0) {"Missing emulator coverage data; execute the coverage-enabled suite first."}
    }
}

tasks.register<JacocoReport>("jacocoEmulatorReport") {
    group="verification"
    description="Reports a completed explicit-emulator run separately from JVM coverage."
    dependsOn("compileDebugKotlin","compileDebugJavaWithJavac",verifyEmulatorCoverageData)
    executionData(rootProject.file("build/reports/quality/instrumentation.ec"))
    classDirectories.setFrom(productionCoverageClasses)
    sourceDirectories.setFrom(files("src/main/java","src/main/kotlin"))
    reports {
        html.required.set(true)
        xml.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/emulator/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/emulator/coverage.xml"))
    }
    doFirst {
        check(classDirectories.files.any {it.extension=="class"}) {"Missing production classes."}
        check(executionData.files.all {it.isFile && it.length()>0}) {"Missing emulator coverage data."}
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
    testCoverage {jacocoVersion="0.8.15"}
    buildTypes.getByName("debug") {
        enableAndroidTestCoverage=providers.gradleProperty("androidCoverage").orNull=="true"
    }
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

// Standalone JVM tools avoid coupling mutation execution to an unsupported Android PIT plugin.
val qualityPolicy=Properties().apply {rootProject.file("config/quality/policy.properties").inputStream().use {load(it)}}
val qualityCpd by configurations.creating
val qualityPit by configurations.creating
dependencies {
    qualityCpd("net.sourceforge.pmd:pmd-cli:7.19.0")
    qualityCpd("net.sourceforge.pmd:pmd-kotlin:7.19.0")
    qualityPit("org.pitest:pitest-command-line:1.20.0")
}
tasks.register<JavaExec>("qualityDuplication") {
    group="verification"
    classpath=qualityCpd
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    val output=layout.buildDirectory.file("reports/quality/cpd.xml")
    doFirst {output.get().asFile.parentFile.mkdirs()}
    args("cpd","--language","kotlin","--minimum-tokens",qualityPolicy.getProperty("cpdMinimumTokens"),"--dir","src/main/java",
        "--format","xml","--report-file",output.get().asFile.absolutePath,"--no-fail-on-violation")
}
tasks.register<JavaExec>("qualityMutation") {
    group="verification"
    dependsOn("testDebugUnitTest")
    val unit=tasks.named<Test>("testDebugUnitTest")
    classpath=files(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) + qualityPit + files(provider {unit.get().classpath}) + files(provider {unit.get().testClassesDirs})
    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    val mutationXml=layout.buildDirectory.file("reports/quality/pit/mutations.xml")
    doFirst {mutationXml.get().asFile.delete()}
    doLast {check(mutationXml.get().asFile.let {it.isFile && it.readText().contains("<mutation ")}) {"PIT did not produce mutations; inspect tool output."}}
    args("--reportDir",layout.buildDirectory.dir("reports/quality/pit").get().asFile.absolutePath,
        "--targetClasses","ar.didgetaller.core.PlotTouchKt",
        "--targetTests","ar.didgetaller.core.PlotTouchTest",
        "--sourceDirs","src/main/java","--outputFormats","XML,HTML","--timestampedReports","false",
        "--mutationThreshold",qualityPolicy.getProperty("mutationThreshold"),
        "--threads","1","--mutators","DEFAULTS","--timeoutConst","4000")
}
