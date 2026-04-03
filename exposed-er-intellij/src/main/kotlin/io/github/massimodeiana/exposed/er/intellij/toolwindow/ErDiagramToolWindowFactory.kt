package io.github.massimodeiana.exposed.er.intellij.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.content.ContentFactory
import io.github.massimodeiana.exposed.er.core.renderer.MermaidRenderer
import io.github.massimodeiana.exposed.er.intellij.psi.DiscoveredTable
import io.github.massimodeiana.exposed.er.intellij.psi.PsiSchemaExtractor
import kotlinx.coroutines.*
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultTreeModel

class ErDiagramToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val diagramPanel = ErDiagramPanel()
        val rootNode = CheckedTreeNode("Tables")

        val tree = CheckboxTree(TableTreeCellRenderer(), rootNode)
        TreeSpeedSearch.installOn(tree)

        val updater = DiagramUpdater(project, diagramPanel, tree, rootNode)

        tree.addCheckboxTreeListener(object : com.intellij.ui.CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                updater.updateDiagramFromSelection()
            }
        })

        val filterAction = object : AnAction("Filter Tables", "Select which tables to include in the diagram", AllIcons.General.Filter) {
            override fun actionPerformed(e: AnActionEvent) {
                val scrollPane = JScrollPane(tree).apply {
                    preferredSize = Dimension(400, 500)
                }
                JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(scrollPane, tree)
                    .setTitle("Select Exposed Tables")
                    .setMovable(true)
                    .setResizable(true)
                    .setRequestFocus(true)
                    .createPopup()
                    .showInFocusCenter()
            }
        }

        val refreshAction = object : AnAction("Refresh", "Rescan project for Exposed tables", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                updater.rescan()
            }
        }

        val selectAllAction = object : AnAction("Select All", "Select all tables", AllIcons.Actions.Selectall) {
            override fun actionPerformed(e: AnActionEvent) {
                setAllChecked(rootNode, true)
                (tree.model as DefaultTreeModel).nodeChanged(rootNode)
                tree.repaint()
                updater.updateDiagramFromSelection()
            }
        }

        val deselectAllAction = object : AnAction("Deselect All", "Deselect all tables", AllIcons.Actions.Unselectall) {
            override fun actionPerformed(e: AnActionEvent) {
                setAllChecked(rootNode, false)
                (tree.model as DefaultTreeModel).nodeChanged(rootNode)
                tree.repaint()
                updater.updateDiagramFromSelection()
            }
        }

        val actionGroup = DefaultActionGroup(filterAction, refreshAction, selectAllAction, deselectAllAction)
        val toolbar = ActionManager.getInstance().createActionToolbar("ExposedErDiagram", actionGroup, true)

        val toolWindowPanel = SimpleToolWindowPanel(true, true)
        toolbar.targetComponent = toolWindowPanel
        toolWindowPanel.toolbar = toolbar.component
        toolWindowPanel.setContent(diagramPanel)

        val content = ContentFactory.getInstance().createContent(toolWindowPanel, "", false)
        toolWindow.contentManager.addContent(content)

        DumbService.getInstance(project).runWhenSmart {
            updater.rescan()
        }

        PsiManager.getInstance(project).addPsiTreeChangeListener(
            object : PsiTreeChangeAdapter() {
                override fun childrenChanged(event: PsiTreeChangeEvent) {
                    if (event.file?.name?.endsWith(".kt") == true) {
                        updater.scheduleRescan()
                    }
                }
            },
            toolWindow.disposable
        )
    }

    private fun setAllChecked(node: CheckedTreeNode, checked: Boolean) {
        node.isChecked = checked
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode ?: continue
            setAllChecked(child, checked)
        }
    }
}

private class TableTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
    override fun customizeRenderer(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? CheckedTreeNode ?: return
        val renderer = textRenderer
        when (val userObject = node.userObject) {
            is String -> {
                renderer.icon = AllIcons.Nodes.Folder
                renderer.append(userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }
            is DiscoveredTable -> {
                renderer.icon = AllIcons.Nodes.DataTables
                renderer.append(userObject.objectName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                renderer.append("  ${userObject.tableName}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
    }
}

private class DiagramUpdater(
    private val project: Project,
    private val panel: ErDiagramPanel,
    private val tree: CheckboxTree,
    private val rootNode: CheckedTreeNode
) {
    private val extractor = PsiSchemaExtractor()
    private val renderer = MermaidRenderer()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pendingJob = AtomicReference<Job?>(null)

    fun scheduleRescan() {
        pendingJob.getAndSet(
            scope.launch {
                delay(500)
                rescan()
            }
        )?.cancel()
    }

    fun rescan() {
        scope.launch {
            val discovered = readAction { extractor.discoverTables(project) }

            SwingUtilities.invokeLater {
                rebuildTree(discovered)
                updateDiagramFromSelection()
            }
        }
    }

    fun updateDiagramFromSelection() {
        val selected = collectCheckedTables()
        if (selected.isEmpty()) {
            panel.updateDiagram("erDiagram")
            return
        }
        scope.launch {
            val mermaid = readAction {
                val schema = extractor.extractFromDiscovered(selected)
                renderer.render(schema)
            }
            SwingUtilities.invokeLater {
                panel.updateDiagram(mermaid)
            }
        }
    }

    private fun rebuildTree(discovered: List<DiscoveredTable>) {
        val previouslyChecked = collectCheckedTableNames()
        val isFirstScan = previouslyChecked.isEmpty() && rootNode.childCount == 0

        rootNode.removeAllChildren()

        val byPackage = discovered.groupBy { it.packageName }.toSortedMap()
        for ((pkg, tables) in byPackage) {
            val pkgNode = CheckedTreeNode(pkg.ifEmpty { "(default package)" })
            for (table in tables.sortedBy { it.objectName }) {
                val tableNode = CheckedTreeNode(table)
                tableNode.isChecked = isFirstScan || table.objectName in previouslyChecked
                pkgNode.add(tableNode)
            }
            pkgNode.isChecked = (0 until pkgNode.childCount).all {
                (pkgNode.getChildAt(it) as CheckedTreeNode).isChecked
            }
            rootNode.add(pkgNode)
        }
        rootNode.isChecked = true

        val model = tree.model as DefaultTreeModel
        model.setRoot(rootNode)
        model.reload()

        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    private fun collectCheckedTables(): List<DiscoveredTable> {
        val result = mutableListOf<DiscoveredTable>()
        for (i in 0 until rootNode.childCount) {
            val pkgNode = rootNode.getChildAt(i) as? CheckedTreeNode ?: continue
            for (j in 0 until pkgNode.childCount) {
                val tableNode = pkgNode.getChildAt(j) as? CheckedTreeNode ?: continue
                if (tableNode.isChecked) {
                    val table = tableNode.userObject as? DiscoveredTable ?: continue
                    result.add(table)
                }
            }
        }
        return result
    }

    private fun collectCheckedTableNames(): Set<String> {
        return collectCheckedTables().map { it.objectName }.toSet()
    }
}
