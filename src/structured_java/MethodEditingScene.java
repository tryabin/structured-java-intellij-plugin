package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static structured_java.UserInterfaceUtilities.getField;
import static structured_java.UserInterfaceUtilities.getStyleString;

public class MethodEditingScene extends Scene implements EventHandler<KeyEvent> {

    // Constants
    private static final int METHOD_HEADER_FONT_SIZE = 20;

    // The link back to the main UI
    private StructuredJavaToolWindowFactoryJavaFX ui;

    // Data elements
    private PsiMethod method;
    private boolean isForAddingNewMethod;

    // GUI components
    private Button backButton;
    private List<ComboBox<String>> modifierBoxes = new ArrayList<>();
    private TextField returnTypeField;
    private List<TextField> parameterFields = new ArrayList<>();
    private TextField nameField;
    private Button addParameterButton;
    private TextArea methodTextArea;
    private Button saveMethodButton;
    private HBox methodRow;

    // Formatting parameters
    private int initialIndexAmount;


    public StructuredJavaToolWindowFactoryJavaFX getUi() {
        return ui;
    }

    public void setUi(StructuredJavaToolWindowFactoryJavaFX ui) {
        this.ui = ui;
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

    public boolean isForAddingNewMethod() {
        return isForAddingNewMethod;
    }

    public void setForAddingNewMethod(boolean forAddingNewMethod) {
        isForAddingNewMethod = forAddingNewMethod;
    }

    public int getInitialIndexAmount() {
        return initialIndexAmount;
    }

    public void setInitialIndexAmount(int initialIndexAmount) {
        this.initialIndexAmount = initialIndexAmount;
    }


    public MethodEditingScene(VBox root, StructuredJavaToolWindowFactoryJavaFX ui) {
        super(root);
        this.ui = ui;

        // Make the UI handle key events.
        addEventHandler(KeyEvent.KEY_PRESSED, this);

        // Create an empty method editing scene.
        isForAddingNewMethod = true;
        MethodData emptyMethodData = new MethodData();
        buildMethodEditingScene(root, emptyMethodData);

        // Save method button
        saveMethodButton = new Button("Save Method");
        saveMethodButton.setOnAction(new AddMethodHandler(ui));
        root.getChildren().add(saveMethodButton);
    }


    public MethodEditingScene(VBox root, PsiMethod selectedMethod, StructuredJavaToolWindowFactoryJavaFX ui) {
        super (root);
        this.method = selectedMethod;
        this.ui = ui;

        // Make the UI handle key events.
        addEventHandler(KeyEvent.KEY_PRESSED, this);

        // Create a non-empty method editing scene.
        isForAddingNewMethod = false;
        MethodData methodData = new MethodData(method);
        buildMethodEditingScene(root, methodData);
    }


    private void buildMethodEditingScene(VBox root, MethodData methodData) {

        // Back button
        backButton = new Button("Back");
        backButton.setOnAction(event -> ui.setSceneToClassOutlineScene());
        root.getChildren().add(backButton);

        // Build the source parts.
        buildSourcePartsOfMethodEditingScene(root, methodData, ui.getProject());
    }

    private void buildSourcePartsOfMethodEditingScene(VBox root, MethodData methodData, Project project) {

        // Clear existing data.
        modifierBoxes.clear();
        parameterFields.clear();

        // The row containing method data.
        methodRow = new HBox();
        methodRow.setSpacing(5);

        // Modifier dropdowns
        String fieldStyle = getStyleString(ui.getDefaultFont().getName(), METHOD_HEADER_FONT_SIZE);
        for (String modifier : methodData.getModifiers()) {
            ComboBox<String> modifierBox = new ComboBox<>(FXCollections.observableArrayList(PsiModifier.MODIFIERS));
            modifierBox.getSelectionModel().select(modifier);
            modifierBox.setStyle(fieldStyle);
            methodRow.getChildren().add(modifierBox);
            modifierBoxes.add(modifierBox);

            // If the DELETE key is pressed on a modifier box then delete the box
            // and set the focus on the next element.
            modifierBox.setOnKeyPressed(event -> {
                if (event.getCode() == DELETE) {
                    for (int i = 0; i < methodRow.getChildren().size(); i++) {
                        Node child = methodRow.getChildren().get(i);
                        if (child == modifierBox) {
                            methodRow.getChildren().remove(modifierBox);
                            modifierBoxes.remove(modifierBox);
                            editMethodSource();
                            methodRow.getChildren().get(i).requestFocus();
                            break;
                        }
                    }
                }
            });
        }

        // Add modifier button
        Button addModifierButton = new Button("Add Modifier");
        addModifierButton.setStyle(fieldStyle);
        methodRow.getChildren().add(addModifierButton);
        addModifierButton.setOnAction(event ->  {
            ComboBox<String> modifierBox = new ComboBox<>(FXCollections.observableArrayList(PsiModifier.MODIFIERS));
            modifierBox.setStyle(fieldStyle);
            methodRow.getChildren().add(modifierBoxes.size(), modifierBox);
            modifierBoxes.add(modifierBox);
            modifierBox.requestFocus();
        });

        // Type text field
        returnTypeField = getField(methodData.getReturnType(), ui.getDefaultFont().getName(), METHOD_HEADER_FONT_SIZE);
        methodRow.getChildren().add(returnTypeField);

        // Name field
        nameField = getField(methodData.getName(), ui.getDefaultFont().getName(), METHOD_HEADER_FONT_SIZE);
        methodRow.getChildren().add(nameField);

        // Parameters fields
        for (String parameter : methodData.getParameters()) {
            TextField parameterField = getField(parameter, ui.getDefaultFont().getName(), METHOD_HEADER_FONT_SIZE);
            parameterFields.add(parameterField);
            methodRow.getChildren().add(parameterField);

            // If the DELETE key is pressed on a parameter field then delete the field
            // If the DELETE key is pressed on a parameter field then delete the field
            // and set the focus on the next element.
            parameterField.setOnKeyPressed(event -> {
                if (event.getCode() == DELETE) {
                    for (int i = 0; i < methodRow.getChildren().size(); i++) {
                        Node child = methodRow.getChildren().get(i);
                        if (child == parameterField) {
                            methodRow.getChildren().remove(parameterField);
                            parameterFields.remove(parameterField);
                            editMethodSource();
                            methodRow.getChildren().get(i).requestFocus();
                            break;
                        }
                    }
                }
            });
        }

        // Add parameter button
        addParameterButton = new Button("Add Parameter");
        addParameterButton.setStyle(fieldStyle);
        methodRow.getChildren().add(addParameterButton);
        addParameterButton.setOnAction(event ->  {
            TextField newParameterTextField = getField("<Parameter Type and Name>", ui.getDefaultFont().getName(), METHOD_HEADER_FONT_SIZE);
            newParameterTextField.selectAll();
            methodRow.getChildren().add(methodRow.getChildren().size() - 1, newParameterTextField);
            parameterFields.add(newParameterTextField);
            newParameterTextField.requestFocus();
            newParameterTextField.selectAll();
        });

        root.getChildren().add(methodRow);

        // Method source text editing field
        // Get the method source text and adjust it to not have brackets or starting indents.
        String methodTextAreaString = methodData.getSourceText();
        if (!isForAddingNewMethod) {
            String sourceText = methodData.getSourceText();
            String sourceTextNoBrackets = sourceText.substring(1, sourceText.length() - 1);
            List<String> lines = Arrays.asList(sourceTextNoBrackets.split("\n"));
            lines = lines.subList(1, lines.size()-1);
            initialIndexAmount = TextUtils.indexOf("[^\\s]", lines.get(0));
            methodTextAreaString = "";
            for (String line : lines) {
                methodTextAreaString += line.substring(initialIndexAmount) + "\n";
            }
        }
        methodTextArea = new TextArea(methodTextAreaString);

        // Set the method source text area font.
        methodTextArea.setFont(ui.getDefaultFont());
        root.getChildren().add(methodTextArea);

        // Add a key listener to replace tabs with 4 spaces.
        methodTextArea.textProperty().addListener((observable, oldValue, newValue) ->
        {
            methodTextArea.setText(methodTextArea.getText().replace("\t", "    "));
        });

        // Add a key listener to the method editing text area so the source code is updated
        // as soon as the text in the method editing area changes.
        if (method != null) {
            methodTextArea.textProperty().addListener((observable, oldValue, newValue) ->
            {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    Document document = editor.getDocument();

                    // Need to find the offset of the left bracket because the UI method text is just the body.
                    int leftBracketOffset = Utilities.findOffsetOfSubstring(document.getText(method.getTextRange()), "\\{");

                    // Replace the source text.
                    String sourceText = convertMethodAreaTextToSourceText(initialIndexAmount);
                    document.replaceString(method.getTextRange().getStartOffset() + leftBracketOffset + 1, method.getTextRange().getEndOffset() - 1, sourceText);
                });
            });
        }
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
                editMethodSource();
            }
        }
    }


    private void editMethodSource() {
        PsiClass currentClass = Utilities.getCurrentClass(ui.getProject());
        PsiMethod[] currentMethods = Utilities.getCurrentMethods(currentClass);
        PsiField[] currentVariables = Utilities.getCurrentVariables(currentClass);
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
            offsetToInsertNewMethod = currentClass.getLBrace().getTextOffset() + 1;
        }

        // Delete the current method and then insert the new method in its place.
        WriteCommandAction.writeCommandAction(ui.getProject()).run(() -> method.delete());
        Utilities.waitForNumberOfMethodsInClassToChange(originalNumberOfMethods, currentClass);
        AddMethodHandler.insertNewMethodText(this, offsetToInsertNewMethod);

        // Rebuild the method editing scene.
        PsiMethod[] newPsiMethods = ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>) currentClass::getMethods);
        for (PsiMethod newMethod : newPsiMethods) {
            String newMethodName = ApplicationManager.getApplication().runReadAction((Computable<String>) newMethod::getName);
            if (nameField.getText().equals(newMethodName)) {
                method = newMethod;
                break;
            }
        }

        // Find the index of the currently focused component.
        int focusedIndex = 0;
        for (int i = 0; i < methodRow.getChildren().size(); i++) {
            Node component = methodRow.getChildren().get(i);
            if (component.equals(getFocusOwner())) {
                focusedIndex = i;
                break;
            }
        }

        // Rebuild the UI components.
        VBox root = new VBox();
        setRoot(root);

        // Add the back button.
        root.getChildren().add(backButton);

        // Add the other components.
        MethodData newMethodData = new MethodData(method);
        buildSourcePartsOfMethodEditingScene(root, newMethodData, ui.getProject());

        // Set the focused component.
        if (focusedIndex < methodRow.getChildren().size()) {
            methodRow.getChildren().get(focusedIndex).requestFocus();
        }
        else {
            methodRow.getChildren().get(methodRow.getChildren().size() - 1).requestFocus();
        }
    }


    /**
     * Get the current text from the text area and modify it slightly so it looks correct in the source.
     * @param indentAmount The indent amount for the lines.
     * @return
     */
    public String convertMethodAreaTextToSourceText(int indentAmount) {
        String sourceText = "";
        for (String line : methodTextArea.getText().split("\n")) {
            sourceText += StringUtils.repeat(" ", indentAmount);
            sourceText += line + "\n";
        }
        sourceText = "\n" + sourceText + "    ";

        return sourceText;
    }
}
