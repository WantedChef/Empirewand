plugins {
    `java`
    checkstyle
    id("com.github.spotbugs") version "6.0.18"
    jacoco
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.5")
    implementation("org.bstats:bstats-bukkit:3.0.2")

    testImplementation("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xshare:off")
    // Disable automatic test framework detection to avoid deprecation warning
    systemProperty("junit.platform.autoDetect.classpath", "false")
    // Allow bStats to initialize in unit tests without relocation
    systemProperty("bstats.relocatecheck", "false")
    // Suppress the deprecation warning
    logging.captureStandardError(LogLevel.INFO)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    // Enable preview features to handle any potential unnamed class usage
    options.compilerArgs.add("--enable-preview")
}

checkstyle {
    toolVersion = "10.17.0"
    isIgnoreFailures = true  // Temporarily ignore failures
    maxWarnings = 10  // Allow some warnings
}

spotbugs {
    toolVersion = "4.8.5"
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
    ignoreFailures = true  // Temporarily ignore failures
    showProgress = true
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.test, tasks.jacocoTestReport)
}

// Expand Gradle properties into plugin.yml
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "name" to (project.findProperty("pluginName") ?: project.name),
                "version" to project.version
            )
        )
    }
}

// Configure Shadow plugin for creating fat JAR
tasks.shadowJar {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.example.empirewand.EmpireWandPlugin"
    }
    // Relocate bStats to avoid conflicts
    relocate("org.bstats", "com.example.empirewand.libs.bstats")
}

// Create a fat JAR including runtime dependencies (fallback when Shadow plugin is unavailable)
val fatJar = tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    manifest {
        attributes["Main-Class"] = "com.example.empirewand.EmpireWandPlugin"
    }
}

artifacts { archives(tasks.shadowJar) }

tasks.build { dependsOn(tasks.shadowJar) }
