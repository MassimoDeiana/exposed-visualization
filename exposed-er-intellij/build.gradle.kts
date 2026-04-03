import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "io.github.massimodeiana"
version = "0.1.0"

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
        intellijIdea("2025.3.4")
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

        description = """
            <p>Generate ER diagrams from <a href="https://github.com/JetBrains/Exposed">Kotlin Exposed</a> table definitions — no database connection required.</p>
            <ul>
                <li>Live ER diagram in a tool window, updates as you edit</li>
                <li>Filter tables by package with a checkbox tree popup</li>
                <li>Zoom, pan, and fit-to-view controls</li>
                <li>Supports light and dark themes</li>
                <li>Handles all relationship types: one-to-many, many-to-many, self-referential</li>
                <li>Works with Table, IntIdTable, LongIdTable, UUIDTable, CompositeIdTable</li>
            </ul>
        """.trimIndent()

        changeNotes = """
            <p><b>0.1.0</b> — Initial release</p>
            <ul>
                <li>PSI-based table analysis (no compilation needed)</li>
                <li>Mermaid ER diagram rendering via JCEF</li>
                <li>Package-based table filtering with CheckboxTree popup</li>
                <li>Zoom, pan, fit-to-view controls</li>
                <li>Auto-refresh with debounce on file changes</li>
                <li>Light and dark theme support</li>
            </ul>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "253"
        }
    }
    buildSearchableOptions = false

    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
    }
}

kotlin {
    jvmToolchain(21)
}
