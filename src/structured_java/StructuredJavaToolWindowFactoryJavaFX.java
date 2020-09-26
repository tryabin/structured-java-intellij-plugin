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
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import static structured_java.UserInterfaceUtilities.buildMethodRow;
import static structured_java.UserInterfaceUtilities.getField;


public class StructuredJavaToolWindowFactoryJavaFX implements ToolWindowFactory, EventHandler<KeyEvent> {

    private static final String DEFAULT_HIGHLIGHT_COLOR = "-fx-background-color: #308cfc;";
    private static final List<Area> areaOrdering = Arrays.asList(Area.VARIABLE, Area.METHOD, Area.ENUM, Area.INNER_CLASS);

    private Project project;
    private PsiClass currentClass;
    private KeyboardFocusInfo keyboardFocusInfo;
    private List<PsiField> variables = new ArrayList<>();
    private List<PsiMethod> methods = new ArrayList<>();

    private List<PsiClass> enums = new ArrayList<>();
    private List<PsiClass> innerClasses = new ArrayList<>();
    private List<List<PsiNamedElement>> dataAreas = new ArrayList<>();

    private ClassOutlineScene classOutlineScene;
    private MethodEditingScene methodEditingScene;
    private JFXPanel fxPanel;

    public Project getProject() {
        return project;
    }

    public KeyboardFocusInfo getKeyboardFocusInfo () { return keyboardFocusInfo; }

    public ClassOutlineScene getClassOutlineScene() {
        return classOutlineScene;
    }


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        fxPanel = new JFXPanel();
        JComponent component = toolWindow.getComponent();

        // Start the Structured Java tool window UI.
        DumbService.getInstance(project).smartInvokeLater(() -> Platform.runLater(() -> {
            // Initialize the object holding the focus info.
            keyboardFocusInfo = new KeyboardFocusInfo();

            // Build the scene.
            buildEmptyMethodEditingScene();
            rebuildClassOutlineScene();
        }));

