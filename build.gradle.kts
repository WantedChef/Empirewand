plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example.empirewand"
version = "1.1.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API to compile against Bukkit/Spigot API (provided by server at runtime)
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    // Annotations (optional, compileOnly)
    compileOnly("org.jetbrains:annotations:24.1.0")
    // SpotBugs annotations used by @SuppressFBWarnings
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    // bStats metrics (classes are referenced; provided at runtime by bundled libs or can be shaded if needed)
    compileOnly("org.bstats:bstats-bukkit:3.0.2")
}

tasks.processResources {
    // Replace ${name} and ${version} in plugin.yml
    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "name" to (project.name),
                "version" to (project.version.toString())
            )
        )
    }
}

tasks.jar {
    // Keep default archiveBaseName = project.name (set in settings.gradle.kts)
    // processResources already adds resources; avoid duplicates
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    archiveClassifier.set("all")
    // No relocation by default; add if shading third-party libs
    // Note: minimization can fail with newer class file versions; disabled here
}

tasks.build {
    dependsOn(tasks.jar)
    dependsOn(tasks.shadowJar)
}

// Disable test compilation/execution unless explicitly enabled later
tasks.compileTestJava {
    enabled = false
}
tasks.test {
    enabled = false
}
