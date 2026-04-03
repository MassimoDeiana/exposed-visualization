package io.github.massimodeiana.exposed.er.intellij.psi

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.massimodeiana.exposed.er.core.model.ReferenceAction
import io.github.massimodeiana.exposed.er.core.model.RelationshipType
import org.jetbrains.kotlin.psi.KtFile

class PsiSchemaExtractorTest : BasePlatformTestCase() {

    private val extractor = PsiSchemaExtractor()

    fun testShouldFindTableObjectsInKotlinFiles() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Users : Table("users") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }

            object NotATable {
                val something = "hello"
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))

        assertEquals(1, schema.tables.size)
        assertEquals("users", schema.tables[0].name)
    }

    fun testShouldExtractTableNameFromConstructor() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object MyRecipes : Table("recipes") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))

        assertEquals("recipes", schema.tables[0].name)
    }

    fun testShouldExtractColumnNameAndType() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Users : Table("users") {
                val id = long("id")
                val email = varchar("email", 255)
                val name = varchar("name", 100)
                val bio = text("bio")
                val isActive = bool("is_active")
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))
        val columns = schema.tables[0].columns

        assertEquals(5, columns.size)
        assertEquals("id", columns[0].name)
        assertEquals("long", columns[0].type)
        assertEquals("email", columns[1].name)
        assertEquals("varchar(255)", columns[1].type)
        assertEquals("bio", columns[3].name)
        assertEquals("text", columns[3].type)
        assertEquals("is_active", columns[4].name)
        assertEquals("bool", columns[4].type)
    }

    fun testShouldDetectNullableColumns() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Recipes : Table("recipes") {
                val id = long("id")
                val description = text("description").nullable()
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))
        val columns = schema.tables[0].columns

        assertFalse(columns[0].nullable)
        assertTrue(columns[1].nullable)
    }

    fun testShouldExtractForeignKeyReferences() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table
            import org.jetbrains.exposed.v1.core.ReferenceOption

            object Users : Table("users") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }

            object Recipes : Table("recipes") {
                val id = long("id")
                val ownerId = long("owner_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))

        assertEquals(1, schema.relationships.size)
        val rel = schema.relationships[0]
        assertEquals("recipes", rel.fromTable)
        assertEquals("ownerId", rel.fromColumns[0])
        assertEquals("users", rel.toTable)
        assertEquals("id", rel.toColumns[0])
        assertEquals(ReferenceAction.SET_NULL, rel.onDelete)
    }

    fun testShouldExtractCompositePrimaryKey() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object RecipesIngredients : Table("recipes_ingredients") {
                val recipeId = long("recipe_id")
                val ingredientId = long("ingredient_id")
                val quantity = double("quantity")
                override val primaryKey = PrimaryKey(recipeId, ingredientId)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))
        val pk = schema.tables[0].primaryKey

        assertNotNull(pk)
        assertEquals(2, pk!!.columns.size)
        assertTrue(pk.columns.contains("recipeId"))
        assertTrue(pk.columns.contains("ingredientId"))
    }

    fun testShouldExtractSelfReferentialReferences() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object TaxonomyNodes : Table("taxonomy_nodes") {
                val id = long("id")
                val parentId = long("parent_id").references(TaxonomyNodes.id).nullable()
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))

        assertEquals(1, schema.relationships.size)
        val rel = schema.relationships[0]
        assertEquals("taxonomy_nodes", rel.fromTable)
        assertEquals("taxonomy_nodes", rel.toTable)
    }

    fun testShouldDetectManyToManyJunctionTables() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Recipes : Table("recipes") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }

            object Ingredients : Table("ingredients") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }

            object RecipesIngredients : Table("recipes_ingredients") {
                val recipeId = long("recipe_id").references(Recipes.id)
                val ingredientId = long("ingredient_id").references(Ingredients.id)
                val quantity = double("quantity")
                override val primaryKey = PrimaryKey(recipeId, ingredientId)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))
        val junctionRels = schema.relationships.filter { it.fromTable == "recipes_ingredients" }

        assertEquals(2, junctionRels.size)
        assertTrue(junctionRels.all { it.type == RelationshipType.MANY_TO_MANY })
    }

    fun testShouldExtractIndicesFromInitBlock() {
        val file = myFixture.configureByText(
            "Tables.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Users : Table("users") {
                val id = long("id")
                val email = varchar("email", 255)
                override val primaryKey = PrimaryKey(id)

                init {
                    uniqueIndex("users_email_unique", email)
                    index("idx_users_name", false, email)
                }
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file))
        val indices = schema.tables[0].indices

        assertEquals(2, indices.size)
        val uniqueIdx = indices.find { it.unique }
        assertNotNull(uniqueIdx)
        assertEquals("users_email_unique", uniqueIdx!!.name)
    }

    fun testShouldHandleMultipleTablesAcrossFiles() {
        val file1 = myFixture.configureByText(
            "Users.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Users : Table("users") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val file2 = myFixture.configureByText(
            "Recipes.kt",
            """
            import org.jetbrains.exposed.v1.core.Table

            object Recipes : Table("recipes") {
                val id = long("id")
                override val primaryKey = PrimaryKey(id)
            }
            """.trimIndent()
        ) as KtFile

        val schema = extractor.extractFromFiles(listOf(file1, file2))

        assertEquals(2, schema.tables.size)
        val names = schema.tables.map { it.name }.toSet()
        assertTrue(names.contains("users"))
        assertTrue(names.contains("recipes"))
    }
}
