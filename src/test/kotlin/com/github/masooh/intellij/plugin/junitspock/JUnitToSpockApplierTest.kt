package com.github.masooh.intellij.plugin.junitspock

import com.intellij.openapi.project.DumbService
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors
import org.jetbrains.plugins.groovy.LightGroovyTestCase
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

class JUnitToSpockApplierTest : LightGroovyTestCase() {

    override fun getTestDataPath() = "src/test/resources/testdata"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        // we need to add all used libraries so that annotations and types can be resolved
        return GroovyProjectDescriptors.GROOVY_2_5_JUNIT4_HAMCREST
    }

    fun testName() {
        // copies from #getTestDataPath to test project and opens in editor
        val psiFile = myFixture.configureByFile("BookTest.groovy") as GroovyFile

        DumbService.getInstance(project).runWhenSmart {
            JUnitToSpockApplier(project, editor, psiFile).transformToSpock()
        }
        myFixture.checkResultByFile("BookTestTransformed.groovy")
    }
}
