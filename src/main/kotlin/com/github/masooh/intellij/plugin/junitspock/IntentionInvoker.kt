package com.github.masooh.intellij.plugin.junitspock

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.groovy.intentions.base.Intention
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement

class IntentionInvoker(private var project: Project,
                       private var psiFile: PsiFile,
                       private var editor: Editor) {

    fun findChildrenOfTypeAndInvokeIntention(
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

}

