package io.github.massimodeiana.exposed.er.reflection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableScannerTest {

    private val scanner = TableScanner()

    @Test
    fun `should find table objects in specified package`() {
        val tables = scanner.scan(listOf("io.github.massimodeiana.exposed.er.reflection.fixtures"))

        val tableNames = tables.map { it.tableName }.toSet()
        assertTrue(tableNames.contains("users"))
        assertTrue(tableNames.contains("recipes"))
        assertTrue(tableNames.contains("ingredients"))
        assertTrue(tableNames.contains("recipes_ingredients"))
        assertTrue(tableNames.contains("taxonomy_nodes"))
    }

    @Test
    fun `should ignore non-table classes in same package`() {
        val tables = scanner.scan(listOf("io.github.massimodeiana.exposed.er.reflection.fixtures"))

        val classNames = tables.map { it::class.simpleName }
        assertTrue(classNames.none { it == "NotATable" })
    }

    @Test
    fun `should find tables in subpackages`() {
        val tables = scanner.scan(listOf("io.github.massimodeiana.exposed.er.reflection.fixtures"))

        val tableNames = tables.map { it.tableName }.toSet()
        assertTrue(tableNames.contains("categories"))
    }

    @Test
    fun `should return empty list for package with no tables`() {
        val tables = scanner.scan(listOf("io.github.massimodeiana.exposed.er.reflection.nonexistent"))

        assertTrue(tables.isEmpty())
    }

    @Test
    fun `should scan multiple packages`() {
        val tables = scanner.scan(
            listOf(
                "io.github.massimodeiana.exposed.er.reflection.fixtures",
                "io.github.massimodeiana.exposed.er.reflection.fixtures.subpackage"
            )
        )

        val tableNames = tables.map { it.tableName }.toSet()
        assertTrue(tableNames.contains("users"))
        assertTrue(tableNames.contains("categories"))
    }

    @Test
    fun `should not return duplicate tables when packages overlap`() {
        val tables = scanner.scan(
            listOf(
                "io.github.massimodeiana.exposed.er.reflection.fixtures",
                "io.github.massimodeiana.exposed.er.reflection.fixtures.subpackage"
            )
        )

        val tableNames = tables.map { it.tableName }
        assertEquals(tableNames.size, tableNames.toSet().size)
    }
}
