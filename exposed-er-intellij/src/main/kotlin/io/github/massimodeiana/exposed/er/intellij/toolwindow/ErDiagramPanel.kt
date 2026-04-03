package io.github.massimodeiana.exposed.er.intellij.toolwindow

import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ErDiagramPanel : JPanel(BorderLayout()) {

    private val browser: JBCefBrowser = JBCefBrowser()

    init {
        val htmlUrl = javaClass.getResource("/html/mermaid-viewer.html")?.toExternalForm()
        if (htmlUrl != null) {
            browser.loadURL(htmlUrl)
        }
        add(browser.component, BorderLayout.CENTER)
    }

    fun updateDiagram(mermaidCode: String) {
        val escaped = mermaidCode
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
        browser.cefBrowser.executeJavaScript(
            "renderDiagram(\"$escaped\")",
            browser.cefBrowser.url,
            0
        )
    }

    val component: JComponent get() = this

    fun dispose() {
        browser.dispose()
    }
}
