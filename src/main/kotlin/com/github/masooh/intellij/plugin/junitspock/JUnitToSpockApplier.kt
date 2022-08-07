package com.github.masooh.intellij.plugin.junitspock

import com.github.masooh.intellij.plugin.junitspock.Block.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.plugins.groovy.codeInspection.GroovyQuickFixFactory
import org.jetbrains.plugins.groovy.intentions.conversions.RemoveParenthesesFromMethodCallIntention
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
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyFileImpl
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil

enum class Block {
    EXPECT, GIVEN, WHEN, THEN;

    val label
        get() = this.name.lowercase()
}

class JUnitToSpockApplier(private val project: Project, private val editor: Editor, private val psiFile: PsiFile) {
    companion object {
        private val log = Logger.getInstance(JUnitToSpockApplier::class.java)
    }

    private val typeDefinition = psiFile.getPsiClass() as GrTypeDefinition

    private val groovyFactory
        get() = GroovyPsiElementFactory.getInstance(project)

    fun transformToSpock() {
        WriteCommandAction.runWriteCommandAction(project, null, null, Runnable {
            // spock has it's own runner
            typeDefinition.getAnnotation("org.junit.runner.RunWith")?.delete()
        }, psiFile)

        extendSpecification()
        changeMethods()

        postTransformationIntentions()

        // TODO add spock to dependencies if not present
        // TODO falls Test src/test/groovy anlegt bricht Umwandlung um: PSI and index do not match

        // TODO features durchgehen: https://github.com/opaluchlukasz/junit2spock
        // TODO Plugin/Feature fähig machen, wie bei junit2spock

        /* TODO Wicket feature: tester.assertComponent(FILE_LIST_PATH, ListView.class) -> then: (nur tester.assertX)
            nicht tester.lastRenderedPage.add(checkEventBehavior) oder tester.executeAjaxEvent(NEXT_STEP_LINK_PATH, "onclick")
         */
        optimizeImports()
    }

