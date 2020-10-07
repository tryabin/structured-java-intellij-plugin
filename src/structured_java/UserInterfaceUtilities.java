package structured_java;

import javafx.scene.control.TextField;

public class UserInterfaceUtilities {


    public static TextField getField(String field) {
        TextField textField = getField();
        textField.setText(field);

        return textField;
    }

    public static TextField getField() {
        TextField textField = new TextField();

        // Add a listener to dynamically change the width of the text field so it matches the contents.
        textField.textProperty().addListener((ob, o, n) -> {
            textField.setPrefWidth(TextUtils.computeTextWidth(textField.getFont(), textField.getText(), 0.0D) + 15);
        });

        return textField;
    }
}
