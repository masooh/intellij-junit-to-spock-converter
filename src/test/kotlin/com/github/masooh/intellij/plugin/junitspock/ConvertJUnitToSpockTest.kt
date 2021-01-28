package com.github.masooh.intellij.plugin.junitspock

import com.intellij.openapi.project.DumbService
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors
import org.jetbrains.plugins.groovy.LightGroovyTestCase

class ConvertJavaToGroovyTest : LightGroovyTestCase() {

    override fun getTestDataPath() = "src/test/resources/testdata"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        // we need to add all used libraries so that annotations and types can be resolved
        return GroovyProjectDescriptors.GROOVY_2_5_JUNIT4_SPOCK_HAMCREST
    }

    // must start with 'test' prefix
    fun testClassAndFeatureAreConverted() {
        // copies from #getTestDataPath to test project and opens in editor
        val psiFile = myFixture.configureByFile("SimpleTest.groovy")

        DumbService.getInstance(project).runWhenSmart {
            GroovyConverter.replaceCurlyBracesInAnnotationAttributes(psiFile, project)
            GroovyConverter.applyGroovyFixes(psiFile, project, editor)
            JUnitToSpockApplier(project, editor, psiFile).transformToSpock()
        }

        myFixture.checkResultByFile("SimpleTestTransformed.groovy", true)
    }
}