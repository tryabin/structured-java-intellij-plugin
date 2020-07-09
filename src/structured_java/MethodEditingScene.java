package structured_java;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;

public class MethodEditingScene extends Scene {

    private Button backButton;

    public MethodEditingScene(Parent root) {
        super(root);
    }

    public Button getBackButton() {
        return backButton;
    }

    public void setBackButton(Button backButton) {
        this.backButton = backButton;
    }
}
