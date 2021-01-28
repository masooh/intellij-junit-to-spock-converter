package com.github.masooh.intellij.plugin.junitspock.action

import com.github.masooh.intellij.plugin.junitspock.GroovyConverter
import com.github.masooh.intellij.plugin.junitspock.JUnitToSpockApplier
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
class ConvertJUnitToSpock : AnAction() {
    companion object {
        private val LOG = Logger.getInstance(ConvertJUnitToSpock::class.java)
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
        val editor = event.getRequiredData(PlatformDataKeys.EDITOR)

        JavaToGroovyFileHelper.createGroovyRootAndMoveFile(project, currentFile) { groovyPsiFile ->
            GroovyConverter.replaceCurlyBracesInAnnotationAttributes(groovyPsiFile, project)
            GroovyConverter.applyGroovyFixes(groovyPsiFile, project, editor)
            JUnitToSpockApplier(project, editor, groovyPsiFile).transformToSpock()
        }

    }
}