package structured_java;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ClassOutlineScene extends Scene {

    private Button addVariableButton;
    private Button addMethodButton;
    private ComboBox<String> newVariableAccessModifierBox;
    private ComboBox<String> newVariableStaticModifierBox;
    private TextField newVariableTypeField;
    private TextField newVariableNameField;
    private TextField newMethodTextField;

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

    public TextField getNewMethodTextField() {
        return newMethodTextField;
    }

    public void setNewMethodTextField(TextField newMethodTextField) {
        this.newMethodTextField = newMethodTextField;
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
        sourceText += newVariableAccessModifierBox.getValue() + " ";
        if (newVariableStaticModifierBox.getValue().equals("static")) {
            sourceText += newVariableStaticModifierBox.getValue() + " ";
        }
        sourceText += newVariableTypeField.getText() + " ";
        sourceText += newVariableNameField.getText() + ";";

        return sourceText;
    }
}
