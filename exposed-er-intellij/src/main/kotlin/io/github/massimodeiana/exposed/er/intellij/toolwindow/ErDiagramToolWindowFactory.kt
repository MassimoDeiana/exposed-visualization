package io.github.massimodeiana.exposed.er.intellij.toolwindow

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.content.ContentFactory
import io.github.massimodeiana.exposed.er.core.renderer.MermaidRenderer
import io.github.massimodeiana.exposed.er.intellij.psi.PsiSchemaExtractor
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

class ErDiagramToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ErDiagramPanel()
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        val updater = DiagramUpdater(project, panel)
        updater.refresh()

        PsiManager.getInstance(project).addPsiTreeChangeListener(
            object : PsiTreeChangeAdapter() {
                override fun childrenChanged(event: PsiTreeChangeEvent) {
                    if (event.file?.name?.endsWith(".kt") == true) {
                        updater.scheduleRefresh()
                    }
                }
            },
            toolWindow.disposable
        )
    }
}

private class DiagramUpdater(
    private val project: Project,
    private val panel: ErDiagramPanel
) {
    private val extractor = PsiSchemaExtractor()
    private val renderer = MermaidRenderer()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pendingJob = AtomicReference<Job?>(null)

    fun scheduleRefresh() {
        pendingJob.getAndSet(
            scope.launch {
                delay(500)
                refresh()
            }
        )?.cancel()
    }

    fun refresh() {
        scope.launch {
            val mermaid = readAction {
                val schema = extractor.extract(project)
                renderer.render(schema)
            }
            SwingUtilities.invokeLater {
                panel.updateDiagram(mermaid)
            }
        }
    }
}
