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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroovyFixesApplier {
    void applyGroovyFixes(AnActionEvent event) {
        Project project = event.getProject();
        PsiFile file = event.getRequiredData(PlatformDataKeys.PSI_FILE);
        Editor editor = event.getRequiredData(PlatformDataKeys.EDITOR);

        final List<LocalInspectionToolWrapper> groovyInspections = findGroovyInspections();

        if (!groovyInspections.isEmpty()) {
            // inspired by CleanupInspectionIntention
            final List<ProblemDescriptor> problemDescriptors = inspectFileForProblems(project, file, groovyInspections);

            if (problemDescriptors.isEmpty()) {
                return;
            }

            if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) {
                HintManager.getInstance().showErrorHint(editor, "Cannot write PSI element.");
                return;
            }

            AbstractPerformFixesTask fixesTask =
                    CleanupInspectionUtil.getInstance().applyFixes(project, "Apply Groovy Fixes", problemDescriptors,
                            null, false);

            if (!fixesTask.isApplicableFixFound()) {
                HintManager.getInstance().showErrorHint(editor, "Unfortunately Groovy fixes are currently not available for batch mode\n User interaction is required for each problem found.");
            }
        }
    }

    @NotNull
    private List<ProblemDescriptor> inspectFileForProblems(Project project, PsiFile file, List<LocalInspectionToolWrapper> groovyInspections) {
        final Map<String, List<ProblemDescriptor>> problemDescriptors = ProgressManager.getInstance().runProcess(() -> {
            InspectionManager inspectionManager = InspectionManager.getInstance(project);
            return InspectionEngine.inspectEx(groovyInspections, file, inspectionManager, false, false, new EmptyProgressIndicator());
        }, new EmptyProgressIndicator());

        final List<ProblemDescriptor> descriptions = new ArrayList<>();
        for (List<ProblemDescriptor> group : problemDescriptors.values()) {
            descriptions.addAll(group);
        }
        return descriptions;
    }

    private List<LocalInspectionToolWrapper> findGroovyInspections() {
        final List<InspectionToolWrapper> inspectionToolWrappers = InspectionToolRegistrar.getInstance().get();
        return inspectionToolWrappers.stream()
                .filter(inspectionToolWrapper -> inspectionToolWrapper.getLanguage() != null && inspectionToolWrapper.getLanguage().equalsIgnoreCase("groovy"))
                .filter(wrapper -> wrapper.getGroupDisplayName().equalsIgnoreCase("GPath") || wrapper.getGroupDisplayName().equalsIgnoreCase("Style"))
                .filter(wrapper -> wrapper instanceof LocalInspectionToolWrapper)
                .map(wrapper -> (LocalInspectionToolWrapper) wrapper)
                .collect(Collectors.toList());
    }
}