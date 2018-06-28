package com.github.masooh.intellij.plugin.groovyfier;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.GroovyFileType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author masooh
 */
public class ConvertJavaToGroovy extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConvertJavaToGroovy.class);
    private final GroovyFixesApplier groovyFixesApplier = new GroovyFixesApplier();
    private final JUnitToSpockApplier jUnitToSpockApplier = new JUnitToSpockApplier();

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        VirtualFile currentFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);

        if ("java".equals(currentFile.getExtension())) {
            VirtualFile groovySourceRoot = createGroovySourceRoot(project, currentFile);

            if (groovySourceRoot != null) {
                renameAndMoveToGroovy(currentFile, groovySourceRoot, project);
            }
        }

        if ("groovy".equals(currentFile.getExtension())) {
            groovyFixesApplier.applyGroovyFixes(event);

            jUnitToSpockApplier.transformToSpock(event);
        }

    }

    @Override
    public void update(AnActionEvent event) {
        PsiFile file = event.getData(PlatformDataKeys.PSI_FILE);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);

        boolean enabled = file != null &&
                editor != null &&
                (
                        JavaFileType.INSTANCE.equals(file.getFileType()) ||
                                Arrays.asList(GroovyFileType.getGroovyEnabledFileTypes()).contains(file.getFileType())
                );

        event.getPresentation().setEnabled(enabled);
    }

    private void renameAndMoveToGroovy(VirtualFile currentFile, VirtualFile groovySourcesRoot, Project project) {
        assert groovySourcesRoot != null;
        assert groovySourcesRoot.exists();

        Optional<VirtualFile> sourceRootForCurrentFile = getSourceRootForCurrentFile(project, currentFile);

        assert sourceRootForCurrentFile.isPresent();

        String relativePathForPackageName = VfsUtilCore.getRelativePath(currentFile.getParent(), sourceRootForCurrentFile.get(), '.');

        try {
            WriteAction.run(() -> {
                        String groovyFilename = currentFile.getName().replace(".java", ".groovy");
                        currentFile.rename(this, groovyFilename);
                        VirtualFile lastCreatedDir = groovySourcesRoot;

                        for (String packageElement : relativePathForPackageName.split("\\.")) {
                            lastCreatedDir = lastCreatedDir.findOrCreateChildData(this, packageElement);
                        }
                        currentFile.move(this, lastCreatedDir);
                    }
            );
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private VirtualFile createGroovySourceRoot(Project project, VirtualFile currentFile) {
        Optional<VirtualFile> sourceRootForCurrentFile = getSourceRootForCurrentFile(project, currentFile);
        if (sourceRootForCurrentFile.isPresent()) {
            VirtualFile file = sourceRootForCurrentFile.get();
            VirtualFile sourceDirectory = file.getParent();

            VirtualFile groovyRoot = sourceDirectory.findChild("groovy");
            if (groovyRoot != null && groovyRoot.exists()) {
                return groovyRoot;
            }

            int yesNo = Messages.showYesNoDialog(project, "Groovy source root is not present, do you want to create it?", "Groovyfier", Messages.getQuestionIcon());
            if (yesNo == Messages.NO) {
                return null;
            }
            try {
                WriteAction.run(() -> sourceDirectory.createChildDirectory(this, "groovy"));
            } catch (IOException e) {
                String message = "Error while creating groovy directory";
                Messages.showErrorDialog(e.getMessage(), message);
                LOG.error(message, e);
            }

            final VirtualFile createdGroovyRoot = sourceDirectory.findChild("groovy");
            Module module = getCurrentModule(project);
            ModuleRootModificationUtil.updateModel(module, modifiableRootModel -> {
                ContentEntry[] contentEntries = modifiableRootModel.getContentEntries();
                assert contentEntries.length == 1; // I'm not sure what's the use case for several content entries
                contentEntries[0].addSourceFolder(createdGroovyRoot, true);
            });
            createdGroovyRoot.refresh(false, true);
            return createdGroovyRoot;
        } else {
            // TODO exception handling
        }
        return null;
    }

    private Module getCurrentModule(Project project) {
        // FIXME determine correct module
        return ModuleManager.getInstance(project).getModules()[0];
    }

    private Optional<VirtualFile> getSourceRootForCurrentFile(Project project, VirtualFile currentFile) {
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        VirtualFile[] sourceRoots = projectRootManager.getContentSourceRoots();
        return Arrays.stream(sourceRoots).filter(sourceRoot -> VfsUtilCore.isAncestor(sourceRoot, currentFile, true)).findAny();
    }
}
