import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "io.github.massimodeiana"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("io.github.massimodeiana:exposed-er-core:0.1.0-SNAPSHOT")

    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        bundledPlugin("org.jetbrains.kotlin")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.massimodeiana.exposed-er"
        name = "Exposed ER Diagram"
        version = project.version.toString()
    }
}

kotlin {
    jvmToolchain(21)
}
