package com.github.masooh.intellij.plugin.junitspock

class Junit4ToSpockTest : BaseAcceptanceTest() {
    override fun getTestPath() = "junit4ToSpock"

    fun testClassAndFeature() = junitToSpock()

    fun testBeforeAfterMethods() = junitToSpock()

    fun testAssertsAlsoWithMessages() = junitToSpock()

    fun testGivenWhenThenAnalysis() = junitToSpock()

    // will be activated with #3
    fun ignoreReuseGivenWhenThenComments() = junitToSpock()

    fun testExpectArgumentException() = junitToSpock()

    fun testSpringConfig() = junitToSpock()
}