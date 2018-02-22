package com.github.masooh.intellij.plugin.groovyfier;

import java.io.IOException;
import java.util.*;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.plugins.groovy.GroovyFileType;

/**
 * @author masooh
 */
public class ConvertJavaToGroovy extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConvertJavaToGroovy.class);
    private final GroovyFixesApplier groovyFixesApplier = new GroovyFixesApplier();

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
//		VirtualFile currentFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
//		VirtualFile grovoySourceRoot = createGrovoySourceRoot(project, currentFile);
//
//		ActionUtil.performActionDumbAware(new MoveAction(), event);
//
//		rename(currentFile, grovoySourceRoot);

//		applyGroovyStyle(event);

//		Object navigatable = event.getData(CommonDataKeys.NAVIGATABLE);
//		if (navigatable != null) {
//			Messages.showDialog(navigatable.toString(), "Selected Element:", new String[]{"OK"}, -1, null);
//		}

        groovyFixesApplier.applyGroovyFixes(event, project);
    }

    @Override
    public void update(AnActionEvent event) {
        PsiFile file = event.getRequiredData(PlatformDataKeys.PSI_FILE);
        Editor editor = event.getRequiredData(PlatformDataKeys.EDITOR);
        FileType fileType = file.getFileType();

        boolean enabled = file != null && editor != null &&
                Arrays.asList(GroovyFileType.getGroovyEnabledFileTypes()).contains(fileType);

		event.getPresentation().setEnabled(enabled);
    }

    private PsiClass getPsiClass(PsiFile psiFile) {
        if (psiFile instanceof PsiJavaFile) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            PsiClass[] psiClasses = psiJavaFile.getClasses();
            if (psiClasses.length == 1) {
                PsiClass psiClass = psiClasses[0];
                return psiClass;
            }
        }
        return null;
    }

    private void rename(VirtualFile currentFile, VirtualFile grovoySourceRoot) {
        // TODO exception werfen in Methode, ? welche und wo fangen
        if (grovoySourceRoot == null || !grovoySourceRoot.exists()) {
            return;
        }

        try {
            WriteAction.run(() -> {
                        String newName = currentFile.getName().replace(".java", ".groovy");
                        currentFile.rename(this, newName);
                    }
            );
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private VirtualFile createGroovySourceRoot(Project project, VirtualFile currentFile) {
        VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        Optional<VirtualFile> sourceRootForCurrentFile =
                Arrays.stream(sourceRoots).filter(sourceRoot -> currentFile.getPath().startsWith(sourceRoot.getPath())).findAny();
        if (sourceRootForCurrentFile.isPresent()) {
            VirtualFile file = sourceRootForCurrentFile.get();
            VirtualFile sourceDirectory = file.getParent();
            VirtualFile groovyRoot = sourceDirectory.findChild("groovy");
            if (groovyRoot == null || !groovyRoot.exists()) {
                int yesNo = Messages.showYesNoCancelDialog(project, "groovy source root is not present, do you want to create it?", "JavaToGroovy", Messages.getQuestionIcon());
                if (yesNo == Messages.NO) {
                    return file;
                }
                try {
                    WriteAction.run(() -> sourceDirectory.createChildDirectory(this, "groovy"));
                } catch (IOException e) {
                    String message = "Error while creating groovy directory";
                    Messages.showErrorDialog(e.getMessage(), message);
                    LOG.error(message, e);
                }
            }
            groovyRoot.refresh(false, false);
            return groovyRoot;
        }
        return null;
    }
}
