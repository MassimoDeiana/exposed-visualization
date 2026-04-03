package io.github.massimodeiana.exposed.er.intellij.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import io.github.massimodeiana.exposed.er.core.model.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*

class PsiSchemaExtractor {

    fun extract(project: Project): SchemaInfo {
        val tableObjects = findTableObjects(project)
        val tables = tableObjects.map { extractTable(it) }
        val relationships = tableObjects.flatMap { extractRelationships(it, tableObjects) }
        return SchemaInfo(tables, relationships)
    }

    fun extractFromFiles(files: List<KtFile>): SchemaInfo {
        val tableObjects = files.flatMap { findTableObjectsInFile(it) }
        val tables = tableObjects.map { extractTable(it) }
        val relationships = tableObjects.flatMap { extractRelationships(it, tableObjects) }
        return SchemaInfo(tables, relationships)
    }

    private fun findTableObjects(project: Project): List<KtObjectDeclaration> {
        val scope = GlobalSearchScope.projectScope(project)
        val ktFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
        return ktFiles.mapNotNull { vf ->
            com.intellij.psi.PsiManager.getInstance(project).findFile(vf) as? KtFile
        }.flatMap { findTableObjectsInFile(it) }
    }

    private fun findTableObjectsInFile(file: KtFile): List<KtObjectDeclaration> =
        file.declarations.filterIsInstance<KtObjectDeclaration>().filter { isTableObject(it) }

    private fun isTableObject(obj: KtObjectDeclaration): Boolean {
        val supertypes = obj.superTypeListEntries
        return supertypes.any { entry ->
            val typeText = entry.typeReference?.text ?: return@any false
            TABLE_TYPES.any { typeText.startsWith(it) }
        }
    }

    private fun extractTable(obj: KtObjectDeclaration): TableInfo {
        val tableName = extractTableName(obj)
        val columns = extractColumns(obj)
        val primaryKey = extractPrimaryKey(obj)
        val indices = extractIndices(obj)
        return TableInfo(tableName, columns, primaryKey, indices)
    }

    private fun extractTableName(obj: KtObjectDeclaration): String {
        val superTypeEntry = obj.superTypeListEntries.firstOrNull() ?: return obj.name ?: "unknown"
        val callExpression = superTypeEntry.children.filterIsInstance<KtValueArgumentList>().firstOrNull()
            ?: (superTypeEntry as? KtSuperTypeCallEntry)?.valueArgumentList
        val firstArg = callExpression?.arguments?.firstOrNull()
        val argText = firstArg?.getArgumentExpression()?.text
        return argText?.removeSurrounding("\"") ?: obj.name?.lowercase() ?: "unknown"
    }

    private fun extractColumns(obj: KtObjectDeclaration): List<ColumnInfo> {
        val body = obj.body ?: return emptyList()
        return body.properties
            .filter { it.name != "primaryKey" }
            .mapNotNull { extractColumn(it) }
    }

    private fun extractColumn(property: KtProperty): ColumnInfo? {
        val initializer = property.initializer ?: return null
        val calls = flattenCallChain(initializer)
        if (calls.isEmpty()) return null

        val rootCall = calls.first()
        val columnType = extractColumnType(rootCall)
        val columnName = extractColumnName(rootCall)
        if (columnName == null) return null

        val nullable = calls.any { getCallName(it) == "nullable" }

        return ColumnInfo(
            name = columnName,
            type = columnType,
            nullable = nullable,
            defaultValue = null
        )
    }

    private fun extractColumnType(call: KtExpression): String {
        val callName = getCallName(call) ?: return "unknown"
        return when (callName) {
            "long" -> "long"
            "integer" -> "integer"
            "varchar" -> {
                val args = getCallArguments(call)
                val length = args.getOrNull(1)?.text
                if (length != null) "varchar($length)" else "varchar"
            }
            "text" -> "text"
            "bool" -> "bool"
            "double" -> "double"
            "float" -> "float"
            "decimal" -> "decimal"
            "datetime" -> "datetime"
            "date" -> "date"
            "uuid" -> "uuid"
            else -> callName
        }
    }

    private fun extractColumnName(call: KtExpression): String? {
        val args = getCallArguments(call)
        val firstArg = args.firstOrNull()?.text ?: return null
        return firstArg.removeSurrounding("\"")
    }

    private fun extractPrimaryKey(obj: KtObjectDeclaration): PrimaryKeyInfo? {
        val body = obj.body ?: return null
        val pkProperty = body.properties.find { it.name == "primaryKey" } ?: return null
        val initializer = pkProperty.initializer ?: return null

        val args = getCallArguments(initializer)
        val columnNames = args
            .filter { !it.text.contains("name") }
            .map { resolvePropertyName(it.text) }

        if (columnNames.isEmpty()) return null
        return PrimaryKeyInfo(columnNames, name = null)
    }

    private fun extractIndices(obj: KtObjectDeclaration): List<IndexInfo> {
        val body = obj.body ?: return emptyList()
        val initBlocks = body.anonymousInitializers
        return initBlocks.flatMap { init ->
            val block = init.body as? KtBlockExpression ?: return@flatMap emptyList()
            block.statements.mapNotNull { extractIndex(it) }
        }
    }

