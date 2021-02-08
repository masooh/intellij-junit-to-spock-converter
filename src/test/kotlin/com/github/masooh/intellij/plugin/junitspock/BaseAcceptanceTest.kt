package com.github.masooh.intellij.plugin.junitspock

import com.intellij.openapi.project.DumbService
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors
import org.jetbrains.plugins.groovy.LightGroovyTestCase

abstract class BaseAcceptanceTest : LightGroovyTestCase() {

    abstract fun getTestPath() : String

    override fun getTestDataPath() = "src/test/resources/testdata"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        // we need to add all used libraries so that annotations and types can be resolved
        return GroovyProjectDescriptors.GROOVY_2_5_JUNIT_SPOCK_1_HAMCREST
    }

    protected fun junitToSpock() {
        // otherwise class Book is not found and i.e. property style replacement does not work
        myFixture.copyDirectoryToProject("lib", "lib")

        // copies from #getTestDataPath to test project and opens in editor
        val psiFile = myFixture.configureByFile("${getTestPath()}/${getTestName(true)}/Junit.groovy")

        DumbService.getInstance(project).runWhenSmart {
            GroovyConverter.replaceCurlyBracesInAnnotationAttributes(psiFile, project)
            GroovyConverter.applyGroovyFixes(psiFile, project, editor)
            JUnitToSpockApplier(project, editor, psiFile).transformToSpock()
        }

        myFixture.checkResultByFile("${getTestPath()}/${getTestName(true)}/Spock.groovy", true)
    }

    protected fun javaToGroovy() {
        // otherwise class Book is not found and i.e. property style replacement does not work
        myFixture.copyDirectoryToProject("lib", "lib")

        // copies from #getTestDataPath to test project and opens in editor
        val psiFile = myFixture.configureByFile("${getTestPath()}/${getTestName(true)}/Java.groovy")

        DumbService.getInstance(project).runWhenSmart {
            GroovyConverter.replaceCurlyBracesInAnnotationAttributes(psiFile, project)
            GroovyConverter.applyGroovyFixes(psiFile, project, editor)
        }

        myFixture.checkResultByFile("${getTestPath()}/${getTestName(true)}/Groovy.groovy", true)
    }

}