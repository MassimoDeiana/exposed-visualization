package io.github.massimodeiana.exposed.er.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class GenerateErDiagramAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Exposed ER Diagram")
        if (toolWindow != null) {
            toolWindow.show()
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }
}
