package io.github.massimodeiana.exposed.er.reflection.fixtures

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object TestUsers : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255)
    val name = varchar("name", 255)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("users_email_unique", email)
    }
}

object TestRecipes : Table("recipes") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val ownerId = long("owner_id").references(TestUsers.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val visibility = varchar("visibility", 20).default("PRIVATE")

    override val primaryKey = PrimaryKey(id)

    init {
        index("idx_recipes_owner_id", false, ownerId)
    }
}

object TestIngredients : Table("ingredients") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}

object TestRecipesIngredients : Table("recipes_ingredients") {
    val recipeId = long("recipe_id").references(TestRecipes.id)
    val ingredientId = long("ingredient_id").references(TestIngredients.id)
    val quantity = double("quantity")

    override val primaryKey = PrimaryKey(recipeId, ingredientId)
}

object TestTaxonomyNodes : Table("taxonomy_nodes") {
    val id = long("id").autoIncrement()
    val taxonomyKey = varchar("taxonomy_key", 255)
    val parentId = long("parent_id").references(id).nullable()

    override val primaryKey = PrimaryKey(id)
}

object NotATable {
    val something = "I am not a table"
}
