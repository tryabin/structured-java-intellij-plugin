package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javafx.scene.input.KeyCode.*;


public class StructuredJavaToolWindowFactoryJavaFX implements ToolWindowFactory, EventHandler<KeyEvent> {

    public static final String DEFAULT_HIGHLIGHT_COLOR = "-fx-background-color: #308cfc;";

    private Project project;
    private KeyboardFocusInfo keyboardFocusInfo;
    private List<PsiField> variables = new ArrayList<>();
    private List<PsiMethod> methods = new ArrayList<>();
    private List<PsiClass> enums = new ArrayList<>();
    private List<PsiClass> innerClasses = new ArrayList<>();
    private List<List<PsiNamedElement>> dataAreas = new ArrayList<>();

    private VBox root = new VBox();
    private List<VBox> areas = new ArrayList<>();
    private Button addVariableButton;
    private TextField newVariableField;

    public Project getProject() {
        return project;
    }

    public TextField getNewVariableField() {
        return newVariableField;
    }

    public KeyboardFocusInfo getKeyboardFocusInfo () { return keyboardFocusInfo; };

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        final JFXPanel fxPanel = new JFXPanel();
        JComponent component = toolWindow.getComponent();

        // Start the Structured Java tool window UI.
        // ApplicationManager.getApplication().invokeLater(() -> {
        // DumbService.getInstance(project).runWhenSmart(() -> {
        DumbService.getInstance(project).smartInvokeLater(() -> Platform.runLater(() -> {
        // Platform.runLater(() -> {

            // Build the user interface.
            buildUserInterface();

            // Create the scene and add it to the panel.
            Scene scene = new Scene(root, 400, 250);
            scene.setOnKeyPressed(this);
            fxPanel.setScene(scene);

            // Don't focus on any particular component.
            root.requestFocus();

            // Setup keyboard focus.
            keyboardFocusInfo = new KeyboardFocusInfo();
            highlightFocusedComponent();
        // });
        }));