    private fun extractIndex(statement: KtExpression): IndexInfo? {
        val callName = getCallName(statement) ?: return null
        if (callName != "index" && callName != "uniqueIndex") return null

        val args = getCallArguments(statement)
        val name = args.firstOrNull()?.text?.removeSurrounding("\"")
        val isUnique = callName == "uniqueIndex"

        val columnArgs = if (callName == "index") {
            args.drop(2)
        } else {
            args.drop(1)
        }
        val columns = columnArgs.map { resolvePropertyName(it.text) }

        return IndexInfo(name = name, columns = columns, unique = isUnique)
    }

    private fun extractRelationships(
        obj: KtObjectDeclaration,
        allObjects: List<KtObjectDeclaration>
    ): List<RelationshipInfo> {
        val body = obj.body ?: return emptyList()
        val tableName = extractTableName(obj)
        val isJunction = isJunctionTable(obj)

        return body.properties
            .filter { it.name != "primaryKey" }
            .mapNotNull { property ->
                val initializer = property.initializer ?: return@mapNotNull null
                val calls = flattenCallChain(initializer)
                val referencesCall = calls.find { getCallName(it) == "references" }
                    ?: return@mapNotNull null

                val args = getCallArguments(referencesCall)
                val targetRef = args.firstOrNull()?.text ?: return@mapNotNull null
                val parts = targetRef.split(".")
                val targetTableObjName = parts.firstOrNull() ?: return@mapNotNull null
                val targetColumnName = parts.getOrNull(1) ?: "id"

                val targetObj = allObjects.find { it.name == targetTableObjName }
                val targetTableName = if (targetObj != null) extractTableName(targetObj)
                    else if (targetTableObjName == obj.name) tableName
                    else targetTableObjName.lowercase()

                val onDelete = extractReferenceAction(args, "onDelete")
                val onUpdate = extractReferenceAction(args, "onUpdate")

                val type = if (isJunction) RelationshipType.MANY_TO_MANY else RelationshipType.ONE_TO_MANY

                RelationshipInfo(
                    fromTable = tableName,
                    fromColumns = listOf(property.name ?: "unknown"),
                    toTable = targetTableName,
                    toColumns = listOf(targetColumnName),
                    type = type,
                    onDelete = onDelete,
                    onUpdate = onUpdate
                )
            }
    }

    private fun isJunctionTable(obj: KtObjectDeclaration): Boolean {
        val body = obj.body ?: return false
        val pkProperty = body.properties.find { it.name == "primaryKey" } ?: return false
        val pkArgs = getCallArguments(pkProperty.initializer ?: return false)
            .filter { !it.text.contains("name") }
        if (pkArgs.size < 2) return false

        val pkColumnNames = pkArgs.map { resolvePropertyName(it.text) }.toSet()
        val fkColumnNames = body.properties
            .filter { it.name != "primaryKey" }
            .filter { property ->
                val calls = flattenCallChain(property.initializer ?: return@filter false)
                calls.any { getCallName(it) == "references" }
            }
            .map { it.name ?: "" }
            .toSet()

        return pkColumnNames == fkColumnNames
    }

    private fun extractReferenceAction(args: List<KtValueArgument>, paramName: String): ReferenceAction? {
        val arg = args.find { it.getArgumentName()?.asName?.asString() == paramName }
            ?: return null
        val value = arg.getArgumentExpression()?.text ?: return null
        return when {
            value.contains("CASCADE") -> ReferenceAction.CASCADE
            value.contains("SET_NULL") -> ReferenceAction.SET_NULL
            value.contains("RESTRICT") -> ReferenceAction.RESTRICT
            value.contains("NO_ACTION") -> ReferenceAction.NO_ACTION
            value.contains("SET_DEFAULT") -> ReferenceAction.SET_DEFAULT
            else -> null
        }
    }

    private fun flattenCallChain(expression: KtExpression): List<KtExpression> {
        val result = mutableListOf<KtExpression>()
        var current: KtExpression? = expression
        while (current != null) {
            when (current) {
                is KtDotQualifiedExpression -> {
                    result.add(0, current.selectorExpression ?: current)
                    current = current.receiverExpression
                }
                is KtCallExpression -> {
                    result.add(0, current)
                    current = null
                }
                else -> {
                    result.add(0, current)
                    current = null
                }
            }
        }
        return result
    }

    private fun getCallName(expression: KtExpression): String? = when (expression) {
        is KtCallExpression -> expression.calleeExpression?.text
        is KtDotQualifiedExpression -> (expression.selectorExpression as? KtCallExpression)?.calleeExpression?.text
        else -> null
    }

    private fun getCallArguments(expression: KtExpression): List<KtValueArgument> = when (expression) {
        is KtCallExpression -> expression.valueArguments
        is KtDotQualifiedExpression -> (expression.selectorExpression as? KtCallExpression)?.valueArguments ?: emptyList()
        else -> emptyList()
    }

    private fun resolvePropertyName(text: String): String = text.trim()

    companion object {
        private val TABLE_TYPES = setOf(
            "Table", "IntIdTable", "LongIdTable", "UUIDTable", "CompositeIdTable"
        )
    }
}
