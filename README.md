# Exposed Visualization

Generate ER diagrams from [Kotlin Exposed](https://github.com/JetBrains/Exposed) table definitions — no database connection required.

Works by analyzing your `Table` objects directly from compiled classes or source code and producing [Mermaid](https://mermaid.js.org/) ER diagrams.

## Tools

| Module | Description |
|--------|-------------|
| **Maven Plugin** | Run `mvn exposed-er:generate` to produce an ER diagram |
| **Gradle Plugin** | Run `./gradlew generateErDiagram` to produce an ER diagram |
| **IntelliJ Plugin** | Live ER diagram in a tool window, updates as you edit |

## Maven Plugin

Add to your `pom.xml`:

```xml
<plugin>
    <groupId>io.github.massimodeiana</groupId>
    <artifactId>exposed-er-maven-plugin</artifactId>
    <version>0.1.0</version>
    <configuration>
        <packages>
            <package>com.example.entity</package>
        </packages>
    </configuration>
</plugin>
```

Then run:

```bash
mvn exposed-er:generate
```

Output: `target/er-diagram.mmd`

## Gradle Plugin

Add to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.massimodeiana.exposed-er") version "0.1.0"
}

exposedErDiagram {
    packages.set(listOf("com.example.entity"))
    outputFile.set(layout.buildDirectory.file("er-diagram.mmd"))
}
```

Then run:

```bash
./gradlew generateErDiagram
```

## IntelliJ Plugin

Install from the [JetBrains Marketplace](https://plugins.jetbrains.com) (search "Exposed ER Diagram").

- Open any project with Exposed table definitions
- The **Exposed ER Diagram** tool window appears on the right
- Use the **Filter** button to select which tables to include
- Diagram auto-refreshes as you edit `.kt` files
- Zoom, pan, and fit-to-view controls
- Supports both light and dark themes

## Supported Patterns

- Single and composite primary keys
- Foreign key relationships (`.references()`)
- One-to-many and many-to-many (junction tables)
- Self-referential tables
- Nullable relationships
- Cascade/SET_NULL/RESTRICT reference actions
- Table indices
- `Table`, `IntIdTable`, `LongIdTable`, `UUIDTable`, `CompositeIdTable`

## Architecture

```
exposed-er-core          Data model (SchemaInfo) + Mermaid renderer
exposed-er-reflection    Extracts schema from compiled Table classes
exposed-er-maven-plugin  Maven plugin wrapper
exposed-er-gradle-plugin Gradle plugin wrapper
exposed-er-intellij      IntelliJ plugin (PSI-based, no compilation needed)
```

## Building from Source

```bash
# Maven modules (core, reflection, maven-plugin)
mvn install

# Gradle plugin
cd exposed-er-gradle-plugin && ./gradlew publishToMavenLocal

# IntelliJ plugin
cd exposed-er-intellij && ./gradlew buildPlugin
```

## License

[MIT](LICENSE)
