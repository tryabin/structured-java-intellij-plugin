package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

import static structured_java.Utilities.getCurrentClass;

public class AddVariableHandler implements EventHandler<ActionEvent> {

    private Project project;
    private ClassOutlineScene classOutlineScene;

    public AddVariableHandler(Project project, ClassOutlineScene classOutlineScene) {
        this.project = project;
        this.classOutlineScene = classOutlineScene;
    }

    @Override
    public void handle(ActionEvent event) {
        
        // Get the class variables.
        PsiClass currentClass = getCurrentClass(project);
        PsiField[] psiFields = ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) currentClass::getFields);
        List<PsiField> variables = Arrays.asList(psiFields);

        // Modify the source code to add the variable.
        WriteCommandAction.writeCommandAction(project).run(() -> {
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
            String variableTextToInsert = "\n    " + classOutlineScene.getNewVariableSourceText();
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            editor.getDocument().insertString(offsetToInsertVariable, variableTextToInsert);
        });

        // Wait until the number of variables in the class changes.
        Utilities.waitForNumberOfVariablesInClassToChange(psiFields.length, currentClass);

        // Focus on the row of the new variable.
        KeyboardFocusInfo focusInfo = classOutlineScene.getKeyboardFocusInfo();
        focusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);

        // Rebuild the class outline scene.
        classOutlineScene.buildClassOutlineScene();
    }
}
