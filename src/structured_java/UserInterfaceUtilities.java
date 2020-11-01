package structured_java;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;

public class UserInterfaceUtilities {

    public static TextField getField(String field, String fontName, int fontSize) {
        TextField textField = new TextField();
        textField.setStyle(getStyleString(fontName, fontSize));
        textField.setAlignment(Pos.CENTER);
        textField.setText(field);

        // Dynamically resize the width of the text field so it matches the text length.
        textField.prefColumnCountProperty().bind(textField.textProperty().length().add(1)); // Add 1 for the cursor.

        return textField;
    }


    public static TextField getField(String field) {
        TextField textField = new TextField();
        textField.setText(field);
        textField.prefColumnCountProperty().bind(textField.textProperty().length().add(1)); // Add 1 for the cursor.

        return textField;
    }


    public static String getStyleString(String fontName, int fontSize) {
        return "-fx-font: " + fontSize + "px \"" + fontName + "\";";
    }
}
