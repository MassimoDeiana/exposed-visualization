package io.github.massimodeiana.exposed.er.reflection

import io.github.massimodeiana.exposed.er.core.model.*
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

class SchemaExtractor {

    fun extract(tables: List<Table>): SchemaInfo {
        val tableInfos = tables.map { extractTable(it) }
        val relationships = tables.flatMap { extractRelationships(it) }
        return SchemaInfo(tableInfos, relationships)
    }

    private fun extractTable(table: Table): TableInfo {
        val columns = table.columns.map { extractColumn(it) }
        val primaryKey = extractPrimaryKey(table)
        val indices = extractIndices(table)
        return TableInfo(table.tableName, columns, primaryKey, indices)
    }

    private fun extractColumn(column: Column<*>): ColumnInfo {
        val type = formatColumnType(column)
        val nullable = column.columnType.nullable
        return ColumnInfo(column.name, type, nullable, defaultValue = null)
    }

    private fun formatColumnType(column: Column<*>): String {
        val columnType = column.columnType
        val typeName = columnType::class.simpleName ?: return "unknown"

        return when {
            typeName.contains("AutoIncColumnType") -> {
                val delegateType = extractAutoIncDelegate(columnType)
                if (delegateType != null) formatTypeName(delegateType, null) else "long"
            }
            else -> formatTypeName(typeName, columnType)
        }
    }

    private fun extractAutoIncDelegate(columnType: Any): String? {
        return try {
            val field = columnType::class.java.declaredFields.find { it.name == "_autoincSeq" || it.name == "delegate" }
                ?: columnType::class.java.superclass?.declaredFields?.find { it.name == "delegate" }
            if (field != null) {
                field.isAccessible = true
                val delegate = field.get(columnType)
                delegate?.let { it::class.simpleName }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTypeName(typeName: String, columnType: Any?): String = when {
        typeName.contains("LongColumnType") -> "long"
        typeName.contains("IntegerColumnType") -> "integer"
        typeName.contains("VarCharColumnType") -> if (columnType != null) extractVarcharLength(columnType) else "varchar"
        typeName.contains("TextColumnType") -> "text"
        typeName.contains("BooleanColumnType") -> "bool"
        typeName.contains("DoubleColumnType") -> "double"
        typeName.contains("FloatColumnType") -> "float"
        typeName.contains("DecimalColumnType") -> "decimal"
        typeName.contains("DateTimeColumnType") || typeName.contains("JavaLocalDateTimeColumnType") || typeName.contains("KotlinLocalDateTimeColumnType") -> "datetime"
        typeName.contains("DateColumnType") || typeName.contains("JavaLocalDateColumnType") || typeName.contains("KotlinLocalDateColumnType") -> "date"
        typeName.contains("UUIDColumnType") -> "uuid"
        else -> typeName.removeSuffix("ColumnType").lowercase()
    }

    private fun extractVarcharLength(columnType: Any): String {
        return try {
            val field = columnType::class.java.declaredFields.find { it.name == "colLength" }
            if (field != null) {
                field.isAccessible = true
                val length = field.get(columnType)
                "varchar($length)"
            } else "varchar"
        } catch (_: Exception) {
            "varchar"
        }
    }

    private fun extractPrimaryKey(table: Table): PrimaryKeyInfo? {
        val pk = table.primaryKey ?: return null
        val columns = pk.columns.map { it.name }
        return PrimaryKeyInfo(columns, pk.name)
    }

    private fun extractIndices(table: Table): List<IndexInfo> =
        table.indices.map { index ->
            IndexInfo(
                name = index.customName,
                columns = index.columns.map { it.name },
                unique = index.unique
            )
        }

    private fun extractRelationships(table: Table): List<RelationshipInfo> {
        val isJunction = isJunctionTable(table)
        return table.foreignKeys.map { fk ->
            val type = if (isJunction) RelationshipType.MANY_TO_MANY else RelationshipType.ONE_TO_MANY
            RelationshipInfo(
                fromTable = fk.fromTable.tableName,
                fromColumns = fk.from.map { it.name },
                toTable = fk.targetTable.tableName,
                toColumns = fk.target.map { it.name },
                type = type,
                onDelete = fk.deleteRule?.let { mapReferenceOption(it) },
                onUpdate = fk.updateRule?.let { mapReferenceOption(it) }
            )
        }
    }

    private fun isJunctionTable(table: Table): Boolean {
        val pk = table.primaryKey ?: return false
        val pkColumnNames = pk.columns.map { it.name }.toSet()
        if (pkColumnNames.size < 2) return false

        val fkSourceColumns = table.foreignKeys.flatMap { it.from.map { col -> col.name } }.toSet()
        return pkColumnNames == fkSourceColumns
    }

    private fun mapReferenceOption(option: ReferenceOption): ReferenceAction = when (option) {
        ReferenceOption.CASCADE -> ReferenceAction.CASCADE
        ReferenceOption.SET_NULL -> ReferenceAction.SET_NULL
        ReferenceOption.RESTRICT -> ReferenceAction.RESTRICT
        ReferenceOption.NO_ACTION -> ReferenceAction.NO_ACTION
        ReferenceOption.SET_DEFAULT -> ReferenceAction.SET_DEFAULT
    }
}
