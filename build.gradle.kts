plugins {
    `java`
    checkstyle
    id("com.github.spotbugs") version "6.0.18"
    jacoco
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.bstats:bstats-bukkit:3.0.2")

    testImplementation("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
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

