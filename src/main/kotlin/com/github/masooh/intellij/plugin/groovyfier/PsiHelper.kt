package com.github.masooh.intellij.plugin.groovyfier

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod

fun GrMethod.voidReturnToDef() {
    // remove void
    this.returnTypeElementGroovy!!.delete()
    // add def
    this.modifierList.setModifierProperty(GrModifier.DEF, true)
}

fun GrMethod.removeStaticModifier() {
    this.modifierList.setModifierProperty(GrModifier.STATIC, false)
}

fun PsiElement.replaceElement(replacement: PsiElement) {
    this.parent.addAfter(replacement, this)
    this.delete()
}

fun GrMethod.changeMethodNameTo(name: String) {
    val factory = GroovyPsiElementFactory.getInstance(this.project)
    val methodFromText = factory.createMethodFromText("def $name() {}")

    // change name
    this.nameIdentifierGroovy.replace(methodFromText.nameIdentifierGroovy)
}

fun PsiFile.getPsiClass(): PsiClass? {
    if (this is PsiClassOwner) {
        val psiClasses = this.classes
        if (psiClasses.size == 1) {
            return psiClasses[0]
        } else {
            LOG.error("More or less that one PSI class. Found: " +
                    psiClasses.map { psiClass -> psiClass.qualifiedName }.joinToString()
            )
        }
    }
    return null
}

val LOG = Logger.getInstance("groovyfier.PsiHelper")