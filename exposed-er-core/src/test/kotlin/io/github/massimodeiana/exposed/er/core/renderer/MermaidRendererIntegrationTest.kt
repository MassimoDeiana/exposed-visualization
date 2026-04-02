package io.github.massimodeiana.exposed.er.core.renderer

import io.github.massimodeiana.exposed.er.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MermaidRendererIntegrationTest {

    private val renderer = MermaidRenderer()

    @Test
    fun `should render gusto-like schema with users, recipes, ingredients, and junction table`() {
        val schema = SchemaInfo(
            tables = listOf(
                TableInfo(
                    name = "users",
                    columns = listOf(
                        ColumnInfo("id", "long", false, null),
                        ColumnInfo("email", "varchar(255)", false, null),
                        ColumnInfo("name", "varchar(255)", false, null),
                        ColumnInfo("avatar_url", "varchar(1024)", true, null),
                        ColumnInfo("preferred_language", "varchar(10)", false, "en")
                    ),
                    primaryKey = PrimaryKeyInfo(listOf("id"), "users_pkey"),
                    indices = listOf(
                        IndexInfo("users_email_unique", listOf("email"), true)
                    )
                ),
                TableInfo(
                    name = "recipes",
                    columns = listOf(
                        ColumnInfo("id", "long", false, null),
                        ColumnInfo("name", "varchar(255)", false, null),
                        ColumnInfo("description", "text", true, null),
                        ColumnInfo("difficulty", "varchar(50)", true, null),
                        ColumnInfo("owner_id", "long", true, null),
                        ColumnInfo("visibility", "varchar(20)", false, "PRIVATE")
                    ),
                    primaryKey = PrimaryKeyInfo(listOf("id"), "recipes_pkey"),
                    indices = listOf(
                        IndexInfo("idx_recipes_visibility", listOf("visibility"), false),
                        IndexInfo("idx_recipes_owner_id", listOf("owner_id"), false)
                    )
                ),
                TableInfo(
                    name = "ingredients",
                    columns = listOf(
                        ColumnInfo("id", "long", false, null),
                        ColumnInfo("name", "varchar(255)", false, null),
                        ColumnInfo("unit_of_measure_id", "long", false, null)
                    ),
                    primaryKey = PrimaryKeyInfo(listOf("id"), "ingredients_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "units_of_measure",
                    columns = listOf(
                        ColumnInfo("id", "long", false, null),
                        ColumnInfo("name", "varchar(255)", false, null),
                        ColumnInfo("symbol", "varchar(10)", true, null)
                    ),
                    primaryKey = PrimaryKeyInfo(listOf("id"), "units_of_measure_pkey"),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "recipes_ingredients",
                    columns = listOf(
                        ColumnInfo("recipe_id", "long", false, null),
                        ColumnInfo("ingredient_id", "long", false, null),
                        ColumnInfo("quantity", "double", false, null)
                    ),
                    primaryKey = PrimaryKeyInfo(
                        listOf("recipe_id", "ingredient_id"),
                        "recipes_ingredients_pkey"
                    ),
                    indices = emptyList()
                ),
                TableInfo(
                    name = "taxonomy_nodes",
                    columns = listOf(
                        ColumnInfo("id", "long", false, null),
                        ColumnInfo("taxonomy_key", "varchar(255)", false, null),
                        ColumnInfo("parent_id", "long", true, null),
                        ColumnInfo("path", "varchar(2048)", false, null),
                        ColumnInfo("node_type", "varchar(50)", false, null),
                        ColumnInfo("depth", "integer", false, "0")
                    ),
                    primaryKey = PrimaryKeyInfo(listOf("id"), "taxonomy_nodes_pkey"),
                    indices = listOf(
                        IndexInfo("idx_taxonomy_nodes_key_type", listOf("taxonomy_key", "node_type"), true)
                    )
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
                ),
                RelationshipInfo(
                    fromTable = "ingredients",
                    fromColumns = listOf("unit_of_measure_id"),
                    toTable = "units_of_measure",
                    toColumns = listOf("id"),
                    type = RelationshipType.ONE_TO_MANY,
                    onDelete = null,
                    onUpdate = null
                ),
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
                ),
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

        val expected = """
            erDiagram
                ingredients {
                    long id PK
                    varchar(255) name
                    long unit_of_measure_id FK
                }
                recipes {
                    long id PK
                    varchar(255) name
                    text description
                    varchar(50) difficulty
                    long owner_id FK
                    varchar(20) visibility
                }
                recipes_ingredients {
                    long recipe_id PK,FK
                    long ingredient_id PK,FK
                    double quantity
                }
                taxonomy_nodes {
                    long id PK
                    varchar(255) taxonomy_key
                    long parent_id FK
                    varchar(2048) path
                    varchar(50) node_type
                    integer depth
                }
                units_of_measure {
                    long id PK
                    varchar(255) name
                    varchar(10) symbol
                }
                users {
                    long id PK
                    varchar(255) email
                    varchar(255) name
                    varchar(1024) avatar_url
                    varchar(10) preferred_language
                }
                units_of_measure ||--|{ ingredients : "unit_of_measure_id"
                users ||--o{ recipes : "owner_id"
                ingredients }o--o{ recipes_ingredients : "ingredient_id"
                recipes }o--o{ recipes_ingredients : "recipe_id"
                taxonomy_nodes ||--o{ taxonomy_nodes : "parent_id"
        """.trimIndent()
        assertEquals(expected, result)
    }
}
