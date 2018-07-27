package com.github.masooh.intellij.plugin.groovyfier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotation;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationMemberValue;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
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
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameValuePair;

/**
 * @author masooh
 */
public class ConvertJavaToGroovy extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConvertJavaToGroovy.class);

    private final GroovyFixesApplier groovyFixesApplier = new GroovyFixesApplier();

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

            // must be the first action otherwise exception with TODO
            groovyFixesApplier.applyGroovyFixes(event);

            // TODO einzelne Replacer, die iteriert werden
            replaceCurlyBracesInAnnotationAttributes(event, project);


            /* TODO Groovy array handling
                List<IdentAttachmentMetaData> documents = new ArrayList<>()
                documents.add(test)

                -> List<IdentAttachmentMetaData> documents = [test]
             */
            /* TODO GString intent expression conversion
                FILE_LIST_PATH + ":0:documentDownload" -> GString (
             */

            /* TODO unit tests für Plugin, wie geht das?
                sonst zumindest Test-Datei hinterlegen, die funktioniert (BookTest)
             */
            /* TODO
                Plugin Beschreibung mit Mini-Video (GIF) o.ä., siehe IntelliJ new feature Videos
             */
            new JUnitToSpockApplier(event).transformToSpock();
        }

    }

    private void replaceCurlyBracesInAnnotationAttributes(AnActionEvent event, Project project) {
        PsiFile psiFile = event.getRequiredData(PlatformDataKeys.PSI_FILE);
        final GrTypeDefinition typeDefinition = (GrTypeDefinition) PsiHelper.getPsiClass(psiFile);

        WriteCommandAction
            .runWriteCommandAction(
                project,
                () -> {
                    // TODO java to Kotlin Umwandlung anschauen
                    // TODO alle Annotationen, nicht nur von Klasse

                    PsiAnnotation[] classAnnotations = typeDefinition.getAnnotations();
                    for (PsiAnnotation classAnnotation : classAnnotations) {
                        PsiNameValuePair[] attributes = classAnnotation.getParameterList().getAttributes();
                        if (attributes.length > 0) {
                            GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
                            GrAnnotation annotation = factory.createAnnotationFromText("@Annotation(att=[1])");
                            GrAnnotationMemberValue attributeValue = annotation.getParameterList().getAttributes()[0].getValue();

                            for (PsiNameValuePair attribute : attributes) {
                                PsiAnnotationMemberValue annotationMemberValue = attribute.getValue();

                                if ("{".equals(annotationMemberValue.getFirstChild().getText())
                                    && "}".equals(annotationMemberValue.getLastChild().getText())) {
                                    // { -> [
                                    annotationMemberValue.getFirstChild().replace(attributeValue.getFirstChild());

                                    // } -> ]
                                    annotationMemberValue.getLastChild().replace(attributeValue.getLastChild());
                                }
                            }
                        }
                    }
                }
            );
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

        String relativePathForPackageName = VfsUtilCore
            .getRelativePath(currentFile.getParent(), sourceRootForCurrentFile.get(), '.');

        try {
            WriteAction.run(() -> {
                                String groovyFilename = currentFile.getName().replace(".java", ".groovy");
                                currentFile.rename(this, groovyFilename);
                                VirtualFile lastCreatedDir = groovySourcesRoot;

                                for (String packageElement : relativePathForPackageName.split("\\.")) {
                                    VirtualFile childWithPackageName = lastCreatedDir.findChild(packageElement);
                                    if (childWithPackageName != null && childWithPackageName.isDirectory()) {
                                        lastCreatedDir = childWithPackageName;
                                    } else {
                                        lastCreatedDir = lastCreatedDir.createChildDirectory(this, packageElement);
                                    }
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

            int yesNo = Messages
                .showYesNoDialog(project, "Groovy source root is not present, do you want to create it?", "Groovyfier", Messages
                    .getQuestionIcon());
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
