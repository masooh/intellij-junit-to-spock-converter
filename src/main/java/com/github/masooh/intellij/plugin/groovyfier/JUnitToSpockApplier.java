package com.github.masooh.intellij.plugin.groovyfier;

import static com.github.masooh.intellij.plugin.groovyfier.PsiHelper.changeMethodNameTo;
import static com.github.masooh.intellij.plugin.groovyfier.PsiHelper.removeStaticModifier;
import static com.github.masooh.intellij.plugin.groovyfier.PsiHelper.replaceElement;
import static com.github.masooh.intellij.plugin.groovyfier.PsiHelper.voidReturnToDef;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.codeInspection.GroovyQuickFixFactory;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrLabeledStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrUnaryExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrExtendsClause;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

public class JUnitToSpockApplier {
    private static final Logger LOG = Logger.getInstance(JUnitToSpockApplier.class);
    private final Project project;
    private final PsiFile psiFile;
    private final Editor editor;
    private final GrTypeDefinition typeDefinition;

    public JUnitToSpockApplier(AnActionEvent event) {
        project = event.getProject();
        psiFile = event.getRequiredData(PlatformDataKeys.PSI_FILE);
        editor = event.getRequiredData(PlatformDataKeys.EDITOR);
        typeDefinition = (GrTypeDefinition) PsiHelper.getPsiClass(psiFile);
    }

    void transformToSpock() {
        extendSpecification();
        changeMethods();

        // TODO add spock to dependencies if not present
        // TODO falls Test src/test/groovy anlegt bricht Umwandlung um: PSI and index do not match

        // TODO features durchgehen: https://github.com/opaluchlukasz/junit2spock
        // TODO Plugin/Feature fähig machen, wie bei junit2spock

        /* TODO Wicket feature: tester.assertComponent(FILE_LIST_PATH, ListView.class) -> then: (nur tester.assertX)
            nicht tester.lastRenderedPage.add(checkEventBehavior) oder tester.executeAjaxEvent(NEXT_STEP_LINK_PATH, "onclick")
         */
        // TODO first line then: -> expect:

        optimizeImports();
    }

    private void optimizeImports() {
        /* führt optimize nicht durch. Ursache unklar
            WriteCommandAction.runWriteCommandAction(project, () ->
                JavaCodeStyleManager.getInstance(project).optimizeImports(psiFile));
        */

        // TODO unklar was onTheFly ist
        IntentionAction fix = GroovyQuickFixFactory.getInstance().createOptimizeImportsFix(false);
        if (fix.isAvailable(project, editor, psiFile) && psiFile.isWritable()) {
            fix.invoke(project, editor, psiFile);
        }
    }

    private void changeMethods() {
        for (GrMethod method : typeDefinition.getCodeMethods()) {

            changeMethodHavingAnnotation(method, "org.junit.Test", () -> {
                changeMethodNameTo(method, "\"" + camelToSpace(method.getName()) + "\"");
                voidReturnToDef(method);
                changeMethodBody(method);
            });

            // TODO handle JUnit 5 annotations
            changeMethodHavingAnnotation(method, "org.junit.Before", () -> {
                changeMethodNameTo(method, "setup");
                voidReturnToDef(method);
            });

            changeMethodHavingAnnotation(method, "org.junit.After", () -> {
                changeMethodNameTo(method, "cleanup");
                voidReturnToDef(method);
            });

            changeMethodHavingAnnotation(method, "org.junit.BeforeClass", () -> {
                changeMethodNameTo(method, "setupSpec");
                removeStaticModifier(method);
                voidReturnToDef(method);
            });

            changeMethodHavingAnnotation(method, "org.junit.AfterClass", () -> {
                changeMethodNameTo(method, "cleanupSpec");
                removeStaticModifier(method);
                voidReturnToDef(method);
            });
        }
    }

    /**
     * also deletes the annotation
     */
    private void changeMethodHavingAnnotation(GrMethod method, String annotationName,
                                              Runnable changeInMethod) {

        PsiAnnotation annotation = PsiImplUtil.getAnnotation(method, annotationName);

        if (annotation != null) {
            Object exceptionClass = null;
            for (PsiNameValuePair attribute : annotation.getParameterList().getAttributes()) {
                switch (attribute.getName()) {
                    case "expected":
                        exceptionClass = attribute.getValue();
                        break;
                    // TODO timeout
                    default:
                        LOG.error("unhandled attribute: {}", attribute.getName());
                }
            }

            WriteCommandAction.runWriteCommandAction(project, annotation::delete);
            WriteCommandAction.runWriteCommandAction(project, changeInMethod);
        }
    }

