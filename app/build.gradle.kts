import java.util.Properties
import java.util.concurrent.TimeUnit

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
        versionCode = 13
        versionName = "3.3.0-beta1"
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
    // JVM codec characterization; Android's own org.json is verified separately on the emulator.
    testImplementation("org.json:json:20250517")
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
fun registerMutation(name: String, directory: String, targets: String, testTargets: String, threshold: String?) {
    tasks.register<JavaExec>(name) {
        group="verification"
        dependsOn("testDebugUnitTest")
        val unit=tasks.named<Test>("testDebugUnitTest")
        classpath=files(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) + qualityPit + files(provider {unit.get().classpath}) + files(provider {unit.get().testClassesDirs})
        mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
        val mutationXml=layout.buildDirectory.file("reports/quality/$directory/mutations.xml")
        doFirst {mutationXml.get().asFile.delete()}
        doLast {
            val python = if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3"
            val validation = ProcessBuilder(python, rootProject.file("tools/mutation_report.py").absolutePath,
                mutationXml.get().asFile.absolutePath).directory(rootDir).redirectErrorStream(true).start()
            val report = validation.inputStream.bufferedReader().use { it.readText() }
            check(validation.waitFor() == 0) { "PIT report verification failed: $report" }
            logger.lifecycle(report.trim())
        }
        args("--reportDir",layout.buildDirectory.dir("reports/quality/$directory").get().asFile.absolutePath,
            "--targetClasses",targets,"--targetTests",testTargets,
            "--sourceDirs","src/main/java","--outputFormats","XML,HTML","--timestampedReports","false",
            "--threads","2","--mutators","DEFAULTS","--timeoutConst","4000",
            "--excludedClasses","*Test*,*DifferentialProbe*,*Benchmark*")
        if(threshold!=null)args("--mutationThreshold",threshold)
    }
}
registerMutation("qualityMutation", "pit", "ar.didgetaller.core.PlotTouchKt", "ar.didgetaller.core.PlotTouchTest", qualityPolicy.getProperty("mutationThreshold"))
registerMutation("qualityMutationWorkshop", "pit-workshop",
    "ar.didgetaller.core.WorkshopPlans,ar.didgetaller.core.WorkshopPlans\$*,ar.didgetaller.core.WorkshopExports,ar.didgetaller.core.ConstructionProjects,ar.didgetaller.core.ConstructionCatalog,ar.didgetaller.core.ConstructionDrawing,ar.didgetaller.core.ConstructionForm,ar.didgetaller.core.ConstructionSearch,ar.didgetaller.core.ConstructionAnalysisKt",
    "ar.didgetaller.core.Workshop*Test,ar.didgetaller.core.Construction*Test", null)
registerMutation("qualityMutationMetronome", "pit-metronome",
    "ar.didgetaller.core.MetronomeEngine,ar.didgetaller.core.MetronomeGrid,ar.didgetaller.core.MetronomeSettings*,ar.didgetaller.core.FocusedMetronomeClick,ar.didgetaller.core.MetronomeOutputKt,ar.didgetaller.core.MetronomePcm,ar.didgetaller.core.MetronomeAudioKt",
    "ar.didgetaller.core.Metronome*Test", null)

// The expanded domain report is informational until survivors have been individually reviewed.
val domainTargets=listOf("MetronomeEngine", "MetronomeGrid", "MetronomeSettings*", "FocusedMetronomeClick", "MetronomeOutputKt", "PitchDetector", "YinPeriodSearch", "YinPeriodSearch\$Companion", "PitchTrace", "PitchTrace\$Builder",
    "Music", "LivePitch", "SessionAnalysis", "AxisDomain", "PlotProjection", "PlotTouchKt", "PlaybackController",
    "PlaybackController\$events\$1", "WavCodec", "ProgressiveWavWriter", "ProgressiveWavWriter\$Companion", "Pcm16WavReader", "RecordingCapture", "CaptureHandoff", "RecordingCoordinator", "SpectrogramRaster", "PlotReduction", "TraceDrawing", "WaveGeometry\$Companion", "RasterPlacement\$Companion")
val syntheticTests=fileTree("src/test/java/ar/didgetaller/core").matching {include("*Test.kt");exclude("Real*Test.kt")}
    .files.sortedBy {it.name}.joinToString(",") {"ar.didgetaller.core."+it.nameWithoutExtension}
val persistenceTargets = listOf("StoredDocument", "RecordingMetadata", "MeasurementMetadata", "SessionToolsMetadata",
    "JsonPreferenceDocument", "JsonPreferenceDocument\$Companion", "LibrarySnapshot\$Companion").joinToString(",") { "ar.didgetaller.data.$it" }
