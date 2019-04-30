package com.github.masooh.intellij.plugin.groovyfier

import com.github.masooh.intellij.plugin.groovyfier.Block.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.plugins.groovy.codeInspection.GroovyQuickFixFactory
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrLabeledStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrUnaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil

enum class Block {
    EXPECT, GIVEN, WHEN, THEN;

    val label
        get() = this.name.toLowerCase()
}

class JUnitToSpockApplier(event: AnActionEvent) {
    companion object {
        private val log = Logger.getInstance(JUnitToSpockApplier::class.java)
    }

    private val project: Project = event.project!!
    private val psiFile: PsiFile = event.getRequiredData(PlatformDataKeys.PSI_FILE)
    private val editor: Editor = event.getRequiredData(PlatformDataKeys.EDITOR)
    private val typeDefinition: GrTypeDefinition

    private val groovyFactory
        get() = GroovyPsiElementFactory.getInstance(project)

    private val javaFactory
        get() = JavaPsiFacade.getInstance(project).elementFactory

    init {
        typeDefinition = psiFile.getPsiClass() as GrTypeDefinition
    }

    fun transformToSpock() {
        // spock has it's own runner
        typeDefinition.getAnnotation("org.junit.runner.RunWith")?.delete()

        extendSpecification()
        changeMethods()

        // TODO add spock to dependencies if not present
        // TODO falls Test src/test/groovy anlegt bricht Umwandlung um: PSI and index do not match

        // TODO features durchgehen: https://github.com/opaluchlukasz/junit2spock
        // TODO Plugin/Feature fähig machen, wie bei junit2spock

        /* TODO Wicket feature: tester.assertComponent(FILE_LIST_PATH, ListView.class) -> then: (nur tester.assertX)
            nicht tester.lastRenderedPage.add(checkEventBehavior) oder tester.executeAjaxEvent(NEXT_STEP_LINK_PATH, "onclick")
         */
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

            // TODO extract changes in list and apply only one of it
            changeMethodHavingAnnotation(method, "org.junit.Test", "org.junit.jupiter.api.Test") { annotation ->
                var exceptionClass: GrReferenceExpression? = null
                for (attribute in annotation.parameterList.attributes) {
                    when (attribute.name) {
                        "expected" -> exceptionClass = attribute.value as GrReferenceExpression
                        // TODO timeout
                        else -> log.error("unhandled attribute: {}", attribute.name)
                    }
                }

                method.changeMethodNameTo("\"" + camelToSpace(method.name) + "\"")
                        .voidReturnToDef()

                changeFeatureBody(method)

                if (exceptionClass != null) {
                    val statement = "then: thrown(${exceptionClass.qualifierExpression!!.text})"
                    val thrownBlock = createStatementFromText<GrLabeledStatement>(statement)

                    // add would insert statement after closing }
                    method.block!!.addBefore(thrownBlock, method.block!!.lastChild)
                }
            }

            changeMethodHavingAnnotation(method, "org.junit.Before", "org.junit.jupiter.api.BeforeEach") {
                method.changeMethodNameTo("setup")
            }

            changeMethodHavingAnnotation(method, "org.junit.After", "org.junit.jupiter.api.AfterEach") {
                method.changeMethodNameTo("cleanup")
            }

            changeMethodHavingAnnotation(method, "org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll") {
                method.changeMethodNameTo("setupSpec")
                        .removeStaticModifier()
            }

            changeMethodHavingAnnotation(method, "org.junit.AfterClass", "org.junit.jupiter.api.AfterAll") {
                method.changeMethodNameTo("cleanupSpec")
                        .removeStaticModifier()
            }
        }
    }

    /**
     * also deletes the annotation and replaces void with def
     */
    private fun changeMethodHavingAnnotation(method: GrMethod, vararg annotationName: String,
                                             changeInMethod: (PsiAnnotation) -> Unit) {


        val annotation = annotationName.asSequence().mapNotNull { PsiImplUtil.getAnnotation(method, it) }.firstOrNull()

        if (annotation != null) {
            // todo macht es Sinn für jede Methode extra Write action?
            WriteCommandAction.runWriteCommandAction(project) {
                changeInMethod(annotation)
                annotation.delete()
                method.voidReturnToDef()
            }
        }
    }

    private fun changeFeatureBody(method: GrMethod) {
        val statements = method.block?.statements

        var currentBlock: Block? = null

        statements?.forEach { statement ->
            when (currentBlock) {
                null -> {
                    log.info("null:")
                    currentBlock = when {
                        statement.isAssertion() -> {
                            val replacedStatement = replaceWithSpockAssert(statement as GrMethodCallExpression)
                            addLabelToStatement(EXPECT, replacedStatement)
                        }
                        else -> {
                            addLabelToStatement(WHEN, statement)
                        }
                    }
                }
                WHEN -> {
                    log.info("when:")
                    if (statement.isAssertion()) {
                        // todo hier wird ersetzt, um das ersetzte wieder zu ersetzen
                        //    kann man das in einem machen? statement replace with (label + spock assert)
                        val statementWithSpockAssertion = replaceWithSpockAssert(statement as GrMethodCallExpression)
                        currentBlock = addLabelToStatement(THEN, statementWithSpockAssertion)
                    }
                }
                EXPECT -> {
                    log.info("expect:")
                    val block = handleExpectAndThen(statement)
                    if (block != null) {
                        currentBlock = block
                    }
                }
                GIVEN -> {
                    log.info("given:")
                    TODO("given not implemented yet")
                }
                THEN -> {
                    log.info("then:")
                    val block = handleExpectAndThen(statement)
                    if (block != null) {
                        currentBlock = block
                    }
                }
            }

        }
    }

    private fun handleExpectAndThen(statement: GrStatement): Block? {
        return when {
            statement.isAssertion() -> {
                replaceWithSpockAssert(statement as GrMethodCallExpression)
                null // stay in block
            }
            statement is GrVariableDeclaration -> {
                null // stay in block
            }
            else -> {
                addLabelToStatement(WHEN, statement) // next when
            }
        }
    }

    private fun addLabelToStatement(block: Block, statement: GrStatement): Block {
        val statementWithLabel = groovyFactory.createStatementFromText("${block.label}: statement", statement.parent)
        val textStatement = statementWithLabel.lastChild as GrStatement
        textStatement.replaceWithStatement(statement)

        // todo https://youtrack.jetbrains.com/issue/IDEA-185879
        //  wie komme ich an attachement

        statement.replaceWithStatement(statementWithLabel)
        return block
    }

    private fun replaceWithSpockAssert(methodCallExpression: GrMethodCallExpression): GrExpression {
        val argumentList = methodCallExpression.argumentList

        var spockAssert: GrExpression? = null
        var message: GrExpression? = null

        // remove Assert class if there
        val methodName = methodCallExpression.firstChild.text.replace("Assert.", "")

        when (methodName) {
            "assertEquals" -> {
                spockAssert = argumentList.withArgs(
                        two = { expected, actual ->
                            createBinaryExpression("actual == expected", actual, expected)
                        },
                        three = { msg, expected, actual ->
                            message = msg
                            createBinaryExpression("actual == expected", actual, expected)
                        })
            }
            "assertNotEquals" -> {
                spockAssert = argumentList.withArgs(
                        two = { expected, actual ->
                            createBinaryExpression("actual != unexpected", actual, expected)
                        },
                        three = { msg, expected, actual ->
                            message = msg
                            createBinaryExpression("actual != unexpected", actual, expected)
                        })
            }
            "assertTrue" -> {
                argumentList.withArgs(
                        one = { it },
                        two = { msg, cond ->
                            message = msg
                            cond
                        })?.let {
                    spockAssert = createExpression("condition").replaceWithExpression(it, true)
                }
            }
            "assertFalse" -> {
                argumentList.withArgs(
                        one = { it },
                        two = { msg, condition ->
                            message = msg
                            condition
                        })?.let {
                    val unaryExpression = createExpression<GrUnaryExpression>("!actual")
                    unaryExpression.operand!!.replaceWithExpression(it, true)
                    spockAssert = unaryExpression
                }
            }
            "assertNotNull" -> {
                argumentList.withArgs(
                        one = { it },
                        two = { msg, obj ->
                            message = msg
                            obj
                        }
                )?.let {
                    spockAssert = createExpression<GrBinaryExpression>("actual != null").apply {
                        leftOperand.replaceWithExpression(it, true)
                    }
                }
            }
            "assertNull" -> {
                argumentList.withArgs(
                        one = { it },
                        two = { msg, obj ->
                            message = msg
                            obj
                        })?.let {
                    val nullExpression = createExpression<GrBinaryExpression>("actual == null")
                    nullExpression.leftOperand.replaceWithExpression(it, true)
                    spockAssert = nullExpression
                }
            }
            else -> {
                log.warn("Unknown assert $methodName")
            }
        }

        log.info("Replace $methodName, args: ${argumentList.expressionArguments.size} with $spockAssert")

        return spockAssert?.let { assertion ->
            // actual replacement if not done above
            val replacedExpression = methodCallExpression.replaceWithExpression(assertion, true)

            message?.let {
                // add message as comment
                val whiteSpaceAndComment = replacedExpression.createComment(it.text)
                replacedExpression.addRangeAfter(whiteSpaceAndComment)
            }
            replacedExpression
        } ?: methodCallExpression
    }

    private fun createBinaryExpression(expression: String, left: GrExpression, right: GrExpression): GrBinaryExpression {
        val equalsExpression = createExpression<GrBinaryExpression>(expression)
        equalsExpression.leftOperand.replaceWithExpression(left, true)
        equalsExpression.rightOperand!!.replaceWithExpression(right, true)
        return equalsExpression
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : GrStatement> createStatementFromText(expression: String): T {
        return groovyFactory.createStatementFromText(expression) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : GrExpression> createExpression(expression: String): T {
        return groovyFactory.createExpressionFromText(expression) as T
    }

    /**
     * only insert extends if no extends are there
     */
    private fun extendSpecification() {
        if (typeDefinition.extendsList?.textLength == 0) {
            val definition = groovyFactory.createTypeDefinition("class A extends spock.lang.Specification {}")
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
}

private fun GrStatement.isAssertion(): Boolean {
    return if (this is GrMethodCallExpression) {
        val text = this.firstChild.text
        // with or without import
        text.startsWith("assert") || text.startsWith("Assert.")
    } else {
        false
    }
}