        component.getParent().add(fxPanel);
    }

    protected void buildUserInterface() {
        // Get all data in the currently opened class.
        PsiClass currentClass = Utilities.getCurrentClass(project);
        variables.clear();
        variables.addAll(Arrays.asList(currentClass.getFields()));
        methods.clear();
        methods.addAll(Arrays.asList(currentClass.getMethods()));
        enums.clear();
        innerClasses.clear();

        // Get any inner enum and regular classes.
        PsiClass[] allInnerClasses = ApplicationManager.getApplication().runReadAction((Computable<PsiClass[]>) currentClass::getAllInnerClasses);
        for (PsiClass innerClass : allInnerClasses) {
            if (innerClass.isEnum()) {
                enums.add(innerClass);
            } else {
                innerClasses.add(innerClass);
            }
        }

        // Add the variables and methods to the data areas list as PsiNamedElement objects.
        dataAreas.clear();
        dataAreas.add(new ArrayList<>(variables));
        dataAreas.add(new ArrayList<>(methods));
        dataAreas.add(new ArrayList<>(enums));
        dataAreas.add(new ArrayList<>(innerClasses));

        // Start building the user interface.
        root.getChildren().clear();
        root.setSpacing(20);
        root.setPadding(new Insets(0, 0, 0, 20));

        // The component holding the class info.
        VBox classBox = new VBox();
        classBox.getChildren().add(new Label(currentClass.getName()));
        root.getChildren().add(classBox);

        // Build the data areas.
        VBox variablesArea = buildVariablesArea();
        VBox methodsArea = buildMethodsArea();
        VBox enumsArea = buildEnumsArea();
        VBox innerClassesArea = buildInnerClassesArea();

        // Add the areas if they aren't empty.
        areas.clear();

        // The component holding the variables.
        if (!variables.isEmpty()) {
            root.getChildren().add(variablesArea);
            areas.add(variablesArea);
        }

        // The component holding the methods.
        if (!methods.isEmpty()) {
            root.getChildren().add(methodsArea);
            areas.add(methodsArea);
        }

        // The component holding inner classes that are enums.
        if (!enums.isEmpty()) {
            root.getChildren().add(enumsArea);
            areas.add(enumsArea);
        }

        // The component holding non-enum inner classes.
        if (!innerClasses.isEmpty()) {
            root.getChildren().add(innerClassesArea);
            areas.add(innerClassesArea);
        }
    }

    @Override
    public void handle(KeyEvent event) {
        VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        // Navigating areas or row with the up/down arrow keys.
        if (event.getCode() == UP) {
            moveFocusUpForAreaOrRow();

        }
        if (event.getCode() == DOWN) {
            moveFocusDownForAreaOrRow();
        }

        // If we are in row selection mode and the ENTER key is pressed, set the keyboard focus
        // to edit the first text field in the row. Otherwise switch to row selection mode.
        // If a button is focused, only activate the button.
        if (event.getCode() == ENTER) {
            if (addVariableButton.isFocused()) {
                addVariableButton.fire();
            }
            else {
                switch (keyboardFocusInfo.getFocusLevel()) {
                    case AREA: {
                        keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);
                        keyboardFocusInfo.setFocusedRow(0);
                        break;
                    }
                    case ROW: {
                        HBox row = (HBox) (focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow()));
                        row.getChildren().get(0).requestFocus();
                        keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.COLUMN);
                        keyboardFocusInfo.setFocusedColumn(0);
                        break;
                    }
                    case COLUMN: {
                        // Rename the variable if the focused column is the name.
                        HBox row = (HBox) (focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow()));
                        if (keyboardFocusInfo.getFocusedColumn() == row.getChildren().size() - 1) {
                            // Get the IntelliJ reference to the thing to rename.
                            List<PsiNamedElement> psiNamedElements = dataAreas.get(keyboardFocusInfo.getFocusedAreaIndex());
                            PsiNamedElement psiElement = psiNamedElements.get(keyboardFocusInfo.getFocusedRow());

                            // Run the rename refactor function.
                            String newName = getFocusedTextField().getText();
                            Runnable renameVariableAction = () ->
                            {
                                for (PsiReference reference : ReferencesSearch.search(psiElement)) {
                                    reference.handleElementRename(newName);
                                }
                                psiElement.setName(newName);
                            };
                            WriteCommandAction.runWriteCommandAction(project, renameVariableAction);
                        }

                        // Move the focus to row selection.
                        focusedRowArea.requestFocus();
                        keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);
                        break;
                    }
                }
            }
        }

        // Use TAB and alt-TAB to cycle through rows in row selection mode, and columns if not.
        if (event.getCode() == TAB) {
            if (!event.isShiftDown()) {
                if (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
                    moveFocusRightOneColumn();
                }
                else {
                    moveFocusDownForAreaOrRow();
                }
            }
            else {
                if (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
                    moveFocusLeftOneColumn();
                }
                else {
                    moveFocusUpForAreaOrRow();
                }
            }
        }

        // Use the LEFT key to exit editing a row without making any changes and to move up one focus level.
        if (event.getCode() == LEFT) {
            if (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.ROW) {
                keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.AREA);
            }
        }

        // Use the RIGHT key to enter an area or row.
        if (event.getCode() == RIGHT) {
            switch (keyboardFocusInfo.getFocusLevel()) {
                case AREA: {
                    keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);
                    keyboardFocusInfo.setFocusedRow(0);
                    break;
                }
                case ROW: {
                    HBox row = (HBox) (focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow()));
                    row.getChildren().get(0).requestFocus();
                    keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.COLUMN);
                    keyboardFocusInfo.setFocusedColumn(0);
                    break;
                }
            }
        }

        // If a variable row is highlighted and the Delete key is pressed then delete the variable.
        if (event.getCode() == DELETE) {
            switch (keyboardFocusInfo.getFocusLevel()) {
                case ROW: {
                    // If the focus is not on the Add Variable row then delete the focused variable.
                    if (keyboardFocusInfo.getFocusedRow() != focusedRowArea.getChildren().size() - 1) {
                        // Delete the variable from the source code.
                        PsiField variableToDelete = variables.get(keyboardFocusInfo.getFocusedRow());
                        WriteCommandAction.writeCommandAction(project).run(variableToDelete::delete);

                        // Wait until the number of variables in the class changes.
                        Utilities.waitForNumberOfVariablesInClassToChange(variables.size(), this);

                        // Rebuild the UI.
                        buildUserInterface();
                    }
                }
            }
        }
        // Highlight the currently focused row.
        highlightFocusedComponent();

        // Set the keyboard focus to the correct element.
        setKeyboardFocus();
    }

    private void moveFocusDownForAreaOrRow() {
        VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        switch (keyboardFocusInfo.getFocusLevel()) {
            case AREA:
                if (keyboardFocusInfo.getFocusedAreaIndex() < areas.size() - 1) {
                    keyboardFocusInfo.incrementFocusedAreaIndex();
                }
                break;
            case ROW:
                if (keyboardFocusInfo.getFocusedRow() < focusedRowArea.getChildren().size() - 1) {
                    keyboardFocusInfo.incrementRow();
                }
                break;
        }
    }

    private void moveFocusUpForAreaOrRow() {
        switch (keyboardFocusInfo.getFocusLevel()) {
            case AREA:
                if (keyboardFocusInfo.getFocusedAreaIndex() > 0) {
                    keyboardFocusInfo.decrementFocusedAreaIndex();
                }
                break;
            case ROW:
                if (keyboardFocusInfo.getFocusedRow() > 0) {
                    keyboardFocusInfo.decrementRow();
                }
                break;
        }
    }

    private TextField getFocusedTextField() {
        VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);
        HBox row = (HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow());
        return (TextField) row.getChildren().get(keyboardFocusInfo.getFocusedColumn());
    }

    private void setNullStyleRecursively(VBox node) {
        for (Node child : node.getChildren()) {
            child.setStyle(null);
            if (child instanceof VBox) {
                setNullStyleRecursively((VBox) child);
            }
        }
    }

    protected void highlightFocusedComponent() {
        // Reset the style of every component that can be highlighted.
        setNullStyleRecursively(root);

        // Get the area of focus.
        VBox areaOfFocus = areas.get(keyboardFocusInfo.getFocusedAreaIndex());

        // Highlight the correct component.
        switch (keyboardFocusInfo.getFocusLevel()) {
            case AREA:
                areaOfFocus.setStyle(DEFAULT_HIGHLIGHT_COLOR);
                break;
            case ROW:
            case COLUMN:
                VBox rowArea = (VBox) areaOfFocus.getChildren().get(1);
                rowArea.getChildren().get(keyboardFocusInfo.getFocusedRow()).setStyle(DEFAULT_HIGHLIGHT_COLOR);
                break;
        }
    }

    protected void setKeyboardFocus() {
        VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        switch(keyboardFocusInfo.getFocusLevel()) {
            case AREA:
                focusedArea.requestFocus();
                break;
            case ROW:
                focusedRowArea.requestFocus();
                break;
            case COLUMN:
                HBox row = (HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow());
                row.getChildren().get(keyboardFocusInfo.getFocusedColumn()).requestFocus();
                break;
        }
    }

    private void moveFocusRightOneColumn() {
        VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        if (keyboardFocusInfo.getFocusedColumn() < ((HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow())).getChildren().size() - 1 &&
            keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
            keyboardFocusInfo.incrementColumn();
        }
    }

    private void moveFocusLeftOneColumn() {
        if (keyboardFocusInfo.getFocusedColumn() > 0 &&
            keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
            keyboardFocusInfo.decrementColumn();
        }
    }

    private VBox buildVariablesArea() {
        // Build the component holding the rows.
        VBox areaRowBox = new VBox();
        for (PsiField variable : variables) {
            HBox rowBox = new HBox();
            rowBox.setSpacing(5);

            // Get a list of strings for all of the variable's fields.
            List<String> currentFields = new ArrayList<>();

            // Modifiers
            PsiElement[] modifiers = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> variable.getModifierList().getChildren());
            for (PsiElement modifier : modifiers) {
                currentFields.add(modifier.getText());
            }

            // Type
            String variableType = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> variable.getType().getPresentableText());
            currentFields.add(variableType);

            // Name
            String variableName = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> variable.getName());
            currentFields.add(variableName);

            // Add all of the fields as text fields to the current row.
            for (String field : currentFields) {
                TextField textField = getVariableField(field);
                rowBox.getChildren().add(textField);
            }

            areaRowBox.getChildren().add(rowBox);
        }

        // The row for adding a new variable.
        HBox newVariableRow = new HBox();

        // The textfield for the new variable.
        newVariableField = new TextField();

         // Exit adding a variable with CTRL-X.
        // newVariableField.setOnKeyPressed(getExitTextFieldHandler());
        newVariableRow.getChildren().add(newVariableField);

        // The button to add a variable.
        addVariableButton = new Button("Add Variable");
        addVariableButton.setOnAction(new AddVariableHandler(this));
        newVariableRow.getChildren().add(addVariableButton);
        areaRowBox.getChildren().add(newVariableRow);

        // Build the root component of the area.
        VBox area = new VBox();
        Label label = new Label("Variables");
        area.getChildren().add(label);
        area.getChildren().add(areaRowBox);

        return area;
    }

    /**
     * This handler is attached to textfields so they can be exited with some key combination.
     * @return
     */
    private EventHandler<KeyEvent> getExitTextFieldHandler() {
        return event -> {
            VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
            VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);
            if (event.getCode() == X && event.isControlDown()) {
                switch (keyboardFocusInfo.getFocusLevel()) {
                    case COLUMN: {
                        if (keyboardFocusInfo.getFocusedRow() == focusedRowArea.getChildren().size() - 1 &&
                            keyboardFocusInfo.getFocusedColumn() == 0) {
                            keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);

                            // Highlight the currently focused row.
                            highlightFocusedComponent();

                            // Set the keyboard focus to the correct element.
                            setKeyboardFocus();
                        }
                    }
                }
            }
        };
    }

    private VBox buildMethodsArea() {
        // Build the component holding the rows.
        VBox areaRowBox = new VBox();
        for (PsiMethod method : methods) {
            HBox rowBox = new HBox();
            rowBox.setSpacing(5);

            // Get a list of strings for all of the method's fields.
            List<String> currentFields = new ArrayList<>();

            // Modifiers
            PsiElement[] modifiers = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> method.getModifierList().getChildren());
            for (PsiElement modifier : modifiers) {
                currentFields.add(modifier.getText());
            }

            // Return Type
            boolean methodIsConstructor = ApplicationManager.getApplication().runReadAction((Computable<Boolean>) method::isConstructor);
            if (!methodIsConstructor) {
                String methodReturnType = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> method.getReturnType().getPresentableText());
                currentFields.add(methodReturnType);
            }

            // Name
            String methodName = ApplicationManager.getApplication().runReadAction((Computable<String>) method::getName);
            currentFields.add(methodName);

            // Add all of the fields as text fields to the current row.
            for (String field : currentFields) {
                TextField textField = getVariableField(field);
                rowBox.getChildren().add(textField);
            }

            areaRowBox.getChildren().add(rowBox);
        }

        // Build the root component of the area.
        VBox area = new VBox();
        Label label = new Label("Methods");
        area.getChildren().add(label);
        area.getChildren().add(areaRowBox);

        return area;
    }

    private VBox buildEnumsArea() {
        // Build the component holding the rows.
        VBox areaRowBox = new VBox();
        for (PsiClass enumClass : enums) {
            HBox rowBox = new HBox();
            rowBox.setSpacing(5);

            // Get a list of strings for all of the enum's fields.
            List<String> currentFields = new ArrayList<>();

            // Modifiers
            PsiElement[] modifiers = enumClass.getModifierList().getChildren();
            for (PsiElement modifier : modifiers) {
                currentFields.add(modifier.getText());
            }

            // Name
            currentFields.add(enumClass.getName());

            // Add all of the fields as text fields to the current row.
            for (String field : currentFields) {
                TextField textField = getVariableField(field);
                rowBox.getChildren().add(textField);
            }

            areaRowBox.getChildren().add(rowBox);
        }

        // Build the root component of the area.
        VBox area = new VBox();
        Label label = new Label("Enums");
        area.getChildren().add(label);
        area.getChildren().add(areaRowBox);

        return area;
    }

    private VBox buildInnerClassesArea() {
        // Build the component holding the rows.
        VBox areaRowBox = new VBox();
        for (PsiClass innerClass : innerClasses) {
            HBox rowBox = new HBox();
            rowBox.setSpacing(5);

            // Get a list of strings for all of the class's fields.
            List<String> currentFields = new ArrayList<>();

            // Modifiers
            PsiElement[] modifiers = innerClass.getModifierList().getChildren();
            for (PsiElement modifier : modifiers) {
                currentFields.add(modifier.getText());
            }

            // Name
            currentFields.add(innerClass.getName());

            // Add all of the fields as text fields to the current row.
            for (String field : currentFields) {
                TextField textField = getVariableField(field);
                rowBox.getChildren().add(textField);
            }

            areaRowBox.getChildren().add(rowBox);
        }

        // Build the root component of the area.
        VBox area = new VBox();
        Label label = new Label("Inner Classes");
        area.getChildren().add(label);
        area.getChildren().add(areaRowBox);

        return area;
    }

    private TextField getVariableField(String field) {
        TextField textField = new TextField();

        // Add a listener to dynamically change the width of the text field so it matches
        // the contents.
        textField.textProperty().addListener((ob, o, n) -> {
            textField.setPrefWidth(TextUtils.computeTextWidth(textField.getFont(), textField.getText(), 0.0D) + 15);
        });
        textField.setText(field);

        // Set custom TAB behavior when editing a text field so only the text fields
        // in that row are navigable.
        textField.setFocusTraversable(false);
        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == TAB) {
                if (!event.isShiftDown()) {
                    moveFocusRightOneColumn();
                } else {
                    moveFocusLeftOneColumn();
                }

                setKeyboardFocus();
                event.consume();
            }
        });
        return textField;
    }
}