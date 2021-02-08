package com.github.masooh.intellij.plugin.junitspock

class Junit5ToSpockTest : BaseAcceptanceTest() {
    override fun getTestPath() = "junit5ToSpock"

    fun testBeforeAfterAnnotations() = junitToSpock()

    // will be fixed with: Support assertThrows() conversion, with and without variable declaration #21
    fun ignoreAssertions() = junitToSpock()
}