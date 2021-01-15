package com.github.masooh.intellij.plugin.junitspock

import com.github.masooh.intellij.plugin.junitspock.action.ConvertJavaToGroovy
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class ConvertJavaToGroovyTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getTestDataPath() = "src/test/resources/testdata"

    fun testName() {
        myFixture.configureByFile("BookTest.java")
        myFixture.testAction(ConvertJavaToGroovy())
        myFixture.checkResultByFile("BookTestTransformed.groovy")
    }
}
