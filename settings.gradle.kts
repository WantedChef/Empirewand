pluginManagement {
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2/") }
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "1.9.23"
    }
}

rootProject.name = "empirewand"
