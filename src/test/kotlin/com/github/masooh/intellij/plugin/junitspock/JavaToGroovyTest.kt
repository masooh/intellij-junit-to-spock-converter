package com.github.masooh.intellij.plugin.junitspock

class JavaToGroovyTest : BaseAcceptanceTest() {
    override fun getTestPath() = "javaToGroovy"

    fun testArrays() = javaToGroovy()
}