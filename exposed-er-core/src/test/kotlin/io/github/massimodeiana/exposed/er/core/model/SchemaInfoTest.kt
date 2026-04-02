package io.github.massimodeiana.exposed.er.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaInfoTest {

    @Test
    fun `should create empty schema`() {
        val schema = SchemaInfo(tables = emptyList(), relationships = emptyList())

        assertTrue(schema.tables.isEmpty())
        assertTrue(schema.relationships.isEmpty())
    }

    @Test
    fun `should create schema with tables and relationships`() {
        val usersTable = TableInfo(
            name = "users",
            columns = listOf(
                ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                ColumnInfo(name = "email", type = "varchar(255)", nullable = false, defaultValue = null)
            ),
            primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
            indices = emptyList()
        )
        val recipesTable = TableInfo(
            name = "recipes",
            columns = listOf(
                ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                ColumnInfo(name = "owner_id", type = "long", nullable = true, defaultValue = null)
            ),
            primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "recipes_pkey"),
            indices = emptyList()
        )
        val relationship = RelationshipInfo(
            fromTable = "recipes",
            fromColumns = listOf("owner_id"),
            toTable = "users",
            toColumns = listOf("id"),
            type = RelationshipType.ONE_TO_MANY,
            onDelete = ReferenceAction.SET_NULL,
            onUpdate = null
        )

        val schema = SchemaInfo(
            tables = listOf(usersTable, recipesTable),
            relationships = listOf(relationship)
        )

        assertEquals(2, schema.tables.size)
        assertEquals(1, schema.relationships.size)
        assertEquals("users", schema.tables[0].name)
        assertEquals("recipes", schema.tables[1].name)
    }
}

class TableInfoTest {

    @Test
    fun `should create table with no primary key`() {
        val table = TableInfo(
            name = "audit_log",
            columns = listOf(
                ColumnInfo(name = "message", type = "text", nullable = false, defaultValue = null)
            ),
            primaryKey = null,
            indices = emptyList()
        )

        assertNull(table.primaryKey)
        assertEquals(1, table.columns.size)
    }

    @Test
    fun `should create table with composite primary key`() {
        val table = TableInfo(
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

        assertEquals(2, table.primaryKey!!.columns.size)
        assertEquals("recipe_id", table.primaryKey!!.columns[0])
        assertEquals("ingredient_id", table.primaryKey!!.columns[1])
    }

    @Test
    fun `should create table with indices`() {
        val table = TableInfo(
            name = "users",
            columns = listOf(
                ColumnInfo(name = "id", type = "long", nullable = false, defaultValue = null),
                ColumnInfo(name = "email", type = "varchar(255)", nullable = false, defaultValue = null)
            ),
            primaryKey = PrimaryKeyInfo(columns = listOf("id"), name = "users_pkey"),
            indices = listOf(
                IndexInfo(name = "users_email_unique", columns = listOf("email"), unique = true)
            )
        )

        assertEquals(1, table.indices.size)
        assertTrue(table.indices[0].unique)
    }
}

class ColumnInfoTest {

    @Test
    fun `should create nullable column with default value`() {
        val column = ColumnInfo(
            name = "visibility",
            type = "varchar(20)",
            nullable = false,
            defaultValue = "PRIVATE"
        )

        assertEquals("PRIVATE", column.defaultValue)
        assertEquals(false, column.nullable)
    }

    @Test
    fun `should create nullable column without default`() {
        val column = ColumnInfo(
            name = "description",
            type = "text",
            nullable = true,
            defaultValue = null
        )

        assertTrue(column.nullable)
        assertNull(column.defaultValue)
    }
}

class RelationshipInfoTest {

    @Test
    fun `should create self-referential relationship`() {
        val relationship = RelationshipInfo(
            fromTable = "taxonomy_nodes",
            fromColumns = listOf("parent_id"),
            toTable = "taxonomy_nodes",
            toColumns = listOf("id"),
            type = RelationshipType.ONE_TO_MANY,
            onDelete = null,
            onUpdate = null
        )

        assertEquals(relationship.fromTable, relationship.toTable)
    }

    @Test
    fun `should create many-to-many relationship`() {
        val relationship = RelationshipInfo(
            fromTable = "recipes_ingredients",
            fromColumns = listOf("recipe_id"),
            toTable = "recipes",
            toColumns = listOf("id"),
            type = RelationshipType.MANY_TO_MANY,
            onDelete = null,
            onUpdate = null
        )

        assertEquals(RelationshipType.MANY_TO_MANY, relationship.type)
    }

    @Test
    fun `should create relationship with cascade actions`() {
        val relationship = RelationshipInfo(
            fromTable = "recipe_translations",
            fromColumns = listOf("recipe_id"),
            toTable = "recipes",
            toColumns = listOf("id"),
            type = RelationshipType.ONE_TO_MANY,
            onDelete = ReferenceAction.CASCADE,
            onUpdate = ReferenceAction.RESTRICT
        )

        assertEquals(ReferenceAction.CASCADE, relationship.onDelete)
        assertEquals(ReferenceAction.RESTRICT, relationship.onUpdate)
    }
}

class IndexInfoTest {

    @Test
    fun `should create composite index`() {
        val index = IndexInfo(
            name = "idx_taxonomy_nodes_key_type",
            columns = listOf("taxonomy_key", "node_type"),
            unique = true
        )

        assertEquals(2, index.columns.size)
        assertTrue(index.unique)
    }

    @Test
    fun `should create index with no name`() {
        val index = IndexInfo(name = null, columns = listOf("email"), unique = false)

        assertNull(index.name)
    }
}
