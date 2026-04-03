package io.github.massimodeiana.exposed.er.intellij.toolwindow

import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

class ErDiagramPanel : JPanel(BorderLayout()) {

    private val browser: JBCefBrowser = JBCefBrowser()
    private var tempDir: File? = null

    init {
        val fileUrl = extractHtmlToTempDir()
        if (fileUrl != null) {
            browser.loadURL(fileUrl)
        }
        add(browser.component, BorderLayout.CENTER)
    }

    private fun extractHtmlToTempDir(): String? {
        val dir = File(System.getProperty("java.io.tmpdir"), "exposed-er-diagram")
        dir.mkdirs()
        tempDir = dir

        val htmlStream = javaClass.getResourceAsStream("/html/mermaid-viewer.html") ?: return null
        val jsStream = javaClass.getResourceAsStream("/html/mermaid.min.js") ?: return null

        val htmlFile = File(dir, "mermaid-viewer.html")
        val jsFile = File(dir, "mermaid.min.js")

        htmlFile.outputStream().use { out -> htmlStream.copyTo(out) }
        jsFile.outputStream().use { out -> jsStream.copyTo(out) }

        return htmlFile.toURI().toString()
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
        tempDir?.deleteRecursively()
    }
}
