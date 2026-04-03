package io.github.massimodeiana.exposed.er.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class ExposedErPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("exposedErDiagram", ExposedErExtension::class.java)

        extension.outputFile.convention(project.layout.buildDirectory.file("er-diagram.mmd"))

        project.tasks.register("generateErDiagram", GenerateErDiagramTask::class.java) { task ->
            task.group = "documentation"
            task.description = "Generate an ER diagram from Exposed table definitions"
            task.packages.set(extension.packages)
            task.outputFile.set(extension.outputFile)
            task.dependsOn("classes")
        }
    }
}
