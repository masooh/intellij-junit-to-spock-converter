package com.github.masooh.intellij.plugin.junitspock

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.IOException
import java.util.*

object JavaToGroovyFileHelper {
    private val LOG = Logger.getInstance(JavaToGroovyFileHelper::class.java)

    fun createGroovyRootAndMoveFile(project: Project, currentFile: VirtualFile,
                                    postGroovyRootCreationActions: (Project, PsiFile) -> Unit) {
        val groovySourceRoot = createGroovySourceRoot(project, currentFile)

        if (groovySourceRoot != null) {
            /*  creating groovySourceRoot creates indexing therefore the following
                actions must be deferred
             */
            DumbService.getInstance(project).runWhenSmart {
                renameAndMoveToGroovy(currentFile, groovySourceRoot, project)
                // old PSI is wrong as moved and renamed
                val groovyPsiFile = requireNotNull(PsiManager.getInstance(project).findFile(currentFile))

                postGroovyRootCreationActions(project, groovyPsiFile)
            }

            // no action must occur here
        }
    }

    private fun createGroovySourceRoot(project: Project?, currentFile: VirtualFile): VirtualFile? {
        val sourceRootForCurrentFile = getSourceRootForCurrentFile(project, currentFile)
        if (sourceRootForCurrentFile.isPresent) {
            val file = sourceRootForCurrentFile.get()
            val sourceDirectory = file.parent

            val groovyRoot = sourceDirectory.findChild("groovy")
            if (groovyRoot != null && groovyRoot.exists()) {
                return groovyRoot
            }

            val yesNo = Messages
                    .showYesNoDialog(project, "Groovy source root is not present, do you want to create it?", "Groovyfier", Messages
                            .getQuestionIcon())
            if (yesNo == Messages.NO) {
                return null
            }
            try {
                WriteAction.run<IOException> { sourceDirectory.createChildDirectory(this, "groovy") }
            } catch (e: IOException) {
                val message = "Error while creating groovy directory"
                Messages.showErrorDialog(e.message, message)
                LOG.error(message, e)
            }

            val createdGroovyRoot = sourceDirectory.findChild("groovy")
            val module = getCurrentModule(project)
            ModuleRootModificationUtil.updateModel(module) { modifiableRootModel ->
                val contentEntries = modifiableRootModel.contentEntries
                assert(contentEntries.size == 1) // I'm not sure what's the use case for several content entries
                contentEntries[0].addSourceFolder(createdGroovyRoot!!, true)
            }
            createdGroovyRoot!!.refresh(false, false)

            return createdGroovyRoot
        } else {
            // TODO exception handling
        }
        return null
    }

    private fun getCurrentModule(project: Project?): Module {
        // FIXME determine correct module
        return ModuleManager.getInstance(project!!).modules[0]
    }

    private fun getSourceRootForCurrentFile(project: Project?, currentFile: VirtualFile): Optional<VirtualFile> {
        val projectRootManager = ProjectRootManager.getInstance(project!!)
        val sourceRoots = projectRootManager.contentSourceRoots
        return Arrays.stream(sourceRoots).filter { sourceRoot -> VfsUtilCore.isAncestor(sourceRoot, currentFile, true) }.findAny()
    }

    private fun renameAndMoveToGroovy(currentFile: VirtualFile, groovySourcesRoot: VirtualFile, project: Project?) {
        assert(groovySourcesRoot.exists())

        val sourceRootForCurrentFile = getSourceRootForCurrentFile(project, currentFile)

        assert(sourceRootForCurrentFile.isPresent)

        val relativePathForPackageName = VfsUtilCore
                .getRelativePath(currentFile.parent, sourceRootForCurrentFile.get(), '.')

        try {
            WriteAction.run<IOException> {
                val groovyFilename = currentFile.name.replace(".java", ".groovy")
                currentFile.rename(this, groovyFilename)
                var lastCreatedDir = groovySourcesRoot

                for (packageElement in relativePathForPackageName!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    val childWithPackageName = lastCreatedDir.findChild(packageElement)
                    lastCreatedDir = if (childWithPackageName != null && childWithPackageName.isDirectory) {
                        childWithPackageName
                    } else {
                        lastCreatedDir.createChildDirectory(this, packageElement)
                    }
                }
                currentFile.move(this, lastCreatedDir)
            }
        } catch (e: IOException) {
            LOG.error(e) // fixme macht es Sinn hier noch weiter zu machen?
        }

    }

}