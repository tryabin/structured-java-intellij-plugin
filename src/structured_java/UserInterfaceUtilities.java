package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class UserInterfaceUtilities {

    public static HBox buildMethodRow(PsiMethod method) {
        HBox rowBox = new HBox();
        rowBox.setSpacing(5);

        // Modifiers
        PsiElement[] modifiers = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> method.getModifierList().getChildren());
        for (PsiElement modifier : modifiers) {
            rowBox.getChildren().add(getField(modifier.getText()));
        }

        // Return Type
        boolean methodIsConstructor = ApplicationManager.getApplication().runReadAction((Computable<Boolean>) method::isConstructor);
        if (!methodIsConstructor) {
            String methodReturnType = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> method.getReturnType().getPresentableText());
            rowBox.getChildren().add(getField(methodReturnType));
        }

        // Build the parameters component.
        VBox parametersListComponent = getMethodFullParametersComponent(method);
        rowBox.getChildren().add(parametersListComponent);

        // Name
        String methodName = ApplicationManager.getApplication().runReadAction((Computable<String>) method::getName);
        rowBox.getChildren().add(getField(methodName));

        return rowBox;
    }


    private static VBox getMethodFullParametersComponent(PsiMethod method) {
        VBox parametersComponent = new VBox();

        PsiParameter[] parameters = ApplicationManager.getApplication().runReadAction((Computable<PsiParameter[]>) () -> method.getParameterList().getParameters());
        for (PsiParameter parameter : parameters) {
            String type = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> parameter.getType().getPresentableText());
            String name = ApplicationManager.getApplication().runReadAction((Computable<String>) parameter::getName);
            String parameterString = type + " " + name;
            parametersComponent.getChildren().add(getField(parameterString));
        }

        return parametersComponent;
    }


    public static TextField getField(String field) {
        TextField textField = new TextField();

        // Add a listener to dynamically change the width of the text field so it matches
        // the contents.
        textField.textProperty().addListener((ob, o, n) -> {
            textField.setPrefWidth(TextUtils.computeTextWidth(textField.getFont(), textField.getText(), 0.0D) + 15);
        });

        textField.setText(field);
        return textField;
    }
}
