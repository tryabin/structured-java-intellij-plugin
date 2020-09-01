package structured_java;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ClassOutlineScene extends Scene {

    private Button addVariableButton;
    private Button addMethodButton;
    private TextField newVariableTextField;
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

    public TextField getNewVariableTextField() {
        return newVariableTextField;
    }

    public void setNewVariableTextField(TextField newVariableTextField) {
        this.newVariableTextField = newVariableTextField;
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
}
