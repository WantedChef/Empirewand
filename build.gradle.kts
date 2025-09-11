plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    checkstyle
    //id("com.github.spotbugs") version "5.2.1"
}

group = "nl.wantedchef.empirewand"
version = "1.1.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    // Paper repo - with fallback to Spigot
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("io.papermc")
        }
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        content {
            includeGroup("org.spigotmc")
        }
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        content {
            includeGroup("org.spigotmc")
        }
    }
}

dependencies {
    // Spigot API as fallback to Paper API (provided by server at runtime)
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    // Annotations (optional, compileOnly)
    compileOnly("org.jetbrains:annotations:24.1.0")
    // SpotBugs annotations used by @SuppressFBWarnings
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    // bStats metrics (classes are referenced; provided at runtime by bundled libs or can be shaded if needed)
    compileOnly("org.bstats:bstats-bukkit:3.0.2")
    
    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
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

checkstyle {
    toolVersion = "10.12.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}


}

// Enable test compilation/execution
tasks.compileTestJava {
    enabled = true
}
tasks.test {
    enabled = true
    useJUnitPlatform()
}
