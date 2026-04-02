# Exposed Visualization

A set of tools to generate ER diagrams from Kotlin Exposed table definitions.

## Project Structure

```
exposed-visualization/
├── exposed-er-core/           # Shared data model + diagram rendering (Mermaid, PlantUML, etc.)
├── exposed-er-reflection/     # Extracts table metadata from compiled classes via JVM reflection
├── exposed-er-maven-plugin/   # Maven plugin wrapper around reflection
├── exposed-er-gradle-plugin/  # Gradle plugin wrapper around reflection
├── exposed-er-intellij/       # IntelliJ plugin using PSI for live diagram generation
```

## Development Principles

### TDD (Test-Driven Development)
- **Red → Green → Refactor.** Always write a failing test first, then write the minimum code to make it pass, then refactor.
- No production code without a corresponding test.
- Tests are first-class citizens — they should be as clean as the production code.
- Run tests frequently. Every change should be validated by the test suite.

### Clean Code (Kent Beck, Robert C. Martin)
- **Simple Design (Kent Beck's 4 Rules of Simple Design, in priority order):**
  1. Passes all the tests
  2. Reveals intention (clear, expressive code)
  3. No duplication (DRY)
  4. Fewest elements (no unnecessary abstractions)
- **Small functions.** Each function does one thing, does it well, does it only.
- **Meaningful names.** Names should reveal intent. No abbreviations, no cryptic names.
- **No comments to explain bad code.** Rewrite the code instead. Comments are acceptable only for *why*, never for *what*.
- **Boy Scout Rule.** Leave the code cleaner than you found it.
- **YAGNI.** Don't build it until you need it. No speculative abstractions.
- **Single Responsibility Principle.** Each class/module has one reason to change.
- **Composition over inheritance.**
- **Dependency Inversion.** Depend on abstractions, not concretions.
- **Command-Query Separation.** Functions either do something or answer something, not both.

### Testing Best Practices
- Tests should be **FIRST**: Fast, Independent, Repeatable, Self-validating, Timely.
- One logical assertion per test (multiple asserts are fine if they test one concept).
- Test names describe the behavior, not the implementation: `should extract foreign key relationships between tables`.
- Arrange-Act-Assert structure.
- No test interdependencies — each test sets up its own state.
- Test at the right level: unit tests for logic, integration tests for wiring.

### Kotlin Conventions
- Idiomatic Kotlin: data classes, sealed classes, extension functions, null safety.
- Prefer immutable data (`val`, immutable collections).
- Use Kotlin's type system to make illegal states unrepresentable.

## Build & Test

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Run a specific module's tests
mvn test -pl exposed-er-core
```

## Output Formats
- Mermaid (`.mmd`) — renders in GitHub, IntelliJ, VS Code
- PlantUML (`.puml`) — widely supported
- DOT/Graphviz (`.dot`) — for custom rendering pipelines
