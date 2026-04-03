package io.github.massimodeiana.exposed.er.maven

import io.github.massimodeiana.exposed.er.core.renderer.MermaidRenderer
import io.github.massimodeiana.exposed.er.reflection.SchemaExtractor
import io.github.massimodeiana.exposed.er.reflection.TableScanner
import java.io.File

data class GenerationResult(val tableCount: Int)

class DiagramGenerator {

    fun generate(
        packages: List<String>,
        outputFile: File,
        classLoader: ClassLoader
    ): GenerationResult {
        val tables = TableScanner().scan(packages, classLoader)
        if (tables.isEmpty()) {
            return GenerationResult(tableCount = 0)
        }

        val schema = SchemaExtractor().extract(tables)
        val mermaid = MermaidRenderer().render(schema)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(mermaid)

        return GenerationResult(tableCount = tables.size)
    }
}
