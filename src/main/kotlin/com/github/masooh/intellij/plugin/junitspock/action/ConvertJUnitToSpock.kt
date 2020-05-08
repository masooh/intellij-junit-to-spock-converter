package com.github.masooh.intellij.plugin.junitspock.action

import com.github.masooh.intellij.plugin.junitspock.GroovyFixesApplier
import com.github.masooh.intellij.plugin.junitspock.JUnitToSpockApplier
import com.github.masooh.intellij.plugin.junitspock.JavaToGroovyFileHelper
import com.github.masooh.intellij.plugin.junitspock.getPsiClass
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition

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

        JavaToGroovyFileHelper.createGroovyRootAndMoveFile(project, currentFile) { project, groovyPsiFile ->
            replaceCurlyBracesInAnnotationAttributes(groovyPsiFile, project)

            GroovyFixesApplier.applyGroovyFixes(event, groovyPsiFile)
            JUnitToSpockApplier(event, groovyPsiFile).transformToSpock()
        }

    }

    private fun replaceCurlyBracesInAnnotationAttributes(psiFile: PsiFile, project: Project?) {
        val typeDefinition = psiFile.getPsiClass() as GrTypeDefinition

        WriteCommandAction.runWriteCommandAction(project) {
            // TODO alle Annotationen, nicht nur von Klasse
            val classAnnotations = typeDefinition.annotations
            for (classAnnotation in classAnnotations) {
                val attributes = classAnnotation.parameterList.attributes
                if (attributes.isNotEmpty()) {
                    val factory = GroovyPsiElementFactory.getInstance(project!!)
                    val annotation = factory.createAnnotationFromText("@Annotation(att=[1])")
                    val attributeValue = annotation.parameterList.attributes[0].value

                    for (attribute in attributes) {
                        val annotationMemberValue = attribute.value

                        if ("{" == annotationMemberValue!!.firstChild.text && "}" == annotationMemberValue.lastChild.text) {
                            // { -> [
                            annotationMemberValue.firstChild.replace(attributeValue!!.firstChild)

                            // } -> ]
                            annotationMemberValue.lastChild.replace(attributeValue.lastChild)
                        }
                    }
                }
            }
        }
    }
}