package com.github.masooh.intellij.plugin.junitspock

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.actions.CleanupInspectionUtil
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.groovy.intentions.base.Intention
import org.jetbrains.plugins.groovy.intentions.conversions.ConvertJavaStyleArrayIntention
import org.jetbrains.plugins.groovy.intentions.conversions.strings.ConvertConcatenationToGstringIntention
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrNewExpression
import java.util.*

object GroovyFixesApplier {
    fun applyGroovyFixes(event: AnActionEvent, psiFile: PsiFile) {
        val project = event.project ?: return
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)

        WriteCommandAction.runWriteCommandAction(project) {
            findTypeAndInvokeIntention(
                    project, psiFile, editor,
                    GrNewExpression::class.java,
                    ConvertJavaStyleArrayIntention()
            )

            /*
                 must be executed before property style is active, otherwise "text ${"sdkjfl".getBytes()}"
                 is replaced with "text $"sdkjfl".bytes"
             */
            findTypeAndInvokeIntention(
                    project, psiFile, editor,
                    GrBinaryExpression::class.java,
                    ConvertConcatenationToGstringIntention()
            )

            // TODO call remaining intentions
            /**
             * Expression conversions

            Convert Indexing Method To [] Form
            Convert JUnit assertion to assert statement
            Convert Java-Style Array Creation to Groovy Syntax
            Convert String Concatenation to GString

            Groovy-style

            Remove redundant .class

            Groovy Intentions

            src/META-INF/groovy-intentions.xml
             */
        }


        val groovyInspections = findGroovyInspections()

        if (groovyInspections.isNotEmpty()) {
            // inspired by CleanupInspectionIntention
            val problemDescriptors = inspectFileForProblems(project, psiFile, groovyInspections)

            if (problemDescriptors.isEmpty()) {
                return
            }

            if (!FileModificationService.getInstance().preparePsiElementForWrite(psiFile)) {
                HintManager.getInstance().showErrorHint(editor, "Cannot write PSI element.")
                return
            }

            val fixesTask = CleanupInspectionUtil.getInstance().applyFixes(project, "Apply Groovy Fixes", problemDescriptors, null, false)

            if (!fixesTask.isApplicableFixFound) {
                HintManager.getInstance().showErrorHint(editor, "Unfortunately Groovy fixes are currently not available for batch mode\n User interaction is required for each problem found.")
            }
        }
    }

    private fun findTypeAndInvokeIntention(
            project: Project,
            psiFile: PsiFile,
            editor: Editor,
            clazz: Class<out GroovyPsiElement>,
            intention: Intention
    ) {
        val collection = PsiTreeUtil.findChildrenOfType(psiFile, clazz)
        collection.forEach {
            editor.caretModel.moveToOffset(it.textOffset)
            val available = intention.isAvailable(project, editor, psiFile)
            if (available) {
                intention.invoke(project, editor, psiFile)
            }
        }
    }

    private fun inspectFileForProblems(project: Project, file: PsiFile, groovyInspections: List<LocalInspectionToolWrapper>): List<ProblemDescriptor> {
        val problemDescriptors = ProgressManager.getInstance().runProcess<Map<String, List<ProblemDescriptor>>>({
            val inspectionManager = InspectionManager.getInstance(project)
            InspectionEngine.inspectEx(groovyInspections, file, inspectionManager, false, EmptyProgressIndicator())
        }, EmptyProgressIndicator())

        val descriptions = ArrayList<ProblemDescriptor>()
        for (group in problemDescriptors.values) {
            descriptions.addAll(group)
        }
        return descriptions
    }

    private fun findGroovyInspections(): List<LocalInspectionToolWrapper> {
        val inspectionToolWrappers = InspectionToolRegistrar.getInstance().createTools()
        return inspectionToolWrappers
                .asSequence()
                .filter {
                    val language = it.language
                    language != null && language.equals("groovy", ignoreCase = true)
                }
                .filter { it.groupDisplayName.equals("GPath", ignoreCase = true) || it.groupDisplayName.equals("Style", ignoreCase = true) }
                .filter { it.id != "ChangeToMethod" } // this is not groovy style
                .filter { it is LocalInspectionToolWrapper }
                .map { it as LocalInspectionToolWrapper }
                .toList()
    }
}