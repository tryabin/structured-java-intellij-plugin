package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    protected static PsiClass getCurrentClass(Project p) {
        // Get the currently selected file.
        FileEditorManager manager = FileEditorManager.getInstance(p);
        VirtualFile[] files = manager.getSelectedFiles();
        VirtualFile currentFile = files[0];

        // Get the PsiClass for the currently selected file.
        final PsiClass[] curClass = new PsiClass[1];
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiFile psiFile = PsiManager.getInstance(p).findFile(currentFile);
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            curClass[0] = psiJavaFile.getClasses()[0];
        });

        return curClass[0];
    }


    protected static void waitForNumberOfVariablesInClassToChange(int originalNumberOfVariables, StructuredJavaToolWindowFactoryJavaFX ui) {
        int pollTimeMs = 50;
        while (ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) getCurrentClass(ui.getProject())::getFields).length == originalNumberOfVariables) {
            try {
                Thread.sleep(pollTimeMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    protected static void waitForNumberOfMethodsInClassToChange(int originalNumberOfMethods, StructuredJavaToolWindowFactoryJavaFX ui) {
        int pollTimeMs = 50;
        while (ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) getCurrentClass(ui.getProject())::getMethods).length == originalNumberOfMethods) {
            try {
                Thread.sleep(pollTimeMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    protected static int findOffsetOfSubstring(String text, String substring) {
        Pattern p = Pattern.compile(substring);
        Matcher matcher = p.matcher(text);
        matcher.find();
        return matcher.start();
    }
}
