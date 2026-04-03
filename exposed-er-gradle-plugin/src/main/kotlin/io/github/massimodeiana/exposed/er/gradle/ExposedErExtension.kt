package io.github.massimodeiana.exposed.er.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty

abstract class ExposedErExtension {
    abstract val packages: ListProperty<String>
    abstract val outputFile: RegularFileProperty
}
