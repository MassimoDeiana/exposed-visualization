package io.github.massimodeiana.exposed.er.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import java.net.URLClassLoader

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
class GenerateDiagramMojo : AbstractMojo() {

    @Parameter(property = "exposed-er.packages", required = true)
    private lateinit var packages: List<String>

    @Parameter(
        property = "exposed-er.outputFile",
        defaultValue = "\${project.build.directory}/er-diagram.mmd"
    )
    private lateinit var outputFile: File

    @Parameter(defaultValue = "\${project}", readonly = true)
    private lateinit var project: MavenProject

    override fun execute() {
        val classLoader = buildClassLoader()
        val result = DiagramGenerator().generate(packages, outputFile, classLoader)

        if (result.tableCount == 0) {
            log.warn("No Exposed Table objects found in packages: $packages")
        } else {
            log.info("ER diagram written to ${outputFile.absolutePath} (${result.tableCount} tables)")
        }
    }

    private fun buildClassLoader(): ClassLoader {
        val urls = project.compileClasspathElements.map { File(it.toString()).toURI().toURL() }
        return URLClassLoader(urls.toTypedArray(), javaClass.classLoader)
    }
}
