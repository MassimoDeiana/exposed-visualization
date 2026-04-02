package io.github.massimodeiana.exposed.er.core.renderer

import io.github.massimodeiana.exposed.er.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MermaidRendererTest {

    private val renderer = MermaidRenderer()

    @Test
    fun `should render single table with columns`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "email", type = "varchar(255)", nullable = false, defaultValue = null),
                        ColumnInfo(name = "name", type = "varchar(255)", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
                    indices = emptyList()
                )
            ),
            relationships = emptyList()
        )

        val result = renderer.render(schema)

        val expected = """
            erDiagram
                users {
                    long id PK
                    varchar(255) email
                    varchar(255) name
                }
        """.trimIndent()
        assertEquals(expected, result)
    }

    @Test
    fun `should mark primary key columns with PK`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "recipes_ingredients",
                    columns = listOf(
                        ColumnInfo(name = "recipe_id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "ingredient_id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "quantity", type = "double", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(
                        columns = listOf("recipe_id", "ingredient_id"),
                        name = "recipes_ingredients_pkey"
                    ),
                    indices = emptyList()
                )
            ),
            relationships = emptyList()
        )

        val result = renderer.render(schema)

        assertTrue(result.contains("long recipe_id PK"))
        assertTrue(result.contains("long ingredient_id PK"))
        assertTrue(result.contains("double quantity") && !result.contains("double quantity PK"))
    }

    @Test
    fun `should mark foreign key columns with FK`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "recipes",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "owner_id", type = "long", nullable = true, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "recipes_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
                    indices = emptyList()
                )
            ),
            relationships = listOf(
                RelationshipInfo(
                    fromTable = "recipes",
                    fromColumns = listOf("owner_id"),
                    toTable = "users",
                    toColumns = listOf("id"),
                    type = RelationshipType.ONE_TO_MANY,
                    onDelete = ReferenceAction.SET_NULL,
                    onUpdate = null
                )
            )
        )

        val result = renderer.render(schema)

        assertTrue(result.contains("long owner_id FK"))
    }

    @Test
    fun `should render one-to-many relationship`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "recipes",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "owner_id", type = "long", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "recipes_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
                    indices = emptyList()
                )
            ),
            relationships = listOf(
                RelationshipInfo(
                    fromTable = "recipes",
                    fromColumns = listOf("owner_id"),
                    toTable = "users",
                    toColumns = listOf("id"),
                    type = RelationshipType.ONE_TO_MANY,
                    onDelete = null,
                    onUpdate = null
                )
            )
        )

        val result = renderer.render(schema)

        assertTrue(result.contains("users ||--|{ recipes"))
    }

    @Test
    fun `should render optional one-to-many relationship for nullable FK`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "recipes",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "owner_id", type = "long", nullable = true, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "recipes_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
                    indices = emptyList()
                )
            ),
            relationships = listOf(
                RelationshipInfo(
                    fromTable = "recipes",
                    fromColumns = listOf("owner_id"),
                    toTable = "users",
                    toColumns = listOf("id"),
                    type = RelationshipType.ONE_TO_MANY,
                    onDelete = ReferenceAction.SET_NULL,
                    onUpdate = null
                )
            )
        )

        val result = renderer.render(schema)

        assertTrue(result.contains("users ||--o{ recipes"))
    }

    @Test
    fun `should render many-to-many relationship`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "ingredients",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "ingredients_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "recipes",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "recipes_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "recipes_ingredients",
                    columns = listOf(
                        ColumnInfo(name = "recipe_id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "ingredient_id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "quantity", type = "double", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(
                        columns = listOf("recipe_id", "ingredient_id"),
                        name = "recipes_ingredients_pkey"
                    ),
                    indices = emptyList()
                )
            ),
            relationships = listOf(
                RelationshipInfo(
                    fromTable = "recipes_ingredients",
                    fromColumns = listOf("recipe_id"),
                    toTable = "recipes",
                    toColumns = listOf("id"),
                    type = RelationshipType.MANY_TO_MANY,
                    onDelete = null,
                    onUpdate = null
                ),
                RelationshipInfo(
                    fromTable = "recipes_ingredients",
                    fromColumns = listOf("ingredient_id"),
                    toTable = "ingredients",
                    toColumns = listOf("id"),
                    type = RelationshipType.MANY_TO_MANY,
                    onDelete = null,
                    onUpdate = null
                )
            )
        )

        val result = renderer.render(schema)

        assertTrue(result.contains("recipes }o--o{ recipes_ingredients"))
        assertTrue(result.contains("ingredients }o--o{ recipes_ingredients"))
    }

    @Test
    fun `should render self-referential relationship`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "taxonomy_nodes",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "parent_id", type = "long", nullable = true, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "taxonomy_nodes_pkey"),
                    indices = emptyList()
                )
            ),
            relationships = listOf(
                RelationshipInfo(
                    fromTable = "taxonomy_nodes",
                    fromColumns = listOf("parent_id"),
                    toTable = "taxonomy_nodes",
                    toColumns = listOf("id"),
                    type = RelationshipType.ONE_TO_MANY,
                    onDelete = null,
                    onUpdate = null
                )
            )
        )

        val result = renderer.render(schema)

        assertTrue(result.contains("taxonomy_nodes ||--o{ taxonomy_nodes"))
    }

    @Test
    fun `should render complete schema with multiple tables and relationships`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "email", type = "varchar(255)", nullable = false, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "recipes",
                    columns = listOf(
                        ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                        ColumnInfo(name = "name", type = "varchar(255)", nullable = false, defaultValue = null),
                        ColumnInfo(name = "owner_id", type = "long", nullable = true, defaultValue = null)
                    ),
                    primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "recipes_pkey"),
                    indices = emptyList()
                )
            ),
            relationships = listOf(
                RelationshipInfo(
                    fromTable = "recipes",
                    fromColumns = listOf("owner_id"),
                    toTable = "users",
                    toColumns = listOf("id"),
                    type = RelationshipType.ONE_TO_MANY,
                    onDelete = ReferenceAction.SET_NULL,
                    onUpdate = null
                )
            )
        )

        val result = renderer.render(schema)

        assertTrue(result.startsWith("erDiagram"))
        assertTrue(result.contains("recipes {"))
        assertTrue(result.contains("users {"))
        assertTrue(result.contains("users ||--o{ recipes"))
    }

    @Test
    fun `should produce deterministic output`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "zebra",
                    columns = listOf(ColumnInfo("id", "long", false, null)),
                    primaryKey = PrimaryKeyInfo(listOf("id"), null),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "alpha",
                    columns = listOf(ColumnInfo("id", "long", false, null)),
                    primaryKey = PrimaryKeyInfo(listOf("id"), null),
                    indices = emptyList()
                )
            ),
            relationships = emptyList()
        )

        val result1 = renderer.render(schema)
        val result2 = renderer.render(schema)

        assertEquals(result1, result2)
        val alphaIndex = result1.indexOf("alpha {")
        val zebraIndex = result1.indexOf("zebra {")
        assertTrue(alphaIndex < zebraIndex, "Tables should be sorted alphabetically")
    }

    @Test
    fun `should handle table with no columns gracefully`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "empty_table",
                    columns = emptyList(),
                    primaryKey = null,
                    indices = emptyList()
                )
            ),
            relationships = emptyList()
        )

        val result = renderer.render(schema)

        val expected = """
            erDiagram
                empty_table {
                }
        """.trimIndent()
        assertEquals(expected, result)
    }
}
