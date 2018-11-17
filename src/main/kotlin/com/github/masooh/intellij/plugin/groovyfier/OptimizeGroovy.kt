package com.github.masooh.intellij.plugin.groovyfier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.groovy.GroovyFileType

/**
 * @author masooh
 */
class OptimizeGroovy : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentFile = requireNotNull(event.getData(PlatformDataKeys.VIRTUAL_FILE))

        if ("groovy" != currentFile.extension) {
            return
        }

        // TODO why must this be the first action otherwise exception
        GroovyFixesApplier.applyGroovyFixes(event)

        /* TODO Groovy array handling
            List<IdentAttachmentMetaData> documents = new ArrayList<>()
            documents.add(test)

            -> List<IdentAttachmentMetaData> documents = [test]
         */
        /* TODO GString intent expression conversion
            FILE_LIST_PATH + ":0:documentDownload" -> GString (
         */
    }

    override fun update(event: AnActionEvent?): Unit {
        val file = event!!.getData(PlatformDataKeys.PSI_FILE)
        val editor = event.getData(PlatformDataKeys.EDITOR)

        val enabled = file != null &&
                editor != null &&
                file.fileType in GroovyFileType.getGroovyEnabledFileTypes()

        event.presentation.isEnabled = enabled
    }

    companion object {
        private val LOG = Logger.getInstance(OptimizeGroovy::class.java)
    }
}