    private fun postTransformationIntentions() {
        WriteCommandAction.runWriteCommandAction(project) {
            val intentionInvoker = IntentionInvoker(project, psiFile, editor)

            // Hamcrest - remove parentheses for expect/that
            intentionInvoker.findChildrenOfTypeAndInvokeIntention(
                    GrMethodCallExpression::class.java,
                    RemoveParenthesesFromMethodCallIntention()
            ) { psi ->
                psi.firstChild.textMatches("that") || psi.firstChild.textMatches("expect")
            }
        }
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

                method.changeMethodNameTo("\"${method.name.camelToSpace()}\"")
                        .voidReturnToDef()

                changeFeatureBody(method, exceptionClass)
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
            // todo should I use an extra WriteCommandAction for each change?
            WriteCommandAction.runWriteCommandAction(project) {
                changeInMethod(annotation)
                annotation.delete()
                method.voidReturnToDef()
            }
        }
    }

    private fun changeFeatureBody(method: GrMethod, exceptionClass: GrReferenceExpression?) {
        val statements = method.block?.statements

        var currentBlock: Block? = null

        statements?.forEachIndexed { idx, statement ->
            when (currentBlock) {
                null -> {
                    log.info("null:")
                    currentBlock = when {
                        statement.isAssertion() -> {
                            val replacedStatement = replaceWithSpockAssert(statement as GrMethodCallExpression, "that")
                            addLabelToStatement(EXPECT, replacedStatement)
                        }
                        else -> {
                            val nextStatement = statements.getOrNull(idx + 1)
                            when {
                                // -> method has only single statement
                                nextStatement == null -> {
                                    when {
                                        exceptionClass != null -> addLabelToStatement(WHEN, statement)
                                        else -> addLabelToStatement(EXPECT, statement)
                                    }
                                }
                                nextStatement.isAssertion() -> addLabelToStatement(WHEN, statement)
                                else -> addLabelToStatement(GIVEN, statement)
                            }
                        }
                    }
                }
                WHEN -> {
                    log.info("when:")
                    if (statement.isAssertion()) {
                        // todo hier wird ersetzt, um das ersetzte wieder zu ersetzen
                        //    kann man das in einem machen? statement replace with (label + spock assert)
                        val statementWithSpockAssertion = replaceWithSpockAssert(statement as GrMethodCallExpression, "expect")
                        currentBlock = addLabelToStatement(THEN, statementWithSpockAssertion)
                    }
                }
                EXPECT -> {
                    log.info("expect:")
                    val block = handleExpectAndThen(statement, statements.getOrNull(idx + 1), "that")
                    if (block != null) {
                        currentBlock = block
                    }
                }
                GIVEN -> {
                    log.info("given:")
                    val nextStatement = statements.getOrNull(idx + 1)
                    when {
                        nextStatement == null && exceptionClass == null -> currentBlock = addLabelToStatement(EXPECT, statement)
                        (nextStatement == null && exceptionClass != null) || nextStatement!!.isAssertion() -> {
                            currentBlock = addLabelToStatement(WHEN, statement)
                        }
                    }
                }
                THEN -> {
                    log.info("then:")
                    val block = handleExpectAndThen(statement, statements.getOrNull(idx + 1), "expect")
                    if (block != null) {
                        currentBlock = block
                    }
                }
            }
        }

        if (exceptionClass != null) {
            val statement = "then: thrown(${exceptionClass.qualifiedReferenceName})"
            val thrownBlock = createStatementFromText<GrLabeledStatement>(statement)

            // add would insert statement after closing }
            method.block!!.addBefore(thrownBlock, method.block!!.lastChild)
        }

    }

    private fun handleExpectAndThen(statement: GrStatement, nextStatement: GrStatement?, hamcrestAssertionName: String): Block? {
        return when {
            statement.isAssertion() -> {
                replaceWithSpockAssert(statement as GrMethodCallExpression, hamcrestAssertionName)
                null // stay in block
            }
            /**
             *  then:
             *  ...
             *  Book otherBook = new Book() // this must be placed in next when block
             *  when:
             *  otherBook.pages = 22
             */
            statement is GrVariableDeclaration && (nextStatement == null || nextStatement.isAssertion()) -> {
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

    private fun replaceWithSpockAssert(methodCallExpression: GrMethodCallExpression, hamcrestAssertionName: String): GrExpression {
        val argumentList = methodCallExpression.argumentList

        var spockAssert: GrExpression? = null
        var message: GrExpression? = null

        // remove Assert class if there
        val methodName = methodCallExpression.firstChild.text.replace("Assert.", "")

        when (methodName) {
            // Hamcrest
            "assertThat" -> {
                spockAssert = createStatementFromText(
                        methodCallExpression.text.replace("assertThat", hamcrestAssertionName)
                )

                val thatImportStatement = groovyFactory.createImportStatement("spock.util.matcher.HamcrestSupport.$hamcrestAssertionName", true, false, null, null)
                (methodCallExpression.containingFile as GroovyFileImpl).addImport(thatImportStatement)
            }
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
                        one = { condition -> condition },
                        two = { msg, cond ->
                            message = msg
                            cond
                        })?.let { condition ->
                    spockAssert = createExpression<GrReferenceExpression>("condition").replaceWithExpression(condition, true)
                }
            }
            "assertFalse" -> {
                argumentList.withArgs(
                        one = { condition -> condition },
                        two = { msg, condition ->
                            message = msg
                            condition
                        })?.let { condition ->
                    val unaryExpression = createExpression<GrUnaryExpression>("!actual")
                    unaryExpression.operand!!.replaceWithExpression(condition, true)
                    spockAssert = unaryExpression
                }
            }
            "assertNotNull" -> {
                argumentList.withArgs(
                        one = { obj -> obj },
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
                        one = { obj -> obj },
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
        return createExpression<GrBinaryExpression>(expression).apply {
            leftOperand.replaceWithExpression(left, true)
            rightOperand!!.replaceWithExpression(right, true)
        }
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
}

private fun String.camelToSpace(): String {
    return StringUtil.join(GroovyNamesUtil.camelizeString(this), { StringUtil.decapitalize(it) }, " ")
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
