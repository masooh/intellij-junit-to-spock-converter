package com.github.masooh.intellij.plugin.groovyfier;

import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import com.intellij.psi.PsiElement;

public class PsiHelper {
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
}