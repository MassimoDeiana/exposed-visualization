plugins {
    kotlin("jvm") version "2.2.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.massimodeiana"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.massimodeiana:exposed-er-core:0.1.0-SNAPSHOT")
    implementation("io.github.massimodeiana:exposed-er-reflection:0.1.0-SNAPSHOT")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("exposedEr") {
            id = "io.github.massimodeiana.exposed-er"
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
