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
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.intentions.conversions.ConvertJavaStyleArrayIntention
import org.jetbrains.plugins.groovy.intentions.conversions.strings.ConvertConcatenationToGstringIntention
import org.jetbrains.plugins.groovy.intentions.style.RemoveRedundantClassPropertyIntention
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrNewExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import java.util.*

object GroovyFixesApplier {
    fun applyGroovyFixes(event: AnActionEvent, psiFile: PsiFile) {
        val project = event.project ?: return
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)

        WriteCommandAction.runWriteCommandAction(project) {
            val intentionInvoker = IntentionInvoker(project, psiFile, editor);

            /*
                List of Groovy Intention classes can be found here:
                src/META-INF/groovy-intentions.xml
             */

            // Convert String Concatenation to GString: new String[] { "4", "5" } -> ["4", "5"]
            intentionInvoker.findChildrenOfTypeAndInvokeIntention(
                    GrNewExpression::class.java,
                    ConvertJavaStyleArrayIntention()
            )

            /* Convert String Concatenation to GString
            ------------------------------------------
                "starting " + BookTest.class.getSimpleName() -> "starting ${BookTest.class.simpleName}"

                 must be executed before property style is active, otherwise "text ${"sdkjfl".getBytes()}"
                 is replaced with "text $"sdkjfl".bytes"
             */
            intentionInvoker.findChildrenOfTypeAndInvokeIntention(
                    GrBinaryExpression::class.java,
                    ConvertConcatenationToGstringIntention()
            )

            //  Remove redundant .class
            intentionInvoker.findChildrenOfTypeAndInvokeIntention(
                    GrReferenceExpression::class.java,
                    RemoveRedundantClassPropertyIntention()
            )

             // Convert Indexing Method To [] Form -> done by GPath Inspection
            // Convert JUnit assertion to assert statement -> done manually, does not work for Spock, see ConvertJunitAssertionToAssertStatementIntention
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