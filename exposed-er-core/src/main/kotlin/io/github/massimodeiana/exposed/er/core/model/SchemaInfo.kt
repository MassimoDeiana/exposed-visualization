package io.github.massimodeiana.exposed.er.core.model

data class SchemaInfo(
    val tables: List<TableInfo>,
    val relationships: List<RelationshipInfo>
)

data class TableInfo(
    val name: String,
    val columns: List<ColumnInfo>,
    val primaryKey: PrimaryKeyInfo?,
    val indices: List<IndexInfo>
)

data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val defaultValue: String?
)

data class PrimaryKeyInfo(
    val columns: List<String>,
    val name: String?
)

data class RelationshipInfo(
    val fromTable: String,
    val fromColumns: List<String>,
    val toTable: String,
    val toColumns: List<String>,
    val type: RelationshipType,
    val onDelete: ReferenceAction?,
    val onUpdate: ReferenceAction?
)

enum class RelationshipType { ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY }

enum class ReferenceAction { CASCADE, SET_NULL, RESTRICT, NO_ACTION, SET_DEFAULT }

data class IndexInfo(
    val name: String?,
    val columns: List<String>,
    val unique: Boolean
)