    private void changeMethodBody(GrMethod method) {
        addWhenToFirstStatement(method);
        replaceAsserts(method);
    }

    private void replaceAsserts(GrMethod method) {
        List<GrStatement> methodCalls = Arrays.stream(method.getBlock().getStatements())
                .filter(grStatement -> grStatement instanceof GrMethodCallExpression)
                .collect(Collectors.toList());

        AtomicBoolean firstAssertion = new AtomicBoolean(true);

        methodCalls.stream()
                .filter(grStatement -> grStatement instanceof GrMethodCallExpression)
                .map(grStatement -> (GrMethodCallExpression)grStatement)
                .filter(methodCallExpression -> {
                    String text = methodCallExpression.getFirstChild().getText();
                    // with or without import
                    return text.startsWith("assert") || text.startsWith("Assert.");
                }).forEach(methodCall -> {
            GrExpression spockAssert = getSpockAssert(methodCall);

            if (firstAssertion.getAndSet(false)) {
                GrStatement spockAssertWithLabel = getFactory().createStatementFromText("then: expression");
                GrExpression grExpression = (GrExpression) spockAssertWithLabel.getLastChild();
                grExpression.replaceWithExpression(spockAssert, true);
                replaceElement(methodCall, spockAssertWithLabel);
            } else {
                replaceElement(methodCall, spockAssert);
            }
        });
    }

    private GrExpression getSpockAssert(GrMethodCallExpression methodCallExpression) {
        GrExpression[] expressionArguments = methodCallExpression.getArgumentList().getExpressionArguments();

        GrExpression firstArgument = expressionArguments[0];
        GrExpression secondArgument = expressionArguments.length > 1 ? expressionArguments[1] : null;

        GrExpression spockAssert;

        // remove Assert class if there
        String methodName = methodCallExpression.getFirstChild().getText().replace("Assert.", "");
        switch (methodName) {
            case "assertEquals":
                GrBinaryExpression equalsExpression = createExpression("actual == expected");
                equalsExpression.getLeftOperand().replaceWithExpression(secondArgument, true);
                equalsExpression.getRightOperand().replaceWithExpression(firstArgument, true);
                spockAssert = equalsExpression;
                break;
            case "assertTrue":
                spockAssert = createExpression("actual").replaceWithExpression(firstArgument, true);
                break;
            case "assertFalse":
                GrUnaryExpression unaryExpression = createExpression("!actual");
                unaryExpression.getOperand().replaceWithExpression(firstArgument, true);
                spockAssert = unaryExpression;
                break;
            case "assertNotNull":
                GrBinaryExpression notNullExpression = createExpression("actual != null");
                notNullExpression.getLeftOperand().replaceWithExpression(firstArgument, true);
                spockAssert = notNullExpression;
                break;
            case "assertNull":
                GrBinaryExpression nullExpression = createExpression("actual == null");
                nullExpression.getLeftOperand().replaceWithExpression(firstArgument, true);
                spockAssert = nullExpression;
                break;
            default:
                throw new IllegalArgumentException("Unknown assert " + methodName);
        }
        return spockAssert;
    }

    private void addWhenToFirstStatement(GrMethod grMethod) {
        GrStatement firstStatement = grMethod.getBlock().getStatements()[0];

        GrLabeledStatement firstStatementWithWhen = createStatementFromText("when: expression");
        firstStatementWithWhen.getStatement().replaceWithStatement(firstStatement);

        replaceElement(firstStatement, firstStatementWithWhen);
    }

    @NotNull
    private <T extends GrStatement> T createStatementFromText(String expression) {
        return (T) getFactory().createStatementFromText(expression);
    }

    @NotNull
    private <T extends GrExpression> T createExpression(String expression) {
        return (T) getFactory().createExpressionFromText(expression);
    }

    /**
     * only insert extends if no extends are there
     */
    private void extendSpecification() {
        if (typeDefinition.getExtendsList().getTextLength() == 0) {
            GrTypeDefinition definition = getFactory().createTypeDefinition("class A extends spock.lang.Specification {}");
            GrExtendsClause extendsClause = definition.getExtendsClause();

            // ask welchen Effekt hat command und groupID?
            WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                PsiElement addedExtend = typeDefinition.addAfter(extendsClause, typeDefinition.getNameIdentifierGroovy());

                JavaCodeStyleManager.getInstance(project).shortenClassReferences(addedExtend);
            }, psiFile);
        }
    }

    @NotNull
    private GroovyPsiElementFactory getFactory() {
        return GroovyPsiElementFactory.getInstance(project);
    }


    private String camelToSpace(final String string) {
        return StringUtil.join(GroovyNamesUtil.camelizeString(string), StringUtil::decapitalize, " ");
    }
}