package com.github.masooh.intellij.plugin.groovyfier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.psi.PsiFile
import com.intellij.testFramework.TestActionEvent
import org.jetbrains.plugins.groovy.LightGroovyTestCase
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

//class ConvertJavaToGroovyTest2 : LightJavaCodeInsightFixtureTestCase() {
class ConvertJavaToGroovyTest : LightGroovyTestCase() {

    override fun getTestDataPath() = "src/test/resources/testdata"

    fun testName() {
        // copies from #getTestDataPath to test project and opens in editor
        val psiFile = myFixture.configureByFile("BookTest.groovy") as GroovyFile

        // todo psiFile is not recognized as Groovy but Plain Text, Groovy is not in list of FileType type = myPatternsTable.findAssociatedFileType(fileName);
//        myFixture.testAction(ConvertJavaToGroovy())
        testAction(ConvertJavaToGroovy(), psiFile)

        myFixture.checkResultByFile("BookTestTransformed.groovy")
    }

    /** inspired by myFixture.testAction
     */
    private fun testAction(action: AnAction, psiFile: PsiFile): Presentation? {
        val e = TestActionEvent(action) // hier Rückbezug was passiert damit?
//        action.beforeActionPerformedUpdate(e)
        if (e.presentation.isEnabled && e.presentation.isVisible) {
//            action.actionPerformed(e)
            // an dieser Stelle gehen wir schon davon aus, dass es eine Groovy Datei ist
            // -> Editor beibringen, dass Inhalt Groovy ist
            // GroovyFixesApplier.applyGroovyFixes(event) geht evtl. auch
            JUnitToSpockApplier(e, psiFile).transformToSpock()
        }
        return e.presentation
    }
}