package com.github.masooh.intellij.plugin.junitspock

import com.github.masooh.intellij.plugin.junitspock.action.ConvertJUnitToSpock
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

// no virtual file

class ConvertJUnitToSpockTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getTestDataPath() = "src/test/resources/testdata"

    fun testName() {
        myFixture.configureByFile("BookTest.java")
        myFixture.testAction(ConvertJUnitToSpock())
        myFixture.checkResultByFile("BookTest.groovy")
    }
}