val persistenceTests = listOf("StoredDocumentTest", "RecordingMetadataTest", "AuxiliaryMetadataTest", "JsonPreferenceDocumentTest", "LibrarySnapshotTest")
    .joinToString(",") { "ar.didgetaller.data.$it" }
val workspaceTargets = listOf("WorkspaceController", "WorkspaceData", "WorkspaceSource", "WorkspaceState",
    "VisualRequest", "VisualRequest\$Companion", "VisualDataKey", "VisualFrames\$Companion").joinToString(",") { "ar.didgetaller.core.$it" } +
    ",ar.didgetaller.audio.CoroutineWorkspaceLoader*"
val simulationTargets = listOf("SimulationController", "SimulationState", "SimulationState\$Working",
    "SimulationState\$Ready", "SimulationState\$Failed").joinToString(",") { "ar.didgetaller.core.$it" } +
    ",ar.didgetaller.lab.CoroutineSimulationLoader*"
val analysisTargets = listOf("RecordingAnalysisController", "RecordingAnalysisStatus",
    "RecordingAnalysisStatus\$Running", "RecordingAnalysisStatus\$Ready", "RecordingAnalysisStatus\$Failed",
    "RecordingAnalysisStatus\$Cancelled").joinToString(",") { "ar.didgetaller.core.$it" } +
    ",ar.didgetaller.audio.CoroutineRecordingAnalysisLoader*"
val progressWorkTargets = "ar.didgetaller.audio.ProgressWorkKt*"
val swipeTargets = listOf("SwipeComparisonController", "SwipeComparisonStatus", "SwipeComparisonStatus\$Disabled",
    "SwipeComparisonStatus\$Loading", "SwipeComparisonStatus\$Ready", "SwipeComparisonStatus\$Failed",
    "SwipeComparisonLoaderKt").joinToString(",") { "ar.didgetaller.core.$it" } +
    ",ar.didgetaller.audio.CoroutineSwipeComparisonLoader*,ar.didgetaller.ui.ComparisonCamerasKt"
val ownedTargets = listOf("LatestAnalysis*", "AnalysisState*", "SessionAnalyses*", "MusicalTraceCatalog*",
    "OptimizationWorkKt*", "AnalysisInterval*", "AnalysisAdmission*", "OfflineWindowPass*", "OfflineSpectrum*",
    "OfflineStatistics*", "RecordingWindow*", "OfflineRecordingAnalysisKt*", "WavAnalysis*", "MusicalTrace*")
    .joinToString(",") { "ar.didgetaller.core.$it" } +
    ",ar.didgetaller.audio.CoroutineAnalysisLoader*,ar.didgetaller.SavedDiagnosticStateKt*,ar.didgetaller.ui.SessionInspector*"
val ownedTests = syntheticTests + ",ar.didgetaller.SavedDiagnosticStateTest,ar.didgetaller.ui.SessionInspectorTest"
registerMutation("qualityMutationOwned", "pit-owned", ownedTargets, ownedTests, null)
registerMutation("qualityMutationDomain", "pit-domain", domainTargets.joinToString(",") {"ar.didgetaller.core.$it"} + "," + ownedTargets + ",ar.didgetaller.data.AtomicCacheFile," + persistenceTargets + "," + workspaceTargets + "," + simulationTargets + "," + analysisTargets + "," + swipeTargets + "," + progressWorkTargets + ",ar.didgetaller.core.SpectrumGeometry\$Companion,ar.didgetaller.core.AxisDrawing,ar.didgetaller.core.PlotGestureCoordinates,ar.didgetaller.core.TraceEvidence\$Companion,ar.didgetaller.ui.PlotInkKt,ar.didgetaller.ui.PlotContentKt,ar.didgetaller.ui.PlotFramePolicyKt",
    ownedTests + ",ar.didgetaller.data.AtomicCacheFileTest,ar.didgetaller.ui.ComparisonCamerasTest,ar.didgetaller.ui.PlotInkTest,ar.didgetaller.ui.PlotFramePolicyTest," + persistenceTests, null)
