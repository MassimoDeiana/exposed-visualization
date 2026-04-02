package io.github.massimodeiana.exposed.er.reflection

import io.github.massimodeiana.exposed.er.core.renderer.MermaidRenderer
import kotlin.test.Test
import kotlin.test.assertTrue

class EndToEndIntegrationTest {

    @Test
    fun `should scan extract and render full mermaid diagram`() {
        val scanner = TableScanner()
        val extractor = SchemaExtractor()
        val renderer = MermaidRenderer()

        val tables = scanner.scan(listOf("io.github.massimodeiana.exposed.er.reflection.fixtures"))
        val schema = extractor.extract(tables)
        val mermaid = renderer.render(schema)

        assertTrue(mermaid.startsWith("erDiagram"))

        // All tables present
        assertTrue(mermaid.contains("users {"))
        assertTrue(mermaid.contains("recipes {"))
        assertTrue(mermaid.contains("ingredients {"))
        assertTrue(mermaid.contains("recipes_ingredients {"))
        assertTrue(mermaid.contains("taxonomy_nodes {"))
        assertTrue(mermaid.contains("categories {"))

        // PK markers
        assertTrue(mermaid.contains("long id PK"))

        // FK markers on junction table
        assertTrue(mermaid.contains("PK,FK"))

        // Relationships
        assertTrue(mermaid.contains("users") && mermaid.contains("recipes"))
        assertTrue(mermaid.contains("recipes_ingredients"))
        assertTrue(mermaid.contains("taxonomy_nodes"))

        // Self-referential
        val selfRefPattern = Regex("""taxonomy_nodes.*taxonomy_nodes""")
        assertTrue(selfRefPattern.containsMatchIn(mermaid))

        // Many-to-many cardinality
        assertTrue(mermaid.contains("}o--o{"))

        // Optional one-to-many (nullable FK)
        assertTrue(mermaid.contains("||--o{"))

        println("--- Generated Mermaid Diagram ---")
        println(mermaid)
        println("--- End ---")
    }
}
