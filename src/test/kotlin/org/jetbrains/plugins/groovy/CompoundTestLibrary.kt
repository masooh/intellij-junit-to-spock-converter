// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel

internal class CompoundTestLibrary(vararg libraries: TestLibrary) : TestLibrary {
    private val myLibraries: Array<out TestLibrary>
    init {
        assert(libraries.isNotEmpty())
        myLibraries = libraries
    }

    override fun addTo(module: Module, model: ModifiableRootModel) {
        for (library in myLibraries) {
            library.addTo(module, model)
        }
    }

}