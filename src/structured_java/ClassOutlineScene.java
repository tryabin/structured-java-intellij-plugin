package structured_java;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ClassOutlineScene {

    private Scene scene;
    private Button addVariableButton;
    private TextField newVariableTextField;

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public Button getAddVariableButton() {
        return addVariableButton;
    }

    public void setAddVariableButton(Button addVariableButton) {
        this.addVariableButton = addVariableButton;
    }

    public TextField getNewVariableTextField() {
        return newVariableTextField;
    }

    public void setNewVariableTextField(TextField newVariableTextField) {
        this.newVariableTextField = newVariableTextField;
    }

    public VBox getRoot() {
        return (VBox) scene.getRoot();
    }

    public List<VBox> getAreas() {
        List<VBox> areas = new ArrayList<>();
        getRoot().getChildren();
        for (int i = 1; i < getRoot().getChildren().size(); i++) {
            areas.add((VBox) getRoot().getChildren().get(i));
        }
        return areas;
    }
}
