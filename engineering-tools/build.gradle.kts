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
    args("app/src/main/java", "build/reports/quality/source-inventory.xml")
}
