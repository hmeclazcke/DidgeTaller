plugins { java }

val kotlinToolsVersion = "2.2.21"
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinToolsVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinToolsVersion")
    testImplementation("junit:junit:4.13.2")
}

// Research code needs desktop JDK audio and allocation counters. Keep it out of Android.
val pitchSources = rootProject.fileTree("tools/pitch-study") { include("*.kt") }
val pitchClasses = layout.buildDirectory.dir("classes/pitch-study")
val appClasses = project(":app").layout.buildDirectory.dir("tmp/kotlin-classes/debug")
val pitchClasspath = files(appClasses, configurations.runtimeClasspath.map { cp ->
    cp.filter { it.name.startsWith("kotlin-stdlib-") }
})
val compilePitchStudy by tasks.registering(JavaExec::class) {
    dependsOn(":app:compileDebugKotlin")
    inputs.files(pitchSources, pitchClasspath)
    outputs.dir(pitchClasses)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    doFirst {
        args("-no-stdlib", "-no-reflect", "-jvm-target", "17", "-classpath", pitchClasspath.asPath,
            "-d", pitchClasses.get().asFile.absolutePath)
        args(pitchSources.files.sorted().map { it.absolutePath })
    }
}

tasks.register<JavaExec>("pitchStudy") {
    group = "verification"
    dependsOn(compilePitchStudy)
    classpath(pitchClasses, pitchClasspath)
    mainClass.set("ar.didgetaller.study.PitchStudyKt")
    args(rootProject.layout.buildDirectory.dir("reports/pitch-study").get().asFile,
        providers.gradleProperty("pitchSplit").getOrElse("development"))
}

tasks.register<JavaExec>("sourceInventory") {
    group = "verification"
    description = "Inventories actual Kotlin PSI declarations without guessing from source regexes."
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("ar.didgetaller.engineering.SourceInventory")
    workingDir(rootDir)
    args(providers.gradleProperty("inventorySource").getOrElse("app/src/main"),
         providers.gradleProperty("inventoryOutput").getOrElse("build/reports/quality/source-inventory.xml"))
}

tasks.register<JavaExec>("bytecodeInventory") {
    group = "verification"
    description = "Reads exact JVM names/descriptors from compiled Kotlin metadata."
    dependsOn(tasks.classes, ":app:compileDebugKotlin")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("ar.didgetaller.engineering.JvmMethodInventory")
    workingDir(rootDir)
    doFirst {
        check(providers.gradleProperty("kotlin.incremental").orNull == "false") {
            "Canonical quality reports require -Pkotlin.incremental=false"
        }
    }
    args("app/build/tmp/kotlin-classes/debug", "build/reports/quality/jvm-method-inventory.xml", "false")
}
