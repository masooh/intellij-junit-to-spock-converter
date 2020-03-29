// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy

import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors.Companion.GROOVY_2_5

/**
 * @author peter
 */
abstract class LightGroovyTestCase : LightJavaCodeInsightFixtureTestCase() {
    val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        // avoid PSI/document/model changes are not allowed during highlighting
        EntryPointsManagerBase.DEAD_CODE_EP_NAME.extensionList
    }

    @Throws(Exception::class)
    public override fun tearDown() {
        super.tearDown()
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return GROOVY_2_5
    }

    /**
     * Return relative path to the test data. Path is relative to the
     * [com.intellij.openapi.application.PathManager.getHomePath]
     *
     * @return relative path to the test data.
     */
    @NonNls
    override fun getBasePath(): String? {
        return null
    }
}