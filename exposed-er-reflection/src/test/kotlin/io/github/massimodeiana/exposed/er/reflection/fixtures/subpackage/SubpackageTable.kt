package io.github.massimodeiana.exposed.er.reflection.fixtures.subpackage

import org.jetbrains.exposed.v1.core.Table

object TestCategories : Table("categories") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}
