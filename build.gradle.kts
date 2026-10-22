plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}

detekt {
    toolVersion = "1.23.8"
    source.setFrom(files("app/src/main/java", "app/src/main/kotlin"))
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = true // Initial audit: report existing findings without failing CI.
    basePath = rootDir.absolutePath
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    include("**/*.kt")
    exclude("**/build/**", "**/generated/**")
    // Syntax-only analysis: no application classpath or compiler-version overrides.
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        txt.required.set(true)
        md.required.set(false)
    }
}

// Separate measurements from threshold-based findings; never use these as a gate.
tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektComplexity") {
    description = "Measures production function complexity for the informational audit."
    group = "verification"
    setSource(detekt.source)
    config.setFrom(files("config/detekt/complexity.yml"))
    buildUponDefaultConfig = false
    ignoreFailures = true
    basePath = rootDir.absolutePath
    reports {
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/complexity.html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/complexity.xml"))
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/complexity.sarif"))
        txt.outputLocation.set(layout.buildDirectory.file("reports/detekt/complexity.txt"))
    }
}
