plugins { java }

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.21")
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
