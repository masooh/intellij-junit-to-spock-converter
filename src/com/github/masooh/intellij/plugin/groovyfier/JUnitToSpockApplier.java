package com.github.masooh.intellij.plugin.groovyfier;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.codeInspection.GroovyQuickFixFactory;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrExtendsClause;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JUnitToSpockApplier {
    private static final Logger LOG = Logger.getInstance(JUnitToSpockApplier.class);

    void transformToSpock(AnActionEvent event) {
        Project project = event.getProject();
        PsiFile psiFile = event.getRequiredData(PlatformDataKeys.PSI_FILE);
        Editor editor = event.getRequiredData(PlatformDataKeys.EDITOR);

        GrTypeDefinition grTypeDefinition = (GrTypeDefinition) getPsiClass(psiFile);

        extendSpecification(project, psiFile, grTypeDefinition);

        changeMethods(project, grTypeDefinition);

        // TODO features durchgehen: https://github.com/opaluchlukasz/junit2spock

        optimizeImports(project, psiFile, editor);
    }

    private void optimizeImports(Project project, PsiFile psiFile, Editor editor) {
        /* führt optimize nicht durch. Ursache unklar
            WriteCommandAction.runWriteCommandAction(project, () ->
                JavaCodeStyleManager.getInstance(project).optimizeImports(psiFile));
        */

        // ask unklar was onTheFly ist
        IntentionAction fix = GroovyQuickFixFactory.getInstance().createOptimizeImportsFix(false);
        if (fix.isAvailable(project, editor, psiFile) && psiFile.isWritable()) {
            fix.invoke(project, editor, psiFile);
        }
    }

    private void changeMethods(Project project, GrTypeDefinition grTypeDefinition) {
        for (GrMethod grMethod : grTypeDefinition.getCodeMethods()) {

            changeMethodHavingAnnotation(project, grMethod, "org.junit.Test", () -> {
                String spacedMethodName = camelToSpace(grMethod.getName());

                changeMethodNameTo(project, grMethod, "\"" + spacedMethodName + "\"");
                voidToDef(grMethod);

                changeMethodBody(project, grMethod);
            });

            changeMethodHavingAnnotation(project, grMethod, "org.junit.Before", () -> {
                changeMethodNameTo(project, grMethod, "setup");
                voidToDef(grMethod);
            });

            changeMethodHavingAnnotation(project, grMethod, "org.junit.After", () -> {
                changeMethodNameTo(project, grMethod, "cleanup");
                voidToDef(grMethod);
            });

            changeMethodHavingAnnotation(project, grMethod, "org.junit.BeforeClass", () -> {
                changeMethodNameTo(project, grMethod, "setupSpec");
                grMethod.getModifierList().setModifierProperty(GrModifier.STATIC, false);
                voidToDef(grMethod);
            });

            changeMethodHavingAnnotation(project, grMethod, "org.junit.AfterClass", () -> {
                changeMethodNameTo(project, grMethod, "cleanupSpec");
                grMethod.getModifierList().setModifierProperty(GrModifier.STATIC, false);
                voidToDef(grMethod);
            });
        }
    }

    /**
     * also removes the annotation
     */
    private void changeMethodHavingAnnotation(Project project, GrMethod grMethod, String annotationName,
                                              Runnable changeInMethod) {

        PsiAnnotation annotation = PsiImplUtil.getAnnotation(grMethod, annotationName);

        if (annotation != null) {
            WriteCommandAction.runWriteCommandAction(project, annotation::delete);
            WriteCommandAction.runWriteCommandAction(project, changeInMethod);
        }
    }

    private void changeMethodNameTo(Project project, GrMethod grMethod, String name) {
        GrMethod methodFromText = getFactory(project).createMethodFromText("def " + name + "() {}");
        // change name
        grMethod.getNameIdentifierGroovy().replace(methodFromText.getNameIdentifierGroovy());
    }

    private void voidToDef(GrMethod grMethod) {
        // remove void
        grMethod.getReturnTypeElementGroovy().delete();

        grMethod.getModifierList().setModifierProperty(GrModifier.DEF, true);
    }

    private void changeMethodBody(Project project, GrMethod grMethod) {
        GrStatement firstStatement = grMethod.getBlock().getStatements()[0];
        GrStatement firstChildWithWhen = getFactory(project).createStatementFromText("when: " + firstStatement.getText());

        replaceElement(firstStatement, firstChildWithWhen);

        List<GrStatement> methodCalls = Arrays.stream(grMethod.getBlock().getStatements())
                .filter(grStatement -> grStatement instanceof GrMethodCallExpression)
                .collect(Collectors.toList());

        AtomicBoolean firstAssertion = new AtomicBoolean(true);

        methodCalls.stream().filter(grStatement -> grStatement.getFirstChild().getText().equals("assertEquals")).forEach(grStatement -> {
            GrArgumentList grArgumentList = (GrArgumentList) grStatement.getLastChild();
            GroovyPsiElement[] allArguments = grArgumentList.getAllArguments();
            String expected = allArguments[0].getText();
            String actual = allArguments[1].getText();
            String label = firstAssertion.get() ? "then: " : "";

            GrStatement assertion = getFactory(project).createStatementFromText(label + actual + " == " + expected);
            firstAssertion.set(false);

            replaceElement(grStatement, assertion);
        });
    }

    private void replaceElement(PsiElement current, PsiElement replacement) {
        current.getParent().addAfter(replacement, current);
        current.delete();
    }

    /**
     * only insert extends if no extends are there
     */
    private void extendSpecification(Project project, PsiFile psiFile, GrTypeDefinition grTypeDefinition) {
        if (grTypeDefinition.getExtendsList().getTextLength() == 0) {

            StringBuilder classText = new StringBuilder();
            classText.append("class A extends spock.lang.Specification {}");

            final GrTypeDefinition definition = getFactory(project).createTypeDefinition(classText.toString());
            GrExtendsClause extendsClause = definition.getExtendsClause();

            // ask welchen Effekt hat command und groupID?
            WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                PsiElement addedExtend = grTypeDefinition.addAfter(extendsClause, grTypeDefinition.getNameIdentifierGroovy());

                JavaCodeStyleManager.getInstance(project).shortenClassReferences(addedExtend);
            }, psiFile);
        }
    }

    @NotNull
    private GroovyPsiElementFactory getFactory(Project project) {
        return GroovyPsiElementFactory.getInstance(project);
    }

    private PsiClass getPsiClass(PsiFile psiFile) {
        if (psiFile instanceof PsiClassOwner) {
            PsiClassOwner psiClassOwner = (PsiClassOwner) psiFile;
            PsiClass[] psiClasses = psiClassOwner.getClasses();
            if (psiClasses.length == 1) {
                PsiClass psiClass = psiClasses[0];
                return psiClass;
            } else {
                LOG.error("More or less that one PSI class. Found: " + psiClasses.length);
            }
        }
        return null;
    }

    private String camelToSpace(final String string) {
        return StringUtil.join(GroovyNamesUtil.camelizeString(string), s -> StringUtil.decapitalize(s), " ");
    }
}