package com.github.masooh.intellij.plugin.groovyfier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.plugins.groovy.codeInspection.GroovyQuickFixFactory
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrLabeledStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrUnaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil
import java.util.concurrent.atomic.AtomicBoolean

class JUnitToSpockApplier(event: AnActionEvent) {
    private val project: Project = event.project!!
    private val psiFile: PsiFile = event.getRequiredData(PlatformDataKeys.PSI_FILE)
    private val editor: Editor = event.getRequiredData(PlatformDataKeys.EDITOR)
    private val typeDefinition: GrTypeDefinition

    private val factory: GroovyPsiElementFactory
        get() = GroovyPsiElementFactory.getInstance(project)

    init {
        typeDefinition = psiFile.getPsiClass() as GrTypeDefinition
    }

    fun transformToSpock() {
        extendSpecification()
        changeMethods()

        // TODO add spock to dependencies if not present
        // TODO falls Test src/test/groovy anlegt bricht Umwandlung um: PSI and index do not match

        // TODO features durchgehen: https://github.com/opaluchlukasz/junit2spock
        // TODO Plugin/Feature fähig machen, wie bei junit2spock

        /* TODO Wicket feature: tester.assertComponent(FILE_LIST_PATH, ListView.class) -> then: (nur tester.assertX)
            nicht tester.lastRenderedPage.add(checkEventBehavior) oder tester.executeAjaxEvent(NEXT_STEP_LINK_PATH, "onclick")
         */
        // TODO first line then: -> expect:

        optimizeImports()
    }

    private fun optimizeImports() {
        /* führt optimize nicht durch. Ursache unklar
            WriteCommandAction.runWriteCommandAction(project, () ->
                JavaCodeStyleManager.getInstance(project).optimizeImports(psiFile));
        */

        // TODO unklar was onTheFly ist
        val fix = GroovyQuickFixFactory.getInstance().createOptimizeImportsFix(false)
        if (fix.isAvailable(project, editor, psiFile) && psiFile.isWritable) {
            fix.invoke(project, editor, psiFile)
        }
    }

    private fun changeMethods() {
        for (method in typeDefinition.codeMethods) {

            changeMethodHavingAnnotation(method, "org.junit.Test") { annotation ->
                var exceptionClass: GrReferenceExpression? = null
                for (attribute in annotation.parameterList.attributes) {
                    when (attribute.name) {
                        "expected" -> exceptionClass = attribute.value as GrReferenceExpression
                    // TODO timeout
                        else -> LOG.error("unhandled attribute: {}", attribute.name)
                    }
                }

                Runnable {
                    method.changeMethodNameTo("\"" + camelToSpace(method.name) + "\"")
                    method.voidReturnToDef()
                    changeMethodBody(method)

                    if (exceptionClass != null) {
                        val statement = "then: thrown(${exceptionClass.qualifierExpression!!.text})"
                        val thrownBlock = createStatementFromText<GrLabeledStatement>(statement)

                        // add would insert statement after closing }
                        method.block!!.addBefore(thrownBlock, method.block!!.lastChild)
                    }
                }
            }

            // TODO handle JUnit 5 annotations
            changeMethodHavingAnnotation(method, "org.junit.Before") {
                Runnable {
                    method.changeMethodNameTo("setup")
                    method.voidReturnToDef()
                }
            }

            changeMethodHavingAnnotation(method, "org.junit.After") {
                Runnable {
                    method.changeMethodNameTo("cleanup")
                    method.voidReturnToDef()
                }
            }

            changeMethodHavingAnnotation(method, "org.junit.BeforeClass") {
                Runnable {
                    method.changeMethodNameTo("setupSpec")
                    method.removeStaticModifier()
                    method.voidReturnToDef()
                }

            }

            changeMethodHavingAnnotation(method, "org.junit.AfterClass") {
                Runnable {
                    method.changeMethodNameTo("cleanupSpec")
                    method.removeStaticModifier()
                    method.voidReturnToDef()
                }
            }
        }
    }

    /**
     * also deletes the annotation
     */
    private fun changeMethodHavingAnnotation(method: GrMethod, annotationName: String,
                                             changeInMethod: (PsiAnnotation) -> Runnable) {

        val annotation = PsiImplUtil.getAnnotation(method, annotationName)

        if (annotation != null) {
            WriteCommandAction.runWriteCommandAction(project, changeInMethod(annotation))
            WriteCommandAction.runWriteCommandAction(project) { annotation.delete() }
        }
    }

