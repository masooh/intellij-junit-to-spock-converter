package com.github.masooh.intellij.plugin.junitspock.action

import com.github.masooh.intellij.plugin.junitspock.GroovyFixesApplier
import com.github.masooh.intellij.plugin.junitspock.JavaToGroovyFileHelper
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService

/**
 * @author masooh
 */
class ConvertJavaToGroovy : AnAction() {
    companion object {
        private val LOG = Logger.getInstance(ConvertJavaToGroovy::class.java)
    }

    override fun update(event: AnActionEvent) {
        val file = event.getData(PlatformDataKeys.VIRTUAL_FILE)
        val editor = event.getData(PlatformDataKeys.EDITOR)

        val enabled = file != null &&
                editor != null &&
                (JavaFileType.INSTANCE == file.fileType)

        event.presentation.isEnabled = enabled
                && !DumbService.isDumb(event.project!!)

    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = requireNotNull(event.project)
        val currentFile = event.getRequiredData(PlatformDataKeys.VIRTUAL_FILE)

        JavaToGroovyFileHelper.createGroovyRootAndMoveFile(project, currentFile) { project, groovyPsiFile ->
            // TODO why must this be the first action otherwise exception
            GroovyFixesApplier.applyGroovyFixes(event, groovyPsiFile)

            /* TODO Groovy array handling
                List<IdentAttachmentMetaData> documents = new ArrayList<>()
                documents.add(test)

                -> List<IdentAttachmentMetaData> documents = [test]
             */
            /* TODO GString intent expression conversion
                FILE_LIST_PATH + ":0:documentDownload" -> GString (
             */
        }
    }
}
