// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.project.IntelliJProjectConfiguration
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties

internal class RepositoryTestLibrary : TestLibrary {
    private val myCoordinates: Array<out String>
    private val myDependencyScope: DependencyScope

    constructor(dependencyScope: DependencyScope, vararg coordinates: String) {
        assert(coordinates.isNotEmpty())
        myCoordinates = coordinates
        myDependencyScope = dependencyScope
    }

    constructor(vararg coordinates: String) : this(DependencyScope.COMPILE, *coordinates)

    constructor(coordinates: String, dependencyScope: DependencyScope) : this(dependencyScope, coordinates)


    override fun addTo(module: Module, model: ModifiableRootModel) {
        val tableModel = model.moduleLibraryTable.modifiableModel
        val library = tableModel.createLibrary(myCoordinates[0])
        val libraryModel = library.modifiableModel
        for (coordinates in myCoordinates) {
            val roots = loadRoots(module.project, coordinates)
            for (root in roots) {
                libraryModel.addRoot(root.file, root.type)
            }
        }
        libraryModel.commit()
        tableModel.commit()
        model.findLibraryOrderEntry(library)!!.scope = myDependencyScope
    }

    companion object {
        fun loadRoots(project: Project?, coordinates: String?): Collection<OrderRoot> {
            val libraryProperties = RepositoryLibraryProperties(coordinates, true)
            val roots = JarRepositoryManager.loadDependenciesModal(project!!, libraryProperties, false,
                    false, null, remoteRepositoryDescriptions)
            assert(!roots.isEmpty())
            return roots
        }

        private val remoteRepositoryDescriptions: List<RemoteRepositoryDescription>?
            get() {
                val remoteRepositoryDescriptions = IntelliJProjectConfiguration.getRemoteRepositoryDescriptions()
                return remoteRepositoryDescriptions.map { repository ->
                    RemoteRepositoryDescription(repository.id, repository.name, repository.url)
                }
            }
    }
}