        component.getParent().add(fxPanel);
    }

    protected void buildClassOutlineScene() {
        // Get all data in the currently opened class.
        currentClass = Utilities.getCurrentClass(project);
        variables.clear();
        variables.addAll(Arrays.asList(ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>)currentClass::getFields)));
        methods.clear();
        methods.addAll(Arrays.asList(ApplicationManager.getApplication().runReadAction((Computable<PsiMethod[]>)currentClass::getMethods)));
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
        VBox root = new VBox();
        classOutlineScene = new ClassOutlineScene(root);
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

        // The component holding the variables.
        root.getChildren().add(variablesArea);

        // The component holding the methods.
        root.getChildren().add(methodsArea);

        // The component holding inner classes that are enums.
        if (!enums.isEmpty()) {
            root.getChildren().add(enumsArea);
        }

        // The component holding non-enum inner classes.
        if (!innerClasses.isEmpty()) {
            root.getChildren().add(innerClassesArea);
        }

        // Add a key handler to the scene.
        classOutlineScene.addEventHandler(KeyEvent.KEY_PRESSED, this);
    }

    @Override
    public void handle(KeyEvent event) {
        if (fxPanel.getScene() instanceof  ClassOutlineScene) {
            handleClassOutLineScene(event);
        }
    }

    private void handleClassOutLineScene(KeyEvent event) {
        // Get the focused area.
        Area currentArea = areaOrdering.get(keyboardFocusInfo.getFocusedAreaIndex());

        // Consume the event in certain situations to prevent undesirable effects.
        boolean enterPressed = event.getCode() == ENTER;
        boolean tabPressed = event.getCode() == TAB;
        boolean leftOrRightPressed = event.getCode() == LEFT || event.getCode() == RIGHT;
        boolean textFieldFocused = classOutlineScene.focusOwnerProperty().get() instanceof TextField;
        boolean comboBoxFocused = classOutlineScene.focusOwnerProperty().get() instanceof ComboBox;
        if (!textFieldFocused && !comboBoxFocused) {
            event.consume();
        }
        if (textFieldFocused && (tabPressed || enterPressed)) {
            event.consume();
        }
        if (comboBoxFocused && (leftOrRightPressed || tabPressed || enterPressed)) {
            event.consume();
        }

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
            if(classOutlineScene.focusOwnerProperty().get() instanceof Button) {
                ((Button)classOutlineScene.focusOwnerProperty().get()).fire();
            }
            else {
                switch (keyboardFocusInfo.getFocusLevel()) {
                    case AREA: {
                        keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);
                        keyboardFocusInfo.setFocusedRow(0);
                        break;
                    }
                    case ROW: {
                        switch (currentArea) {
                            case METHOD:
                                PsiMethod selectedMethod = methods.get(keyboardFocusInfo.getFocusedRow());
                                buildMethodEditingScene(selectedMethod);
                                setSceneToMethodEditing();
                                break;
                            default:
                                keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.COLUMN);
                                keyboardFocusInfo.setFocusedColumn(0);
                        }
                        break;
                    }
                    case COLUMN: {
                        // Do a rename operation if applicable.
                        handleRename();

                        // Change the initial value of a variable if applicable.
                        handleVariableInitialValueChange();

                        // Move the focus to row selection.
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

                    // Need to set the focus here so we can skip focusing on labels.
                    setKeyboardFocus();
                    if (classOutlineScene.focusOwnerProperty().get() instanceof Label) {
                        moveFocusRightOneColumn();
                    }

                    // If the focused component is a text field then highlight the text.
                    selectAllTextIfNavigatedToTextField();
                }
                else {
                    moveFocusDownForAreaOrRow();
                }
            }
            else {
                if (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
                    moveFocusLeftOneColumn();

                    // Need to set the focus here so we can skip focusing on labels.
                    setKeyboardFocus();
                    if (classOutlineScene.focusOwnerProperty().get() instanceof Label) {
                        moveFocusLeftOneColumn();
                    }

                    // If the focused component is a text field then highlight the text.
                    selectAllTextIfNavigatedToTextField();
                }
                else {
                    moveFocusUpForAreaOrRow();
                }
            }
        }

        // Use the LEFT key to exit editing a row without making any changes and to move up one focus level.
        if (event.getCode() == LEFT) {
            switch (keyboardFocusInfo.getFocusLevel()) {
                case ROW: {
                    keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.AREA);
                    break;
                }
                case COLUMN: {
                    return;
                }
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
                    keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.COLUMN);
                    keyboardFocusInfo.setFocusedColumn(0);
                    break;
                }
                case COLUMN: {
                    return;
                }
            }
        }

        // If a variable row is highlighted and the Delete key is pressed then delete the variable.
        if (event.getCode() == DELETE) {
            if (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.ROW) {

                // If the focus is not on the Add Variable row then delete the focused variable.
                switch (currentArea) {
                    case VARIABLE: {
                        PsiField variableToDelete = variables.get(keyboardFocusInfo.getFocusedRow());
                        WriteCommandAction.writeCommandAction(project).run(variableToDelete::delete);

                        // Wait until the number of variables in the class changes.
                        Utilities.waitForNumberOfVariablesInClassToChange(variables.size(), this);

                        // Rebuild the UI.
                        rebuildClassOutlineScene();
                        break;
                    }
                    case METHOD: {
                        PsiMethod methodToDelete = methods.get(keyboardFocusInfo.getFocusedRow());
                        WriteCommandAction.writeCommandAction(project).run(methodToDelete::delete);

                        // Wait until the number of methods in the class changes.
                        Utilities.waitForNumberOfMethodsInClassToChange(methods.size(), project);

                        // Rebuild the UI.
                        rebuildClassOutlineScene();
                        break;
                    }
                }
            }
        }

        // Highlight the currently focused row.
        highlightFocusedComponent();

        // Set the keyboard focus to the correct element.
        setKeyboardFocus();
    }


    private void handleRename() {

        // Get the focused area and row area.
        VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);
        HBox row = (HBox) (focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow()));

        // Rename the variable if the focused column is the variable or method name.
        if (classOutlineScene.getVariableNameTextFields().contains(classOutlineScene.focusOwnerProperty().get()) ||
               (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN &&
                areaOrdering.get(keyboardFocusInfo.getFocusedAreaIndex()) == Area.METHOD &&
                keyboardFocusInfo.getFocusedColumn() == row.getChildren().size() - 1)) {

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
    }


    private void handleVariableInitialValueChange() {

        // Change the initial value of the variable if the focused column is an initial value text field.
        if (classOutlineScene.getVariableInitialValueTextFields().contains(classOutlineScene.focusOwnerProperty().get())) {
            Runnable setInitialValueAction = () ->
            {
                PsiField currentVariable = variables.get(keyboardFocusInfo.getFocusedRow());
                String newInitialValueText = getFocusedTextField().getText();
                PsiExpression newInitialValue = null;

                // Create a new PsiExpression from the new initial value if it is non-empty.
                if (!newInitialValueText.trim().isEmpty()) {
                    newInitialValue = PsiElementFactory.getInstance(project).createExpressionFromText(newInitialValueText, currentVariable.getInitializer());
                }
                currentVariable.setInitializer(newInitialValue);
            };
            WriteCommandAction.runWriteCommandAction(project, setInitialValueAction);

            // Rebuild the UI.
            keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.ROW);
            rebuildClassOutlineScene();
        }
    }


    protected void rebuildClassOutlineScene() {
        buildClassOutlineScene();
        setSceneToClassOutline();
    }





    private void moveFocusDownForAreaOrRow() {
        moveFocusForAreaOrRow(1);
    }


    private void moveFocusUpForAreaOrRow() {
        moveFocusForAreaOrRow(-1);
    }


    private void moveFocusForAreaOrRow(int indexIncrement) {
        List<VBox> areas = classOutlineScene.getAreas();
        VBox focusedArea = areas.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        switch (keyboardFocusInfo.getFocusLevel()) {
            case AREA: {
                int newIndex = keyboardFocusInfo.getFocusedAreaIndex() + indexIncrement;
                if (newIndex >= 0 && newIndex < areas.size()) {
                    keyboardFocusInfo.setFocusedAreaIndex(newIndex);
                }
                break;
            }
            case ROW: {
                int newIndex = keyboardFocusInfo.getFocusedRow() + indexIncrement;
                if (newIndex >= 0 && newIndex < focusedRowArea.getChildren().size()) {
                    keyboardFocusInfo.setFocusedRow(newIndex);
                }
                break;
            }
        }
    }


    private TextField getFocusedTextField() {
        List<VBox> areas = classOutlineScene.getAreas();
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
        setNullStyleRecursively((VBox)classOutlineScene.getRoot());

        // Get the area of focus.
        VBox areaOfFocus = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());

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


    private void selectAllTextIfNavigatedToTextField() {
        if (classOutlineScene.focusOwnerProperty().get() instanceof TextField) {
            TextField focusedTextField = (TextField) classOutlineScene.focusOwnerProperty().get();
            focusedTextField.selectAll();
        }
    }


    protected void setKeyboardFocus() {
        VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        switch(keyboardFocusInfo.getFocusLevel()) {
            case AREA:
                focusedArea.requestFocus();
                break;
            case ROW:
                focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow()).requestFocus();
                break;
            case COLUMN:
                HBox row = (HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow());
                row.getChildren().get(keyboardFocusInfo.getFocusedColumn()).requestFocus();
                break;
        }
    }


    private void moveFocusRightOneColumn() {
        VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);

        // Move the focus one column to the right if we aren't already on the last column.
        if (keyboardFocusInfo.getFocusedColumn() < ((HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow())).getChildren().size() - 1 &&
            keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
            keyboardFocusInfo.incrementColumn();
        }
    }


    private void moveFocusLeftOneColumn() {

        // Move the focus one column to the left if we aren't already on the first column.
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

            // Modifiers
            PsiElement[] modifiers = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> variable.getModifierList().getChildren());
            for (PsiElement modifier : modifiers) {
                if (modifier.getText().trim().isEmpty()) {
                    continue;
                }
                TextField textField = getField(modifier.getText());
                rowBox.getChildren().add(textField);
            }

            // Type
            String variableType = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> variable.getType().getPresentableText());
            TextField variableTypeField = getField(variableType);
            rowBox.getChildren().add(variableTypeField);

            // Name
            String variableName = ApplicationManager.getApplication().runReadAction((Computable<String>) variable::getName);
            TextField nameField = getField(variableName);
            classOutlineScene.getVariableNameTextFields().add(nameField);
            rowBox.getChildren().add(nameField);

            // If the variable has an initial value, add an equals sign label and
            // a text field for the initial value.
            boolean variableHasInitializer =  ApplicationManager.getApplication().runReadAction((Computable<Boolean>) variable::hasInitializer);
            if (variableHasInitializer) {
                String variableInitialValue = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> variable.getInitializer().getText());

                // '=' label.
                Label equalsSign = new Label(" = ");
                rowBox.getChildren().add(equalsSign);

                // Initial value field.
                TextField initialValueField = getField(variableInitialValue);
                classOutlineScene.getVariableInitialValueTextFields().add(initialValueField);
                rowBox.getChildren().add(initialValueField);
            }

            // Otherwise add a button to add an initial value.
            else {
                Button addInitialValueButton = new Button("Set Initial Value");
                rowBox.getChildren().add(addInitialValueButton);

                // When the button is pressed it is replaced with a text field.
                addInitialValueButton.setOnAction(e -> {
                    TextField newInitialValueTextField = getField("<Initial Value>");
                    newInitialValueTextField.selectAll();
                    rowBox.getChildren().set(rowBox.getChildren().size() - 1, newInitialValueTextField);
                    classOutlineScene.getVariableInitialValueTextFields().add(newInitialValueTextField);
                });

            }

            areaRowBox.getChildren().add(rowBox);
        }

        // The row for adding a new variable.
        HBox newVariableRow = new HBox();
        newVariableRow.setSpacing(5);

        // The access modifier dropdown.
        ComboBox<String> accessModifierBox = new ComboBox<>(FXCollections.observableArrayList("private", "protected", "public", "None"));
        accessModifierBox.getSelectionModel().selectFirst();
        newVariableRow.getChildren().add(accessModifierBox);
        classOutlineScene.setNewVariableAccessModifierBox(accessModifierBox);

        // Static or non-stick dropdown.
        ComboBox<String> staticModifierBox = new ComboBox<>(FXCollections.observableArrayList("non-static", "static"));
        staticModifierBox.getSelectionModel().selectFirst();
        newVariableRow.getChildren().add(staticModifierBox);
        classOutlineScene.setNewVariableStaticModifierBox(staticModifierBox);

        // Type text field.
        TextField typeField = getField("<Type>");
        newVariableRow.getChildren().add(typeField);
        classOutlineScene.setNewVariableTypeField(typeField);

        // Name field.
        TextField nameField = getField("<Name>");
        newVariableRow.getChildren().add(nameField);
        classOutlineScene.setNewVariableNameField(nameField);

        // '=' label.
        Label equalsSign = new Label(" = ");
        newVariableRow.getChildren().add(equalsSign);

        // Initial value field.
        TextField initialValueField = getField("<Initial Value>");
        newVariableRow.getChildren().add(initialValueField);
        classOutlineScene.setNewVariableInitialValueField(initialValueField);

        // The button to add a variable.
        Button addVariableButton = new Button("Add Variable");
        addVariableButton.setOnAction(new AddVariableHandler(this));
        newVariableRow.getChildren().add(addVariableButton);
        areaRowBox.getChildren().add(newVariableRow);
        classOutlineScene.setAddVariableButton(addVariableButton);

        // Build the root component of the area.
        VBox area = new VBox();
        Label label = new Label("Variables");
        area.getChildren().add(label);
        area.getChildren().add(areaRowBox);

        return area;
    }


    /**
     * This handler is attached to text fields so they can be exited with some key combination.
     * @return
     */
    private EventHandler<KeyEvent> getExitTextFieldHandler() {
        return event -> {
            VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
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

        // Create a row for each method.
        for (PsiMethod method : methods) {
            HBox rowBox = buildMethodRow(method);

            areaRowBox.getChildren().add(rowBox);
        }

        // Create a row for the button to add a new method.
        Button addMethodButton = new Button("Add Method");
        addMethodButton.setOnAction(event -> {
            buildEmptyMethodEditingScene();
            setSceneToMethodEditing();
        });
        areaRowBox.getChildren().add(addMethodButton);
        classOutlineScene.setAddMethodButton(addMethodButton);

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
                TextField textField = getField(field);
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
                TextField textField = getField(field);
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


    private void buildMethodEditingScene(PsiMethod method) {
        // Build the scene components.
        VBox root = new VBox();
        methodEditingScene = new MethodEditingScene(root, method, this);
    }


    private void buildEmptyMethodEditingScene() {
        VBox root = new VBox();
        methodEditingScene = new MethodEditingScene(root, this);
    }


    public void setSceneToClassOutline() {
        fxPanel.setScene(classOutlineScene);
        methodEditingScene.removeEventHandler(KeyEvent.KEY_PRESSED, methodEditingScene);
        setKeyboardFocus();
        highlightFocusedComponent();
    }


    public void setSceneToMethodEditing() {
        fxPanel.setScene(methodEditingScene);
        classOutlineScene.removeEventHandler(KeyEvent.KEY_PRESSED, this);
        methodEditingScene.getBackButton().requestFocus();
    }
}