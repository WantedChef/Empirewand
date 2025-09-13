
import com.github.spotbugs.snom.Confidence
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:9.7")
        classpath("org.ow2.asm:asm-commons:9.7")
    }
}

plugins {
    java
    checkstyle
    id("com.github.spotbugs") version "6.0.19"
    jacoco
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    implementation("org.bstats:bstats-bukkit:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        relocate("org.bstats", "nl.wantedchef.empirewand.shaded.bstats")
        archiveClassifier.set("") // Produce a single, standard JAR
    }
    checkstyle {
        toolVersion = "10.12.0"
        configFile = file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        maxWarnings = 0
    }
    withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
    }
    spotbugs {
        excludeFilter.set(file("config/spotbugs/exclude.xml"))
        reportLevel.set(Confidence.HIGH)
    }
    withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("html") {
            required.set(true)
        }
    }
    spotbugsMain {
        enabled = true
    }
    spotbugsTest {
        enabled = false
    }
    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }
    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }
    check {
        dependsOn(jacocoTestCoverageVerification)
    }
}

bukkit {
    name = "EmpireWand"
    main = "nl.wantedchef.empirewand.EmpireWandPlugin"
    apiVersion = "1.20"
    authors = listOf("ChefWanted")
    description = "EmpireWand example plugin"
    commands {
        register("ew") {
            description = "EmpireWand commands"
            usage = "/ew <get|bind|unbind|bindall|bindtype|bindcat|set-spell|list|reload|migrate|spells|toggle|stats|switcheffect|cd>"
        }
        register("mz") {
            description = "MephidantesZeist commands"
            usage = "/mz <get|bind|unbind|bindall|bindtype|bindcat|set-spell|list|reload|migrate|spells|toggle|stats|switcheffect|cd>"
        }
    }
    permissions {
        register("empirewand.command.get") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.bind") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.unbind") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.bindall") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.bindtype") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.bindcat") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.set-spell") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.list") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.spells") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.reload") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.migrate") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.toggle") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.stats") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.switcheffect") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.cooldown.toggle") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.cooldown.clear") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.command.cooldown.status") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.command.cooldown.admin") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.get") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.bind") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.unbind") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.bindall") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.bindtype") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.bindcat") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.set-spell") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.list") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.spells") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.reload") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.migrate") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.toggle") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.stats") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.switcheffect") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.cooldown.toggle") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.cooldown.clear") { default = BukkitPluginDescription.Permission.Default.OP }
        register("mephidanteszeist.command.cooldown.status") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("mephidanteszeist.command.cooldown.admin") { default = BukkitPluginDescription.Permission.Default.OP }
        register("empirewand.spell.use.*") { default = BukkitPluginDescription.Permission.Default.TRUE }
        register("empirewand.spell.bind.*") { default = BukkitPluginDescription.Permission.Default.OP }

        val spells = listOf(
            "leap", "comet", "explosive", "magic-missile", "heal", "glacial-spike", "grasping-vines",
            "lifesteal", "polymorph", "ethereal-form", "frost-nova", "chain-lightning", "blink-strike",
            "shadow-cloak", "stasis-field", "gust", "arcane-orb", "void-swap", "sandstorm", "tornado",
            "aura", "blaze-launch", "blood-barrier", "blood-block", "blood-nova", "blood-spam",
            "blood-tap", "comet-shower", "confuse", "crimson-chains", "dark-circle", "dark-pulse",
            "earth-quake", "empire-aura", "empire-comet", "empire-escape", "empire-launch",
            "empire-levitate", "explosion-trail", "explosion-wave", "fireball", "flame-wave",
            "god-cloud", "hemorrhage", "life-reap", "lightning-arrow", "lightning-bolt",
            "lightning-storm", "lightwall", "little-spark", "mephidic-reap", "poison-wave",
            "radiant-beacon", "ritual-of-unmaking", "solar-lance", "soul-sever", "spark",
            "sunburst-step", "teleport", "thunder-blast"
        )
        spells.forEach { spell ->
            register("empirewand.spell.use.$spell") {
                default = BukkitPluginDescription.Permission.Default.TRUE
            }
            register("empirewand.spell.bind.$spell") {
                default = BukkitPluginDescription.Permission.Default.OP
            }
        }
    }
}
