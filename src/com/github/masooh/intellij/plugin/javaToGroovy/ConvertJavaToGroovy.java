package com.github.masooh.intellij.plugin.javaToGroovy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Martin Hofmann-Sobik
 */
public class ConvertJavaToGroovy extends AnAction {
	private static final Logger LOG = Logger.getInstance(ConvertJavaToGroovy.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = event.getProject();
		VirtualFile currentFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
		createGrovoySourceRoot(project, currentFile);
	}

	private void createGrovoySourceRoot(Project project, VirtualFile currentFile) {
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
					return;
				}
				try {
					WriteAction.run(() -> sourceDirectory.createChildDirectory(this, "groovy"));
				} catch (IOException e) {
					String message = "Error while creating groovy directory";
					Messages.showErrorDialog(e.getMessage(), message);
					LOG.error(message, e);
				}
			}

		}
	}
}
