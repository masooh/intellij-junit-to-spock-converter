// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor

interface GroovyProjectDescriptors {
    companion object {
        var LIB_GROOVY_2_5: TestLibrary = RepositoryTestLibrary("org.codehaus.groovy:groovy:2.5.11")
        var LIB_GROOVY_3_0: TestLibrary = RepositoryTestLibrary("org.codehaus.groovy:groovy:3.0.0-alpha-2")
        var LIB_JUNIT4: TestLibrary = RepositoryTestLibrary("junit:junit:4.13.1")
        var LIB_JUNIT5: TestLibrary = RepositoryTestLibrary("org.junit.jupiter:junit-jupiter:5.7.0")
        var LIB_HAMCREST: TestLibrary = RepositoryTestLibrary("org.hamcrest:hamcrest:2.2")

        val GROOVY_2_5: LightProjectDescriptor = LibraryLightProjectDescriptor(LIB_GROOVY_2_5)
        val GROOVY_3_0: LightProjectDescriptor = LibraryLightProjectDescriptor(LIB_GROOVY_3_0)

        val GROOVY_2_5_JUNIT4_HAMCREST: LightProjectDescriptor = LibraryLightProjectDescriptor(LIB_GROOVY_2_5, LIB_JUNIT4, LIB_HAMCREST)
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
