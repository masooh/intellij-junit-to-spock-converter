package com.github.masooh.intellij.plugin.groovyfier;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.actions.AbstractPerformFixesTask;
import com.intellij.codeInspection.actions.CleanupInspectionUtil;
import com.intellij.codeInspection.ex.InspectionToolRegistrar;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroovyFixesApplier {
    public GroovyFixesApplier() {
    }

    void applyGroovyFixes(AnActionEvent event, Project project) {
        PsiFile file = event.getRequiredData(PlatformDataKeys.PSI_FILE);
        Editor editor = event.getRequiredData(PlatformDataKeys.EDITOR);

        final List<InspectionToolWrapper> inspectionToolWrappers = InspectionToolRegistrar.getInstance().get();

        final List<LocalInspectionToolWrapper> wrapperList = inspectionToolWrappers.stream()
                .filter(inspectionToolWrapper -> inspectionToolWrapper.getLanguage() != null && inspectionToolWrapper.getLanguage().equalsIgnoreCase("groovy"))
                .filter(wrapper -> wrapper.getGroupDisplayName().equalsIgnoreCase("GPath") || wrapper.getGroupDisplayName().equalsIgnoreCase("Style"))
                .filter(wrapper -> wrapper instanceof LocalInspectionToolWrapper)
                .map(wrapper -> (LocalInspectionToolWrapper) wrapper)
                .collect(Collectors.toList());


        if (!wrapperList.isEmpty()) {
            // inspired by CleanupInspectionIntention
            final Map<String, List<ProblemDescriptor>> problemDescriptors = ProgressManager.getInstance().runProcess(() -> {
                InspectionManager inspectionManager = InspectionManager.getInstance(project);
                return InspectionEngine.inspectEx(wrapperList, file, inspectionManager, false, false, new EmptyProgressIndicator());
            }, new EmptyProgressIndicator());

            final List<ProblemDescriptor> descriptions = new ArrayList<ProblemDescriptor>();
            for (List<ProblemDescriptor> group : problemDescriptors.values()) {
                descriptions.addAll(group);
            }

            if (!descriptions.isEmpty() && !FileModificationService.getInstance().preparePsiElementForWrite(file))
                return;

            AbstractPerformFixesTask fixesTask =
                    CleanupInspectionUtil.getInstance().applyFixes(project, "Apply Groovy Fixes", descriptions,
                            null, false);

            if (!fixesTask.isApplicableFixFound()) {
                HintManager.getInstance().showErrorHint(editor, "Unfortunately Groovy fixes are currently not available for batch mode\n User interaction is required for each problem found.");
            }
        }
    }
}