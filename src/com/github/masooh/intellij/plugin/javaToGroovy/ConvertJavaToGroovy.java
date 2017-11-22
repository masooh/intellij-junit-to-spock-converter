package com.github.masooh.intellij.plugin.javaToGroovy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.refactoring.actions.MoveAction;

/**
 * @author masooh
 */
public class ConvertJavaToGroovy extends AnAction {
	private static final Logger LOG = Logger.getInstance(ConvertJavaToGroovy.class);

	@Override
	public void actionPerformed(AnActionEvent event) {
//		Project project = event.getProject();
//		VirtualFile currentFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
//		VirtualFile grovoySourceRoot = createGrovoySourceRoot(project, currentFile);
//
//		ActionUtil.performActionDumbAware(new MoveAction(), event);
//
//		rename(currentFile, grovoySourceRoot);

//		applyGroovyStyle(event);

		Object navigatable = event.getData(CommonDataKeys.NAVIGATABLE);
		if (navigatable != null) {
			Messages.showDialog(navigatable.toString(), "Selected Element:", new String[]{"OK"}, -1, null);
		}
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);
		// TODO check if Groovy/Java file - Test with XML
//		e.getPresentation().setEnabled(false);
	}

	private void applyGroovyStyle(AnActionEvent event) {
		Document document = event.getRequiredData(PlatformDataKeys.EDITOR).getDocument();
		PsiFile psiFile = event.getRequiredData(PlatformDataKeys.PSI_FILE);
		PsiElement element = psiFile.getFirstChild();

		Arrays.stream(IntentionManager.getInstance().getAvailableIntentionActions()).filter(
				intentionAction -> intentionAction.getFamilyName().contains("groovy")
		).forEach(System.out::println);

		psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
			@Override
			public void visitElement(PsiElement element) {
				super.visitElement(element);

				System.out.println(element.toString());
			}
		});
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

	private VirtualFile createGrovoySourceRoot(Project project, VirtualFile currentFile) {
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