registerMutation("qualityMutationCapture", "pit-capture", "ar.didgetaller.core.RecordingCapture", "ar.didgetaller.core.RecordingCaptureTest", null)
registerMutation("qualityMutationHandoff", "pit-handoff", "ar.didgetaller.core.CaptureHandoff,ar.didgetaller.core.RecordingCoordinator", "ar.didgetaller.core.CaptureHandoffTest,ar.didgetaller.core.RecordingCoordinatorTest", null)
registerMutation("qualityMutationRaster", "pit-raster", "ar.didgetaller.core.SpectrogramRaster", "ar.didgetaller.core.SpectrogramRasterTest", null)
registerMutation("qualityMutationPlayback", "pit-playback", "ar.didgetaller.core.PlaybackController,ar.didgetaller.core.PlaybackController\$events\$1", "ar.didgetaller.core.PlaybackControllerTest", null)
registerMutation("qualityMutationCache", "pit-cache", "ar.didgetaller.data.AtomicCacheFile", "ar.didgetaller.data.AtomicCacheFileTest", null)
registerMutation("qualityMutationPersistence", "pit-persistence", persistenceTargets, persistenceTests, null)
registerMutation("qualityMutationWorkspace", "pit-workspace", workspaceTargets, "ar.didgetaller.core.Workspace*Test", null)
registerMutation("qualityMutationPlots", "pit-plots", "ar.didgetaller.core.PlotReduction,ar.didgetaller.core.TraceDrawing,ar.didgetaller.core.WaveGeometry\$Companion,ar.didgetaller.core.RasterPlacement\$Companion,ar.didgetaller.core.SpectrumGeometry\$Companion,ar.didgetaller.core.AxisDrawing,ar.didgetaller.core.PlotGestureCoordinates,ar.didgetaller.core.TraceEvidence\$Companion,ar.didgetaller.ui.PlotInkKt,ar.didgetaller.ui.PlotContentKt,ar.didgetaller.ui.PlotFramePolicyKt", "ar.didgetaller.core.PlotReductionTest,ar.didgetaller.core.TraceDrawingTest,ar.didgetaller.core.VisualGeometryTest,ar.didgetaller.core.SpectrumGeometryTest,ar.didgetaller.ui.PlotInkTest,ar.didgetaller.ui.PlotFramePolicyTest,ar.didgetaller.core.AxisDrawingTest,ar.didgetaller.core.PlotGestureCoordinatesTest,ar.didgetaller.core.TraceEvidenceTest,ar.didgetaller.core.ComparisonDrawingTest,ar.didgetaller.core.TracePreparationTest", null)
registerMutation("qualityMutationSimulation", "pit-simulation", simulationTargets, "ar.didgetaller.core.Simulation*Test", null)
registerMutation("qualityMutationAnalysis", "pit-analysis", analysisTargets + "," + progressWorkTargets, "ar.didgetaller.core.RecordingAnalysis*Test,ar.didgetaller.core.ProgressWorkTest", null)
registerMutation("qualityMutationSwipe", "pit-swipe", swipeTargets + "," + progressWorkTargets, "ar.didgetaller.core.SwipeComparison*Test,ar.didgetaller.core.ProgressWorkTest,ar.didgetaller.ui.ComparisonCamerasTest", null)

fun registerComparison(name: String, benchmark: Boolean) {
 tasks.register(name) {
    group = "verification"
    description = "Runs the same synthetic behavioral probe against pinned original and rebuilt bytecode."
    dependsOn("testDebugUnitTest")
    if(benchmark)dependsOn(":engineering-tools:classes")
    val baseline = providers.gradleProperty("differentialBaseline")
    doLast {
        val original = rootProject.file(baseline.get())
        check(original.isFile) { "Provide -PdifferentialBaseline pointing to the pinned original production JAR." }
        val unit = tasks.named<Test>("testDebugUnitTest").get()
        val production = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
        val dependencies = unit.classpath.files.filterNot { it == production }
        val report = rootProject.layout.buildDirectory.dir("reports/quality/differential").get().asFile.apply { mkdirs() }
        for ((name, classes) in listOf("original" to original, "rebuilt" to production)) {
            val benchmarkClasses = if(benchmark)listOf(rootProject.project(":engineering-tools").layout.buildDirectory.dir("classes/java/main").get().asFile) else emptyList()
            val classpath = (listOf(classes) + benchmarkClasses + unit.testClassesDirs.files + dependencies).joinToString(File.pathSeparator)
            val output = File(report, if(benchmark) "$name-performance.txt" else "$name.txt")
            val error = File(report, "$name.stderr.txt")
            val command = listOf(File(System.getProperty("java.home"), "bin/java").absolutePath,
                "-cp", classpath, if(benchmark)"ar.didgetaller.engineering.DspBenchmark" else "ar.didgetaller.core.DifferentialProbe")
            val process = ProcessBuilder(command)
                .redirectOutput(output).redirectError(error).start()
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor()
                error("Differential probe timed out: $name")
            }
            check(process.exitValue() == 0) { "Differential probe failed: $name; inspect $error" }
        }
        if(!benchmark)check(File(report, "original.txt").readBytes().contentEquals(File(report, "rebuilt.txt").readBytes())) {
            "Behavior differs from the original oracle; inspect build/reports/quality/differential."
        }
        logger.lifecycle(if(benchmark) "Performance samples saved separately; no timing threshold is enforced." else "Differential synthetic contracts match byte-for-byte.")
    }
}
}
registerComparison("qualityDifferential", false)
registerComparison("qualityPerformanceComparison", true)
