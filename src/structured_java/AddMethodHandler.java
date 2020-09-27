package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static structured_java.Utilities.getCurrentClass;

public class AddMethodHandler implements EventHandler<ActionEvent> {

    private MethodEditingScene methodEditingScene;
    private StructuredJavaToolWindowFactoryJavaFX ui;


    public AddMethodHandler(MethodEditingScene methodEditingScene, StructuredJavaToolWindowFactoryJavaFX ui) {
        this.methodEditingScene = methodEditingScene;
        this.ui = ui;
    }


    @Override
    public void handle(ActionEvent event) {
        // Insert the text of the new method into the source code using the information in the method editing scene.
        int offsetToInsertMethod = getOffsetToAddNewMethod(methodEditingScene.getProject());
        insertNewMethodText(methodEditingScene, offsetToInsertMethod);

        // Focus on the row of the new method.
        KeyboardFocusInfo focusInfo = ui.getKeyboardFocusInfo();
        focusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);

        // Rebuild the UI.
        ui.buildClassOutlineScene();
        ui.setSceneToClassOutline();
    }


    public static int getOffsetToAddNewMethod(Project project) {

        // Get the class methods.
        PsiClass currentClass = getCurrentClass(project);
        PsiMethod[] psiMethods = ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) currentClass::getMethods);
        List<PsiMethod> methods = Arrays.asList(psiMethods);
        PsiField[] psiFields = ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) currentClass::getFields);
        List<PsiField> variables = Arrays.asList(psiFields);

        // Modify the source code to add the method.
        AtomicInteger offsetToInsertMethod = new AtomicInteger();
        WriteCommandAction.writeCommandAction(project).run(() -> {

            // Insert the method after the last method if it exists.
            if (methods.size() > 0) {
                PsiMethod lastMethod = methods.get(methods.size() - 1);
                offsetToInsertMethod.set(lastMethod.getTextRange().getEndOffset());
            }

            // Otherwise insert the method after the last variable if it exists.
            else if (variables.size() > 0) {
                PsiField lastVariable = variables.get(variables.size() - 1);
                offsetToInsertMethod.set(lastVariable.getTextRange().getEndOffset());
            }

            // Otherwise add the method on the line after the class left brace.
            else {
                offsetToInsertMethod.set(currentClass.getLBrace().getTextOffset() + 1);
            }
        });

        return offsetToInsertMethod.get();
    }

    public static void insertNewMethodText(MethodEditingScene methodEditingScene, int offsetToInsertMethod) {
        // Get the class methods.
        Project project = methodEditingScene.getProject();
        PsiClass currentClass = getCurrentClass(project);
        PsiMethod[] psiMethods = ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) currentClass::getMethods);

        // Modify the source code to add the method.
        WriteCommandAction.writeCommandAction(project).run(() -> {

            // Build the source text of the method.
            String methodTextToInsert = "\n\n    ";

            // Modifiers
            for (ComboBox<String> modifierBox : methodEditingScene.getModifierBoxes()) {
                methodTextToInsert += modifierBox.getValue() + " ";
            }

            // Return type
            methodTextToInsert += methodEditingScene.getReturnTypeField().getText() + " ";

            // Name
            methodTextToInsert += methodEditingScene.getNameField().getText();

            // Parameters
            {
                methodTextToInsert += "(";
                List<TextField> parameterFields = methodEditingScene.getParameterFields();

                // Get all non-empty parameters.
                List<String> nonEmptyParameters = new ArrayList<>();
                for (TextField parameterField : parameterFields) {
                    if (parameterField.getText().length() > 0) {
                        nonEmptyParameters.add(parameterField.getText());
                    }
                }

                // Add the parameters to the source text.
                for (int i = 0; i < nonEmptyParameters.size(); i++) {
                    String parameter = nonEmptyParameters.get(i);
                    methodTextToInsert += parameter;
                    if (i != nonEmptyParameters.size() - 1) {
                        methodTextToInsert += ", ";
                    }
                }
                methodTextToInsert += ")";
            }

            // Method body
            methodTextToInsert += methodEditingScene.getMethodTextArea().getText();

            // Add the new method to the class.
            Editor editor =  FileEditorManager.getInstance(project).getSelectedTextEditor();
            editor.getDocument().insertString(offsetToInsertMethod, methodTextToInsert);
        });

        // Wait until the number of methods in the class changes.
        Utilities.waitForNumberOfMethodsInClassToChange(psiMethods.length, project);
    }
}
