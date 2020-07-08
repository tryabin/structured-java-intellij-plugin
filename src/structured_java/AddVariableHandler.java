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
        PsiField lastVariable = variables.get(variables.size() - 1);

        // Modify the source code to add the variable.
        WriteCommandAction.writeCommandAction(ui.getProject()).run(() -> {

            // Get the class source text.
            Editor editor =  FileEditorManager.getInstance(ui.getProject()).getSelectedTextEditor();
            String sourceText = editor.getDocument().getText();

            // Find the semicolon at the end of the definition of the last variable.
            int offset = lastVariable.getTextOffset();
            boolean previousCharIsFirstBackslashInComment = false;
            boolean previousCharIsAsterisk = false;
            boolean inLineComment = false;
            boolean inBlockComment = false;
            while (offset < sourceText.length()) {
                char curChar = sourceText.charAt(offset);

                // Return if we found an uncommented semicolon.
                if (!inLineComment && !inBlockComment && curChar == ';') {
                    break;
                }

                // Check to see if we are no longer in a block comment.
                if (inBlockComment && previousCharIsAsterisk && curChar == '/') {
                    inBlockComment = false;
                    continue;
                }

                // Check if the current character makes subsequent text commented out.
                if (previousCharIsFirstBackslashInComment && curChar == '/') {
                    inLineComment = true;
                    continue;
                }
                else if (previousCharIsFirstBackslashInComment && curChar == '*') {
                    inBlockComment = true;
                    continue;
                }

                // If we find a backslash make note of it.
                if (curChar == '/') {
                    previousCharIsFirstBackslashInComment = true;
                    continue;
                }

                // Check to see if we could be in the end of a block comment.
                if (curChar == '*') {
                    previousCharIsAsterisk = true;
                    continue;
                }

                // Check to see if we are no longer in a line comment.
                if (inLineComment && curChar == '\n') {
                    inLineComment = false;
                    continue;
                }

                // Reset the "previousChar" variables if we get here.
                previousCharIsAsterisk = previousCharIsFirstBackslashInComment = false;

                offset++;
            }

            // Add the new variable to the class.
            String variableTextToInsert = "\n    " + ui.getNewVariableField().getText();
            editor.getDocument().insertString(offset + 1, variableTextToInsert);
        });

        // Wait until the number of variables in the class changes.
        Utilities.waitForNumberOfVariablesInClassToChange(psiFields.length, ui);

        // Focus on the row of the new variable.
        KeyboardFocusInfo focusInfo = ui.getKeyboardFocusInfo();
        focusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);

        // Rebuild the UI.
        ui.buildClassOutlineScene();
        ui.setSceneToClassOutline();
    }
}
