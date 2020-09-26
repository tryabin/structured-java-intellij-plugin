package structured_java;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static structured_java.UserInterfaceUtilities.getField;

public class MethodEditingScene extends Scene implements EventHandler<KeyEvent> {

    private Project project;
    private PsiMethod method;
    private Button backButton;
    private List<ComboBox<String>> modifierBoxes = new ArrayList<>();
    private TextField returnTypeField;
    private List<TextField> parameterFields = new ArrayList<>();
    private TextField nameField;
    private Button addParameterButton;
    private TextArea methodTextArea;
    private Button saveMethodButton;
    private HBox methodRow;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public PsiMethod getMethod() {
        return method;
    }

    public void setMethod(PsiMethod method) {
        this.method = method;
    }

    public Button getBackButton() {
        return backButton;
    }

    public void setBackButton(Button backButton) {
        this.backButton = backButton;
    }

    public List<ComboBox<String>> getModifierBoxes() {
        return modifierBoxes;
    }

    public void setModifierBoxes(List<ComboBox<String>> modifierBoxes) {
        this.modifierBoxes = modifierBoxes;
    }

    public TextField getReturnTypeField() {
        return returnTypeField;
    }

    public void setReturnTypeField(TextField returnTypeField) {
        this.returnTypeField = returnTypeField;
    }

    public List<TextField> getParameterFields() {
        return parameterFields;
    }

    public void setParameterFields(List<TextField> parameterFields) {
        this.parameterFields = parameterFields;
    }

    public TextField getNameField() {
        return nameField;
    }

    public void setNameField(TextField nameField) {
        this.nameField = nameField;
    }

    public Button getAddParameterButton() {
        return addParameterButton;
    }

    public void setAddParameterButton(Button addParameterButton) {
        this.addParameterButton = addParameterButton;
    }

    public TextArea getMethodTextArea() {
        return methodTextArea;
    }

    public void setMethodTextArea(TextArea methodTextArea) {
        this.methodTextArea = methodTextArea;
    }

    public Button getSaveMethodButton() {
        return saveMethodButton;
    }

    public void setSaveMethodButton(Button saveMethodButton) {
        this.saveMethodButton = saveMethodButton;
    }

    public HBox getMethodRow() {
        return methodRow;
    }

    public void setMethodRow(HBox methodRow) {
        this.methodRow = methodRow;
    }


    public MethodEditingScene(VBox root, StructuredJavaToolWindowFactoryJavaFX ui) {
        super(root);
        this.project = ui.getProject();

        // Create an empty method editing scene.
        MethodData emptyMethodData = new MethodData();
        buildMethodEditingScene(root, emptyMethodData, ui);

        // Save method button
        saveMethodButton = new Button("Save Method");
        saveMethodButton.setOnAction(new AddMethodHandler(this, ui));
        root.getChildren().add(saveMethodButton);
    }

    public MethodEditingScene(VBox root, PsiMethod selectedMethod, StructuredJavaToolWindowFactoryJavaFX ui) {
        super (root);
        this.method = selectedMethod;
        this.project = ui.getProject();

        // Get the method data.
        MethodData methodData = new MethodData(method);
        buildMethodEditingScene(root, methodData, ui);

        // Add a text listener to the method editing text area so the source code is updated
        // as soon as the text in the method editing area changes.
        methodTextArea.textProperty().addListener((observable, oldValue, newValue) ->
        {
            WriteCommandAction.runWriteCommandAction(ui.getProject(), () -> {
                Editor editor = FileEditorManager.getInstance(ui.getProject()).getSelectedTextEditor();
                Document document = editor.getDocument();

                // Need to find the offset of the left bracket because the UI method text is just the body.
                int leftBracketOffset = Utilities.findOffsetOfSubstring(document.getText(method.getTextRange()), "\\{");

                // Replace the method text in the source file.
                String uiText = methodTextArea.getText();
                document.replaceString(method.getTextRange().getStartOffset() + leftBracketOffset, method.getTextRange().getEndOffset(), uiText);
            });
        });
    }

