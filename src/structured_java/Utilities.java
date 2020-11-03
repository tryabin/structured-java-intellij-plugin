package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import javafx.scene.text.Font;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    protected static PsiClass getCurrentClass(Project project) {
        // Get the currently selected file.
        FileEditorManager manager = FileEditorManager.getInstance(project);
        VirtualFile[] files = manager.getSelectedFiles();
        VirtualFile currentFile = files[0];

        // Get the PsiClass for the currently selected file.
        final PsiClass[] curClass = new PsiClass[1];
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            curClass[0] = psiJavaFile.getClasses()[0];
        });

        return curClass[0];
    }


    protected static void waitForNumberOfVariablesInClassToChange(int originalNumberOfVariables, PsiClass currentClass) {
        int pollTimeMs = 50;
        while (ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) currentClass::getFields).length == originalNumberOfVariables) {
            try {
                Thread.sleep(pollTimeMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    protected static void waitForNumberOfMethodsInClassToChange(int originalNumberOfMethods, PsiClass currentClass) {
        int pollTimeMs = 50;
        while (ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) () -> getCurrentMethods(currentClass)).length == originalNumberOfMethods) {
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


    protected static PsiMethod[] getCurrentMethods(PsiClass currentClass) {
        PsiMethod[] psiMethods = ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) currentClass::getMethods);
        return psiMethods;
    }


    protected static PsiField[] getCurrentVariables(PsiClass currentClass) {
        PsiField[] psiFields = ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) currentClass::getFields);
        return psiFields;
    }


    protected static Font getDefaultFont(Project project) {
        // return ApplicationManager.getApplication().runReadAction((Computable<Font>) () -> {
        //     Editor editor = EditorFactory.getInstance().createEditor(FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument(), project);
        //     String defaultFontName = editor.getColorsScheme().getEditorFontName();
        //     int defaultFontSize = editor.getColorsScheme().getEditorFontSize();
        //     return Font.font(defaultFontName, defaultFontSize);
        // });
        // return Font.font("Monospaced");
        return Font.font("Consolas");
    }


    protected static Font getDefaultEditorFont(Project project) {
        return ApplicationManager.getApplication().runReadAction((Computable<Font>) () -> {
            Editor editor = EditorFactory.getInstance().createEditor(FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument(), project);
            String defaultFontName = editor.getColorsScheme().getEditorFontName();
            int defaultFontSize = editor.getColorsScheme().getEditorFontSize();
            return Font.font(defaultFontName, defaultFontSize);
        });
    }
}
