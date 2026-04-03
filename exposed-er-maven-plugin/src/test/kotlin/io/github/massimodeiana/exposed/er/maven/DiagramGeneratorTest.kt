package io.github.massimodeiana.exposed.er.maven

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagramGeneratorTest {

    private val generator = DiagramGenerator()

    @Test
    fun `should write mermaid output to file`() {
        val tempDir = Files.createTempDirectory("er-test").toFile()
        val outputFile = File(tempDir, "er-diagram.mmd")

        val result = generator.generate(
            packages = listOf("io.github.massimodeiana.exposed.er.reflection.fixtures"),
            outputFile = outputFile,
            classLoader = javaClass.classLoader
        )

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.startsWith("erDiagram"))
        assertTrue(content.contains("users {"))
        assertTrue(content.contains("recipes {"))
        assertTrue(result.tableCount > 0)

        tempDir.deleteRecursively()
    }

    @Test
    fun `should create parent directories if they do not exist`() {
        val tempDir = Files.createTempDirectory("er-test").toFile()
        val outputFile = File(tempDir, "nested/deep/er-diagram.mmd")

        generator.generate(
            packages = listOf("io.github.massimodeiana.exposed.er.reflection.fixtures"),
            outputFile = outputFile,
            classLoader = javaClass.classLoader
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().startsWith("erDiagram"))

        tempDir.deleteRecursively()
    }

    @Test
    fun `should return zero table count when no tables found`() {
        val tempDir = Files.createTempDirectory("er-test").toFile()
        val outputFile = File(tempDir, "er-diagram.mmd")

        val result = generator.generate(
            packages = listOf("io.github.massimodeiana.nonexistent"),
            outputFile = outputFile,
            classLoader = javaClass.classLoader
        )

        assertEquals(0, result.tableCount)
        assertTrue(!outputFile.exists())

        tempDir.deleteRecursively()
    }
}
