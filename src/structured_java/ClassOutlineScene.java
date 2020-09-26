package structured_java;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassOutlineScene extends Scene {

    private Button addVariableButton;
    private Button addMethodButton;
    private ComboBox<String> newVariableAccessModifierBox;
    private ComboBox<String> newVariableStaticModifierBox;
    private TextField newVariableTypeField;
    private TextField newVariableNameField;
    private TextField newVariableInitialValueField;
    private Set<TextField> variableNameTextFields = new HashSet<>();
    private Set<TextField> variableInitialValueTextFields = new HashSet<>();

    public ClassOutlineScene(Parent root) {
        super(root);
    }

    public Button getAddVariableButton() {
        return addVariableButton;
    }

    public void setAddVariableButton(Button addVariableButton) {
        this.addVariableButton = addVariableButton;
    }

    public Button getAddMethodButton() {
        return addMethodButton;
    }

    public void setAddMethodButton(Button addMethodButton) {
        this.addMethodButton = addMethodButton;
    }

    public ComboBox<String> getNewVariableAccessModifierBox() {
        return newVariableAccessModifierBox;
    }

    public void setNewVariableAccessModifierBox(ComboBox<String> newVariableAccessModifierBox) {
        this.newVariableAccessModifierBox = newVariableAccessModifierBox;
    }

    public ComboBox<String> getNewVariableStaticModifierBox() {
        return newVariableStaticModifierBox;
    }

    public void setNewVariableStaticModifierBox(ComboBox<String> newVariableStaticModifierBox) {
        this.newVariableStaticModifierBox = newVariableStaticModifierBox;
    }

    public TextField getNewVariableTypeField() {
        return newVariableTypeField;
    }

    public void setNewVariableTypeField(TextField newVariableTypeField) {
        this.newVariableTypeField = newVariableTypeField;
    }

    public TextField getNewVariableNameField() {
        return newVariableNameField;
    }

    public void setNewVariableNameField(TextField newVariableNameField) {
        this.newVariableNameField = newVariableNameField;
    }

    public TextField getNewVariableInitialValueField() {
        return newVariableInitialValueField;
    }

    public void setNewVariableInitialValueField(TextField newVariableInitialValueField) {
        this.newVariableInitialValueField = newVariableInitialValueField;
    }

    public Set<TextField> getVariableInitialValueTextFields() {
        return variableInitialValueTextFields;
    }

    public void setVariableInitialValueTextFields(Set<TextField> variableInitialValueTextFields) {
        this.variableInitialValueTextFields = variableInitialValueTextFields;
    }

    public Set<TextField> getVariableNameTextFields() {
        return variableNameTextFields;
    }

    public void setVariableNameTextFields(Set<TextField> variableNameTextFields) {
        this.variableNameTextFields = variableNameTextFields;
    }

    public List<VBox> getAreas() {
        List<VBox> areas = new ArrayList<>();
        List<Node> rootChildren = ((VBox)getRoot()).getChildren();
        for (int i = 1; i < rootChildren.size(); i++) {
            areas.add((VBox) rootChildren.get(i));
        }
        return areas;
    }

    public String getNewVariableSourceText() {
        String sourceText = "";

        // Access Modifier
        if (!newVariableAccessModifierBox.getValue().equals("None")) {
            sourceText += newVariableAccessModifierBox.getValue() + " ";
        }

        // Static / Non-Static Modifier
        if (newVariableStaticModifierBox.getValue().equals("static")) {
            sourceText += newVariableStaticModifierBox.getValue() + " ";
        }

        // Type
        sourceText += newVariableTypeField.getText() + " ";

        // Name
        sourceText += newVariableNameField.getText();

        // Add the initial value if it is non-empty.
        if (newVariableInitialValueField.getText().trim().length() > 0) {
            sourceText += " = " + newVariableInitialValueField.getText();
        }

        sourceText += ";";

        return sourceText;
    }
}
