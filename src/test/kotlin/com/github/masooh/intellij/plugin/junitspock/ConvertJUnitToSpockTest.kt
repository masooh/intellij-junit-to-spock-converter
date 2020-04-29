package com.github.masooh.intellij.plugin.groovyfier

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

//class ConvertJavaToGroovyTest2 : LightJavaCodeInsightFixtureTestCase() {
class ConvertJavaToGroovyTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getTestDataPath() = "src/test/resources/testdata"

    fun testName() {
        myFixture.configureByFile("BookTest.java")
        myFixture.testAction(ConvertJavaToGroovy())
        myFixture.checkResultByFile("BookTestTransformed.groovy")
    }
}