package io.github.massimodeiana.exposed.er.gradle

import io.github.massimodeiana.exposed.er.core.renderer.MermaidRenderer
import io.github.massimodeiana.exposed.er.reflection.SchemaExtractor
import io.github.massimodeiana.exposed.er.reflection.TableScanner
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URLClassLoader

abstract class GenerateErDiagramTask : DefaultTask() {

    @get:Input
    abstract val packages: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val classLoader = buildClassLoader()
        val tables = TableScanner().scan(packages.get(), classLoader)

        if (tables.isEmpty()) {
            logger.warn("No Exposed Table objects found in packages: ${packages.get()}")
            return
        }

        val schema = SchemaExtractor().extract(tables)
        val mermaid = MermaidRenderer().render(schema)

        val output = outputFile.asFile.get()
        output.parentFile?.mkdirs()
        output.writeText(mermaid)

        logger.lifecycle("ER diagram written to ${output.absolutePath} (${tables.size} tables)")
    }

    private fun buildClassLoader(): ClassLoader {
        val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
        val compiledClasses = project.layout.buildDirectory.dir("classes").get().asFile

        val urls = runtimeClasspath.files.map { it.toURI().toURL() } +
            compiledClasses.walkTopDown()
                .filter { it.isDirectory && it.name == "main" }
                .map { it.toURI().toURL() }
                .toList()

        return URLClassLoader(urls.toTypedArray(), javaClass.classLoader)
    }
}
