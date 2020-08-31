package structured_java;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class MethodEditingScene extends Scene {

    private Button backButton;
    private TextArea methodTextArea;

    public MethodEditingScene(Parent root) {
        super(root);
    }

    public Button getBackButton() {
        return backButton;
    }

    public void setBackButton(Button backButton) {
        this.backButton = backButton;
    }

    public TextArea getMethodTextArea() {
        return methodTextArea;
    }

    public void setMethodTextArea(TextArea methodTextArea) {
        this.methodTextArea = methodTextArea;
    }
}
