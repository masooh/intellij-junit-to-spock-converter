package com.github.masooh.intellij.plugin.javaToGroovy;

import java.io.IOException;
import java.util.*;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.actions.AbstractPerformFixesTask;
import com.intellij.codeInspection.actions.CleanupInspectionIntention;
import com.intellij.codeInspection.actions.CleanupInspectionUtil;
import com.intellij.codeInspection.ex.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.SequentialModalProgressTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author masooh
 */
public class ConvertJavaToGroovy extends AnAction {
	private static final Logger LOG = Logger.getInstance(ConvertJavaToGroovy.class);

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

		PsiFile file = event.getRequiredData(PlatformDataKeys.PSI_FILE);
		Editor editor = event.getRequiredData(PlatformDataKeys.EDITOR);

		final List<InspectionToolWrapper> inspectionToolWrappers = InspectionToolRegistrar.getInstance().get();

		/*
		inspectionToolWrappers.stream()
        .filter(inspectionToolWrapper -> inspectionToolWrapper.getLanguage() != null && inspectionToolWrapper.getLanguage().equalsIgnoreCase("groovy"))
        .collect(Collectors.toList());
		 */

        Optional<InspectionToolWrapper> groovySemicolon = inspectionToolWrappers.stream()
//                .filter(inspectionToolWrapper -> inspectionToolWrapper.getDisplayName().contains("semicolon"))
                .filter(inspectionToolWrapper -> inspectionToolWrapper.getShortName().equals("JavaStylePropertiesInvocation"))
                .filter(inspectionToolWrapper -> inspectionToolWrapper.getLanguage().equals("Groovy"))
                .findAny();


        if (groovySemicolon.isPresent()) {

            InspectionToolWrapper inspectionToolWrapper = groovySemicolon.get();

            // inspired by CleanupInspectionIntention
            final List<ProblemDescriptor> descriptions =
                    ProgressManager.getInstance().runProcess(() -> {
                        InspectionManager inspectionManager = InspectionManager.getInstance(project);
                        // TODO prüfen, ob es andere Methode gibt, die mehrere inspectionToolWrapper nehmen kann
                        // TODO import keymap from Ultimate
                        return InspectionEngine.runInspectionOnFile(file, groovySemicolon.get(), inspectionManager.createNewGlobalContext(false));
                    }, new EmptyProgressIndicator());

            if (!descriptions.isEmpty() && !FileModificationService.getInstance().preparePsiElementForWrite(file)) return;

            System.out.println(descriptions.size());
            System.out.println(descriptions);

            AbstractPerformFixesTask fixesTask =
                    CleanupInspectionUtil.getInstance().applyFixes(project, "Apply Fixes", descriptions,
                            null, false);

            String myText = "TODO";

            if (!fixesTask.isApplicableFixFound()) {
                HintManager.getInstance().showErrorHint(editor, "Unfortunately '" + myText + "' is currently not available for batch mode\n User interaction is required for each problem found");
            }
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

		PsiClass psiClass = getPsiClass(psiFile);

		PsiMethod methodFromText = JavaPsiFacade.getElementFactory(event.getProject()).createMethodFromText("void method() ...", psiClass);
		PsiElement addedMethod = psiClass.add(methodFromText);
		JavaCodeStyleManager.getInstance(event.getProject()).shortenClassReferences(addedMethod);
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
