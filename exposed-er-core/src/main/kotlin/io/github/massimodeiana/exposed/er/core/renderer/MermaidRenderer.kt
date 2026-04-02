package io.github.massimodeiana.exposed.er.core.renderer

import io.github.massimodeiana.exposed.er.core.model.*

class MermaidRenderer {

    fun render(schema: SchemaInfo): String {
        val builder = StringBuilder()
        builder.appendLine("erDiagram")

        val fkColumns = collectForeignKeyColumns(schema.relationships)
        val sortedTables = schema.tables.sortedBy { it.name }

        for (table in sortedTables) {
            renderTable(builder, table, fkColumns)
        }

        val sortedRelationships = schema.relationships.sortedWith(
            compareBy({ it.fromTable }, { it.toTable })
        )
        for (relationship in sortedRelationships) {
            renderRelationship(builder, relationship, schema)
        }

        return builder.toString().trimEnd()
    }

    private fun collectForeignKeyColumns(relationships: List<RelationshipInfo>): Set<Pair<String, String>> =
        relationships.flatMap { rel ->
            rel.fromColumns.map { col -> rel.fromTable to col }
        }.toSet()

    private fun renderTable(
        builder: StringBuilder,
        table: TableInfo,
        fkColumns: Set<Pair<String, String>>
    ) {
        builder.appendLine("    ${table.name} {")
        val pkColumns = table.primaryKey?.columns?.toSet() ?: emptySet()

        for (column in table.columns) {
            val marker = columnMarker(table.name, column.name, pkColumns, fkColumns)
            builder.appendLine("        ${column.type} ${column.name}$marker")
        }

        builder.appendLine("    }")
    }

    private fun columnMarker(
        tableName: String,
        columnName: String,
        pkColumns: Set<String>,
        fkColumns: Set<Pair<String, String>>
    ): String {
        val isPk = columnName in pkColumns
        val isFk = (tableName to columnName) in fkColumns

        return when {
            isPk && isFk -> " PK,FK"
            isPk -> " PK"
            isFk -> " FK"
            else -> ""
        }
    }

    private fun renderRelationship(
        builder: StringBuilder,
        relationship: RelationshipInfo,
        schema: SchemaInfo
    ) {
        val cardinality = resolveCardinality(relationship, schema)
        val label = relationship.fromColumns.joinToString(", ")
        builder.appendLine("    ${relationship.toTable} ${cardinality} ${relationship.fromTable} : \"${label}\"")
    }

    private fun resolveCardinality(
        relationship: RelationshipInfo,
        schema: SchemaInfo
    ): String {
        val fromTable = schema.tables.find { it.name == relationship.fromTable }
        val isNullable = fromTable?.columns
            ?.filter { it.name in relationship.fromColumns }
            ?.any { it.nullable }
            ?: false

        return when (relationship.type) {
            RelationshipType.ONE_TO_ONE -> if (isNullable) "||--o|" else "||--||"
            RelationshipType.ONE_TO_MANY -> if (isNullable) "||--o{" else "||--|{"
            RelationshipType.MANY_TO_MANY -> "}o--o{"
        }
    }
}
