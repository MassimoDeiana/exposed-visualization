package io.github.massimodeiana.exposed.er.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExposedErPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `should register generateErDiagram task`() {
        writeBuildFile()
        writeSettingsFile()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=documentation")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("generateErDiagram"))
    }

    @Test
    fun `should generate mermaid file from exposed tables`() {
        writeBuildFile(
            """
            exposedErDiagram {
                packages.set(listOf("io.github.massimodeiana.exposed.er.gradle.testtables"))
                outputFile.set(layout.buildDirectory.file("er-diagram.mmd"))
            }
            """.trimIndent()
        )
        writeSettingsFile()
        writeTestTable()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateErDiagram", "--stacktrace")
            .forwardOutput()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateErDiagram")?.outcome)

        val outputFile = File(projectDir, "build/er-diagram.mmd")
        assertTrue(outputFile.exists())

        val content = outputFile.readText()
        assertTrue(content.startsWith("erDiagram"))
        assertTrue(content.contains("test_users {"))
        assertTrue(content.contains("long id PK"))
    }

    private fun writeBuildFile(extraConfig: String = "") {
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.2.0"
                id("io.github.massimodeiana.exposed-er")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation("org.jetbrains.exposed:exposed-core:1.2.0")
            }

            $extraConfig
            """.trimIndent()
        )
    }

    private fun writeSettingsFile() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )
    }

    private fun writeTestTable() {
        val tableDir = File(projectDir, "src/main/kotlin/io/github/massimodeiana/exposed/er/gradle/testtables")
        tableDir.mkdirs()
        File(tableDir, "TestUsers.kt").writeText(
            """
            package io.github.massimodeiana.exposed.er.gradle.testtables

            import org.jetbrains.exposed.v1.core.Table

            object TestUsers : Table("test_users") {
                val id = long("id").autoIncrement()
                val email = varchar("email", 255)
                val name = varchar("name", 255)
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        )
    }
}
