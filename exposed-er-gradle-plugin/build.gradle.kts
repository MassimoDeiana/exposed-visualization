plugins {
    kotlin("jvm") version "2.2.0"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.massimodeiana"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.massimodeiana:exposed-er-core:0.1.0")
    implementation("io.github.massimodeiana:exposed-er-reflection:0.1.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website = "https://github.com/MassimoDeiana/exposed-visualization"
    vcsUrl = "https://github.com/MassimoDeiana/exposed-visualization"

    plugins {
        create("exposedEr") {
            id = "io.github.massimodeiana.exposed-er"
            displayName = "Exposed ER Diagram"
            description = "Generate ER diagrams from Kotlin Exposed table definitions"
            tags = listOf("kotlin", "exposed", "database", "diagram", "er-diagram", "orm")
            implementationClass = "io.github.massimodeiana.exposed.er.gradle.ExposedErPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
