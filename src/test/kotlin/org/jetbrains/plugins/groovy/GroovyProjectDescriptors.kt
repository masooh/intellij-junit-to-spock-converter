// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor

interface GroovyProjectDescriptors {
    companion object {
        val LIB_GROOVY_2_5: TestLibrary = RepositoryTestLibrary("org.codehaus.groovy:groovy:2.5.6")
        val LIB_GROOVY_3_0: TestLibrary = RepositoryTestLibrary("org.codehaus.groovy:groovy:3.0.0-alpha-2")

        val GROOVY_2_5: LightProjectDescriptor = LibraryLightProjectDescriptor(LIB_GROOVY_2_5)
        val GROOVY_3_0: LightProjectDescriptor = LibraryLightProjectDescriptor(LIB_GROOVY_3_0)

        val LIB_GROOVY_LATEST = LIB_GROOVY_2_5

        val GROOVY_LATEST: LightProjectDescriptor = LibraryLightProjectDescriptor(LIB_GROOVY_LATEST)

        val GROOVY_LATEST_REAL_JDK: LightProjectDescriptor = object : LibraryLightProjectDescriptor(LIB_GROOVY_LATEST) {
            override fun getSdk(): Sdk? {
                return JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
            }
        }
        val GROOVY_3_0_REAL_JDK: LightProjectDescriptor = object : LibraryLightProjectDescriptor(LIB_GROOVY_3_0) {
            override fun getSdk(): Sdk? {
                return JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
            }
        }
        val GROOVY_2_5_REAL_JDK: LightProjectDescriptor = object : LibraryLightProjectDescriptor(LIB_GROOVY_2_5) {
            override fun getSdk(): Sdk? {
                return JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
            }
        }
    }
}