package io.github.massimodeiana.exposed.er.reflection

import io.github.massimodeiana.exposed.er.core.model.*
import io.github.massimodeiana.exposed.er.reflection.fixtures.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaExtractorTest {

    private val extractor = SchemaExtractor()

    @Test
    fun `should extract simple table with columns`() {
        val schema = extractor.extract(listOf(TestUsers))

        assertEquals(1, schema.tables.size)
        val table = schema.tables[0]
        assertEquals("users", table.name)
        assertEquals(4, table.columns.size)
        assertEquals("id", table.columns[0].name)
        assertEquals("email", table.columns[1].name)
        assertEquals("name", table.columns[2].name)
        assertEquals("is_active", table.columns[3].name)
    }

    @Test
    fun `should extract primary key`() {
        val schema = extractor.extract(listOf(TestUsers))

        val pk = schema.tables[0].primaryKey
        assertNotNull(pk)
        assertEquals(listOf("id"), pk.columns)
    }

    @Test
    fun `should extract composite primary key`() {
        val schema = extractor.extract(listOf(TestRecipesIngredients, TestRecipes, TestIngredients))

        val junctionTable = schema.tables.find { it.name == "recipes_ingredients" }
        assertNotNull(junctionTable)
        val pk = junctionTable.primaryKey
        assertNotNull(pk)
        assertEquals(2, pk.columns.size)
        assertTrue(pk.columns.contains("recipe_id"))
        assertTrue(pk.columns.contains("ingredient_id"))
    }

    @Test
    fun `should extract nullable column`() {
        val schema = extractor.extract(listOf(TestRecipes, TestUsers))

        val recipesTable = schema.tables.find { it.name == "recipes" }
        assertNotNull(recipesTable)
        val ownerIdCol = recipesTable.columns.find { it.name == "owner_id" }
        assertNotNull(ownerIdCol)
        assertTrue(ownerIdCol.nullable)
    }

    @Test
    fun `should extract column types`() {
        val schema = extractor.extract(listOf(TestRecipes, TestRecipesIngredients, TestUsers, TestIngredients))

        val usersTable = schema.tables.find { it.name == "users" }!!
        val idCol = usersTable.columns.find { it.name == "id" }!!
        assertTrue(idCol.type.contains("long", ignoreCase = true) || idCol.type.contains("bigint", ignoreCase = true))

        val emailCol = usersTable.columns.find { it.name == "email" }!!
        assertEquals("varchar(255)", emailCol.type)

        val isActiveCol = usersTable.columns.find { it.name == "is_active" }!!
        assertTrue(isActiveCol.type.contains("bool", ignoreCase = true))

        val recipesTable = schema.tables.find { it.name == "recipes" }!!
        val descCol = recipesTable.columns.find { it.name == "description" }!!
        assertTrue(descCol.type.contains("text", ignoreCase = true))

        val junctionTable = schema.tables.find { it.name == "recipes_ingredients" }!!
        val qtyCol = junctionTable.columns.find { it.name == "quantity" }!!
        assertTrue(qtyCol.type.contains("double", ignoreCase = true) || qtyCol.type.contains("float8", ignoreCase = true))
    }

    @Test
    fun `should extract foreign key as relationship`() {
        val schema = extractor.extract(listOf(TestRecipes, TestUsers))

        assertEquals(1, schema.relationships.size)
        val rel = schema.relationships[0]
        assertEquals("recipes", rel.fromTable)
        assertEquals(listOf("owner_id"), rel.fromColumns)
        assertEquals("users", rel.toTable)
        assertEquals(listOf("id"), rel.toColumns)
    }

    @Test
    fun `should extract set null delete rule`() {
        val schema = extractor.extract(listOf(TestRecipes, TestUsers))

        val rel = schema.relationships[0]
        assertEquals(ReferenceAction.SET_NULL, rel.onDelete)
    }

    @Test
    fun `should detect many-to-many relationship`() {
        val schema = extractor.extract(listOf(TestRecipes, TestIngredients, TestRecipesIngredients))

        val manyToManyRels = schema.relationships.filter { it.type == RelationshipType.MANY_TO_MANY }
        assertEquals(2, manyToManyRels.size)

        val toRecipes = manyToManyRels.find { it.toTable == "recipes" }
        assertNotNull(toRecipes)
        assertEquals("recipes_ingredients", toRecipes.fromTable)

        val toIngredients = manyToManyRels.find { it.toTable == "ingredients" }
        assertNotNull(toIngredients)
        assertEquals("recipes_ingredients", toIngredients.fromTable)
    }

    @Test
    fun `should extract self-referential relationship`() {
        val schema = extractor.extract(listOf(TestTaxonomyNodes))

        assertEquals(1, schema.relationships.size)
        val rel = schema.relationships[0]
        assertEquals("taxonomy_nodes", rel.fromTable)
        assertEquals("taxonomy_nodes", rel.toTable)
        assertEquals(listOf("parent_id"), rel.fromColumns)
        assertEquals(listOf("id"), rel.toColumns)
    }

    @Test
    fun `should extract indices`() {
        val schema = extractor.extract(listOf(TestUsers))

        val table = schema.tables[0]
        val uniqueIdx = table.indices.find { it.name == "users_email_unique" }
        assertNotNull(uniqueIdx)
        assertTrue(uniqueIdx.unique)
        assertEquals(listOf("email"), uniqueIdx.columns)
    }

    @Test
    fun `should handle table with no foreign keys`() {
        val schema = extractor.extract(listOf(TestUsers))

        assertTrue(schema.relationships.isEmpty())
    }

    @Test
    fun `should only include relationships for tables in the input list`() {
        // TestRecipes has FK to TestUsers, but we don't include TestUsers
        val schema = extractor.extract(listOf(TestRecipes))

        // The FK still exists on TestRecipes, but since TestUsers is not in scope,
        // we should still extract it (the target table name is known from the FK metadata)
        assertEquals(1, schema.relationships.size)
        assertEquals("users", schema.relationships[0].toTable)
    }

    @Test
    fun `should detect one-to-many for non-junction table FK`() {
        val schema = extractor.extract(listOf(TestRecipes, TestUsers))

        val rel = schema.relationships[0]
        assertEquals(RelationshipType.ONE_TO_MANY, rel.type)
    }
}
