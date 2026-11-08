plugins { java }

val kotlinToolsVersion = "2.2.21"
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinToolsVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinToolsVersion")
    testImplementation("junit:junit:4.13.2")
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