    private void buildMethodEditingScene(VBox root, MethodData methodData, StructuredJavaToolWindowFactoryJavaFX ui) {
        // Make the UI handle key events.
        addEventHandler(KeyEvent.KEY_PRESSED, this);

        // Back button
        backButton = new Button("Back");
        backButton.setOnAction(event -> ui.rebuildClassOutlineScene());
        root.getChildren().add(backButton);

        // The row containing method data.
        methodRow = new HBox();
        methodRow.setSpacing(5);

        // Modifier dropdowns
        for (String modifier : methodData.getModifiers()) {
            ComboBox<String> modifierBox = new ComboBox<>(FXCollections.observableArrayList(PsiModifier.MODIFIERS));
            modifierBox.getSelectionModel().select(modifier);
            methodRow.getChildren().add(modifierBox);
            modifierBoxes.add(modifierBox);
        }

        // Add modifier button
        Button addModifierButton = new Button("Add Modifier");
        methodRow.getChildren().add(addModifierButton);
        addModifierButton.setOnAction(event ->  {
            ComboBox<String> modifierBox = new ComboBox<>(FXCollections.observableArrayList(PsiModifier.MODIFIERS));
            methodRow.getChildren().add(modifierBoxes.size(), modifierBox);
            modifierBoxes.add(modifierBox);
            modifierBox.requestFocus();
        });

        // Type text field
        returnTypeField = getField(methodData.getReturnType());
        methodRow.getChildren().add(returnTypeField);

        // Name field
        nameField = getField(methodData.getName());
        methodRow.getChildren().add(nameField);

        // Parameters fields
        for (String parameter : methodData.getParameters()) {
            TextField parameterField = getField(parameter);
            parameterFields.add(parameterField);
            methodRow.getChildren().add(parameterField);
        }

        // Add parameter button
        addParameterButton = new Button("Add Parameter");
        methodRow.getChildren().add(addParameterButton);
        addParameterButton.setOnAction(event ->  {
            TextField newParameterTextField = getField("<Parameter Type and Name>");
            newParameterTextField.selectAll();
            methodRow.getChildren().add(methodRow.getChildren().size() - 1, newParameterTextField);
            parameterFields.add(newParameterTextField);
            newParameterTextField.requestFocus();
            newParameterTextField.selectAll();
        });

        root.getChildren().add(methodRow);

        // Method source text
        methodTextArea = new TextArea(methodData.getSourceText());
        root.getChildren().add(methodTextArea);
    }

    @Override
    public void handle(KeyEvent event) {

        Node focusOwner = focusOwnerProperty().get();

        // ENTER causes buttons to fire.
        if (event.getCode() == ENTER) {
            if (focusOwner instanceof Button) {
                ((Button) focusOwner).fire();
            }

            // Pressing ENTER anywhere in the method editing scene except a button or a
            // text area edits the method source code.
            else if (focusOwner != methodTextArea) {
                PsiMethod[] currentMethods = Utilities.getCurrentMethods(project);
                PsiField[] currentVariables = Utilities.getCurrentVariables(project);
                int originalNumberOfMethods = currentMethods.length;

                // Find the offset to insert the method in.
                int offsetToInsertNewMethod = 0;
                if (currentMethods.length > 0 && currentMethods[0] != method) {
                    for (PsiMethod currentMethod : currentMethods) {
                        if (currentMethod == method) {
                            break;
                        }
                        offsetToInsertNewMethod = currentMethod.getTextRange().getEndOffset();
                    }
                }
                else if (currentVariables.length > 0) {
                    PsiField lastVariable = currentVariables[currentVariables.length - 1];
                    offsetToInsertNewMethod = lastVariable.getTextRange().getEndOffset();
                }
                else {
                    offsetToInsertNewMethod = Utilities.getCurrentClass(project).getLBrace().getTextOffset() + 1;
                }

                // Delete the current method and then insert the new method in its place.
                WriteCommandAction.writeCommandAction(project).run(() -> method.delete());
                Utilities.waitForNumberOfMethodsInClassToChange(originalNumberOfMethods, project);
                AddMethodHandler.insertNewMethodText(this, offsetToInsertNewMethod);
            }
        }

        // DELETE deletes a parameter field if it is focused.
        if (event.getCode() == DELETE) {
            if (focusOwner instanceof TextField &&
                parameterFields.contains(focusOwner)) {

                parameterFields.remove(focusOwner);
                methodRow.getChildren().remove(focusOwner);
                addParameterButton.requestFocus();
            }
        }
    }


}
