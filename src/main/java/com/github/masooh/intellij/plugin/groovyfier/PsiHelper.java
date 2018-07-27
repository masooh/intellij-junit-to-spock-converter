package com.github.masooh.intellij.plugin.groovyfier;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class PsiHelper {
    private static final Logger LOG = Logger.getInstance(PsiHelper.class);

    static void voidReturnToDef(GrMethod method) {
        // remove void
        method.getReturnTypeElementGroovy().delete();
        // add def
        method.getModifierList().setModifierProperty(GrModifier.DEF, true);
    }

    static void removeStaticModifier(GrMethod method) {
        method.getModifierList().setModifierProperty(GrModifier.STATIC, false);
    }

    static void replaceElement(PsiElement current, PsiElement replacement) {
        current.getParent().addAfter(replacement, current);
        current.delete();
    }

    static void changeMethodNameTo(GrMethod method, String name) {
        GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(method.getProject());
        GrMethod methodFromText = factory.createMethodFromText("def " + name + "() {}");

        // change name
        method.getNameIdentifierGroovy().replace(methodFromText.getNameIdentifierGroovy());
    }

    static PsiClass getPsiClass(PsiFile psiFile) {
        if (psiFile instanceof PsiClassOwner) {
            PsiClassOwner psiClassOwner = (PsiClassOwner) psiFile;
            PsiClass[] psiClasses = psiClassOwner.getClasses();
            if (psiClasses.length == 1) {
                PsiClass psiClass = psiClasses[0];
                return psiClass;
            } else {
                LOG.error("More or less that one PSI class. Found: " + Arrays.stream(psiClasses).map(
                    psiClass -> psiClass.getQualifiedName()
                ).collect(Collectors.joining()));
            }
        }
        return null;
    }
}