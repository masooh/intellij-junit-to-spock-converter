package com.github.masooh.intellij.plugin.junitspock

class Junit4ToSpockTest : BaseAcceptanceTest() {
    override fun getTestPath() = "junit4ToSpock"

    fun testClassAndFeature() = convertAndCheck()

    fun testBeforeAfterMethods() = convertAndCheck()

    fun testAssertsAlsoWithMessages() = convertAndCheck()

    fun testGivenWhenThenAnalysis() = convertAndCheck()

    // will be activated with #3
    fun ignoreReuseGivenWhenThenComments() = convertAndCheck()

    fun testExpectArgumentException() = convertAndCheck()
}