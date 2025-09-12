import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
    kotlin("jvm") version "1.9.23"
    checkstyle
    id("com.github.spotbugs") version "5.2.1"
    jacoco
    id("com.gradleup.shadow") version "9.1.0"
}

group = "nl.wantedchef.empirewand"
version = "1.1.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("com.mojang")
            includeGroup("net.md-5")
        }
    }
}

dependencies {
    // Paper API to compile against Bukkit/Spigot API (provided by server at runtime)
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    // Annotations (optional, compileOnly)
    compileOnly("org.jetbrains:annotations:24.1.0")
    // SpotBugs annotations used by @SuppressFBWarnings
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    // bStats metrics (bundled and relocated in shaded jar; disabled by default via config)
    implementation("org.bstats:bstats-bukkit:3.0.2")

    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.9.0")
    testImplementation("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    // Include bStats at test runtime to satisfy MetricsService dependencies
    testImplementation("org.bstats:bstats-bukkit:3.0.2")
}

tasks.shadowJar {
    // We leveren alleen een shaded plugin jar (met "all" classifier) voor gebruik op de server
    archiveClassifier.set("all")
    // Relocate bStats om classpath conflicts met andere plugins te voorkomen.
    // Heractiveerd met Shadow 9.1.0 - volledige JDK 21 ondersteuning met ASM 9.8
    relocate("org.bstats", "nl.wantedchef.empirewand.shaded.bstats")
    // Manifest attributes voor debugging
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "EmpireWand",
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
                "Built-JDK" to System.getProperty("java.version"),
                "Created-By" to "Gradle Shadow"
            )
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    // Vervang placeholders in plugin.yml. Gebruik een nette weergavenaam i.p.v. rootProject.name
    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "name" to "EmpireWand", // weergavenaam in /plugins lijst
                "version" to project.version.toString()
            )
        )
    }
}

// Schakel de 'plain' jar uit om conflicts/overschrijven door shadowJar te voorkomen
tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                // Restored to 80% coverage requirement for production quality
                minimum = "0.80".toBigDecimal()
            }
        }
    }
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

spotbugs {
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") {
        required.set(true)
    }
    reports.create("xml") {
        required.set(true)
    }
}

tasks.spotbugsMain {
    enabled = true
}

tasks.spotbugsTest {
    enabled = true
}

// Enable test compilation/execution
tasks.compileTestJava {
    enabled = true
}
tasks.test {
    enabled = true
    useJUnitPlatform()
    // Allow ByteBuddy to instrument Java 21 classes for Mockito inline
    jvmArgs("-Dnet.bytebuddy.experimental=true")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn("jacocoTestCoverageVerification")
}