    private fun changeMethodBody(method: GrMethod) {
        addWhenToFirstStatement(method)
        replaceAsserts(method)
    }

    private fun replaceAsserts(method: GrMethod) {
        val methodCalls = method.block!!.statements.filter { grStatement -> grStatement is GrMethodCallExpression }

        val firstAssertion = AtomicBoolean(true)

        methodCalls.filter { grStatement -> grStatement is GrMethodCallExpression }
                .map { grStatement -> grStatement as GrMethodCallExpression }
                .filter { methodCallExpression ->
                    val text = methodCallExpression.firstChild.text
                    // with or without import
                    methodCallExpression.
                    text.startsWith("assert") || text.startsWith("Assert.")
                }.forEach { methodCall ->
                    val spockAssert = getSpockAssert(methodCall)

                    if (firstAssertion.getAndSet(false)) {
                        val spockAssertWithLabel = factory.createStatementFromText("then: expression")
                        val grExpression = spockAssertWithLabel.lastChild as GrExpression
                        grExpression.replaceWithExpression(spockAssert, true)
                        methodCall.replaceElement(spockAssertWithLabel)
                    } else {
                        methodCall.replaceElement(spockAssert)
                    }
                }
    }

    private fun getSpockAssert(methodCallExpression: GrMethodCallExpression): GrExpression {
        val expressionArguments = methodCallExpression.argumentList.expressionArguments

        val firstArgument = expressionArguments[0]
        val secondArgument = if (expressionArguments.size > 1) expressionArguments[1] else null

        val spockAssert: GrExpression

        // remove Assert class if there
        val methodName = methodCallExpression.firstChild.text.replace("Assert.", "")
        when (methodName) {
            "assertEquals" -> {
                val equalsExpression = createExpression<GrBinaryExpression>("actual == expected")
                equalsExpression.leftOperand.replaceWithExpression(secondArgument!!, true)
                equalsExpression.rightOperand!!.replaceWithExpression(firstArgument, true)
                spockAssert = equalsExpression
            }
            "assertTrue" -> spockAssert = createExpression<GrExpression>("actual").replaceWithExpression(firstArgument, true)
            "assertFalse" -> {
                val unaryExpression = createExpression<GrUnaryExpression>("!actual")
                unaryExpression.operand!!.replaceWithExpression(firstArgument, true)
                spockAssert = unaryExpression
            }
            "assertNotNull" -> {
                val notNullExpression = createExpression<GrBinaryExpression>("actual != null")
                notNullExpression.leftOperand.replaceWithExpression(firstArgument, true)
                spockAssert = notNullExpression
            }
            "assertNull" -> {
                val nullExpression = createExpression<GrBinaryExpression>("actual == null")
                nullExpression.leftOperand.replaceWithExpression(firstArgument, true)
                spockAssert = nullExpression
            }
            else -> throw IllegalArgumentException("Unknown assert $methodName")
        }
        return spockAssert
    }

    private fun addWhenToFirstStatement(grMethod: GrMethod) {
        val firstStatement = grMethod.block!!.statements[0]

        val firstStatementWithWhen = createStatementFromText<GrLabeledStatement>("when: expression")
        firstStatementWithWhen.statement!!.replaceWithStatement(firstStatement)

        firstStatement.replaceElement(firstStatementWithWhen)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : GrStatement> createStatementFromText(expression: String): T {
        return factory.createStatementFromText(expression) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : GrExpression> createExpression(expression: String): T {
        return factory.createExpressionFromText(expression) as T
    }

    /**
     * only insert extends if no extends are there
     */
    private fun extendSpecification() {
        if (typeDefinition.extendsList?.textLength == 0) {
            val definition = factory.createTypeDefinition("class A extends spock.lang.Specification {}")
            val extendsClause = definition.extendsClause!!

            // ask welchen Effekt hat command und groupID?
            WriteCommandAction.runWriteCommandAction(project, null, null, Runnable {
                val addedExtend = typeDefinition.addAfter(extendsClause, typeDefinition.nameIdentifierGroovy)
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(addedExtend)
            }, psiFile)
        }
    }


    private fun camelToSpace(string: String): String {
        return StringUtil.join(GroovyNamesUtil.camelizeString(string), { StringUtil.decapitalize(it) }, " ")
    }

    companion object {
        private val LOG = Logger.getInstance(JUnitToSpockApplier::class.java)
    }
}