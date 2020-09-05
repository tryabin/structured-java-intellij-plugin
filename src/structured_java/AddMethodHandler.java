package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.util.Arrays;
import java.util.List;

import static structured_java.Utilities.getCurrentClass;

public class AddMethodHandler implements EventHandler<ActionEvent> {

    private StructuredJavaToolWindowFactoryJavaFX ui;

    public AddMethodHandler(StructuredJavaToolWindowFactoryJavaFX ui) {
        this.ui = ui;
    }

    @Override
    public void handle(ActionEvent event) {
        
        // Get the class methods.
        PsiClass currentClass = getCurrentClass(ui.getProject());
        PsiMethod[] psiMethods = ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) currentClass::getMethods);
        List<PsiMethod> methods = Arrays.asList(psiMethods);
        PsiField[] psiFields = ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) currentClass::getFields);
        List<PsiField> variables = Arrays.asList(psiFields);

        // Modify the source code to add the method.
        WriteCommandAction.writeCommandAction(ui.getProject()).run(() -> {
            int offsetToInsertMethod;

            // Insert the method after the last method if it exists.
            if (methods.size() > 0) {
                PsiMethod lastMethod = methods.get(methods.size() - 1);
                offsetToInsertMethod = lastMethod.getTextRange().getEndOffset();
            }

            // Otherwise insert the method after the last variable if it exists.
            else if (variables.size() > 0) {
                PsiField lastVariable = variables.get(variables.size() - 1);
                offsetToInsertMethod = lastVariable.getTextRange().getEndOffset();
            }

            // Otherwise add the method on the line after the class left brace.
            else {
                offsetToInsertMethod = currentClass.getLBrace().getTextOffset() + 1;
            }

            // Add the new method to the class.
            Editor editor =  FileEditorManager.getInstance(ui.getProject()).getSelectedTextEditor();
            String methodTextToInsert = "\n\n    " + ui.getNewMethodText() + "\n";
            editor.getDocument().insertString(offsetToInsertMethod, methodTextToInsert);
        });

        // Wait until the number of methods in the class changes.
        Utilities.waitForNumberOfMethodsInClassToChange(psiMethods.length, ui);

        // Focus on the row of the new method.
        KeyboardFocusInfo focusInfo = ui.getKeyboardFocusInfo();
        focusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);

        // Rebuild the UI.
        ui.buildClassOutlineScene();
        ui.setSceneToClassOutline();
    }
}
