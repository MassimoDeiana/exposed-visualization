package io.github.massimodeiana.exposed.er.reflection

import io.github.classgraph.ClassGraph
import org.jetbrains.exposed.v1.core.Table

class TableScanner {

    fun scan(
        packages: List<String>,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader
    ): List<Table> {
        val tables = mutableSetOf<Table>()

        ClassGraph()
            .addClassLoader(classLoader)
            .acceptPackages(*packages.toTypedArray())
            .enableClassInfo()
            .scan()
            .use { scanResult ->
                val tableClasses = scanResult.getSubclasses(Table::class.java)
                for (classInfo in tableClasses) {
                    val table = instantiateTable(classInfo.loadClass())
                    if (table != null) tables.add(table)
                }
            }

        return tables.toList()
    }

    private fun instantiateTable(clazz: Class<*>): Table? {
        return try {
            val instanceField = clazz.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.get(null) as? Table
        } catch (_: NoSuchFieldException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
