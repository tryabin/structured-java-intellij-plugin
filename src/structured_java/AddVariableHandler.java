package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.util.Arrays;
import java.util.List;

import static structured_java.Utilities.getCurrentClass;

public class AddVariableHandler implements EventHandler<ActionEvent> {

    private StructuredJavaToolWindowFactoryJavaFX ui;

    public AddVariableHandler(StructuredJavaToolWindowFactoryJavaFX ui) {
        this.ui = ui;
    }

    @Override
    public void handle(ActionEvent event) {
        
        // Get the class variables.
        PsiClass currentClass = getCurrentClass(ui.getProject());
        PsiField[] psiFields = ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) currentClass::getFields);
        List<PsiField> variables = Arrays.asList(psiFields);

        // Modify the source code to add the variable.
        WriteCommandAction.writeCommandAction(ui.getProject()).run(() -> {
            int offsetToInsertVariable;

            // Add the variable after the last variable if it exists.
            if (variables.size() > 0) {
                PsiField lastVariable = variables.get(variables.size() - 1);
                offsetToInsertVariable = lastVariable.getTextRange().getEndOffset();
            }

            // Otherwise add the variable on the line after the class left brace.
            else {
                offsetToInsertVariable = currentClass.getLBrace().getTextOffset() + 1;
            }

            // Add the new variable to the class.
            String variableTextToInsert = "\n    " + ui.getClassOutlineScene().getNewVariableSourceText();
            Editor editor = FileEditorManager.getInstance(ui.getProject()).getSelectedTextEditor();
            editor.getDocument().insertString(offsetToInsertVariable, variableTextToInsert);
        });

        // Wait until the number of variables in the class changes.
        Utilities.waitForNumberOfVariablesInClassToChange(psiFields.length, ui);

        // Focus on the row of the new variable.
        KeyboardFocusInfo focusInfo = ui.getKeyboardFocusInfo();
        focusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);

        // Rebuild the UI.
        ui.rebuildClassOutlineScene();
    }
}
