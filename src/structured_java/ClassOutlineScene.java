package structured_java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;

import static javafx.scene.input.KeyCode.*;
import static structured_java.UserInterfaceUtilities.getField;

public class ClassOutlineScene extends Scene implements EventHandler<KeyEvent> {

    // The link back to the main UI
    private StructuredJavaToolWindowFactoryJavaFX ui;

    // Constants
    private static final String DEFAULT_HIGHLIGHT_COLOR = "-fx-background-color: #308cfc;";
    private static final List<Area> areaOrdering = Arrays.asList(Area.VARIABLE, Area.METHOD, Area.ENUM, Area.INNER_CLASS);

    // Data elements
    private PsiClass currentClass;
    private KeyboardFocusInfo keyboardFocusInfo;
    private List<PsiField> variables = new ArrayList<>();
    private List<PsiMethod> methods = new ArrayList<>();
    private List<PsiClass> enums = new ArrayList<>();
    private List<PsiClass> innerClasses = new ArrayList<>();
    private List<List<PsiNamedElement>> dataAreas = new ArrayList<>();

    // GUI components
    private VBox root;
    private Button addVariableButton;
    private Button addMethodButton;
    private ComboBox<String> newVariableAccessModifierBox;
    private ComboBox<String> newVariableStaticModifierBox;
    private TextField newVariableTypeField;
    private TextField newVariableNameField;
    private TextField newVariableInitialValueField;
    private List<List<ComboBox<String>>> variableModifierComboBoxes = new ArrayList<>();
    private List<TextField> variableNameTextFields = new ArrayList<>();
    private List<TextField> variableInitialValueTextFields = new ArrayList<>();
    private List<TextField> methodNameTextFields = new ArrayList<>();

    // Handlers
    private EventHandler<ActionEvent> addVariableHandler;


    public KeyboardFocusInfo getKeyboardFocusInfo() {
        return keyboardFocusInfo;
    }

    public ClassOutlineScene(VBox root, StructuredJavaToolWindowFactoryJavaFX ui) {
        super(root);
        this.ui = ui;
        this.root = root;

        // Add a key filter to the scene.
        addEventFilter(KeyEvent.KEY_PRESSED, this);

        // Build the class outline scene.
        addVariableHandler = new AddVariableHandler(ui.getProject(), this);
        keyboardFocusInfo = new KeyboardFocusInfo();
        buildClassOutlineScene();
    }


    protected void buildClassOutlineScene() {

        // Get all data in the currently opened class.
        Project project = ui.getProject();
        currentClass = Utilities.getCurrentClass(project);
        variables.clear();
        variableModifierComboBoxes.clear();
        variableNameTextFields.clear();
        variableInitialValueTextFields.clear();
        variables.addAll(Arrays.asList(ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>)currentClass::getFields)));
        methods.clear();
        methodNameTextFields.clear();
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

        // Focus on the correct component and highlight the correct area.
        setKeyboardFocus();
        highlightFocusedComponent();
    }


    private VBox buildVariablesArea() {
        // Build the component holding the rows.
        VBox areaRowBox = new VBox();
        for (PsiField variable : variables) {
            HBox rowBox = new HBox();
            rowBox.setSpacing(5);

            // Modifiers
            PsiElement[] modifiers = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> variable.getModifierList().getChildren());
            List<ComboBox<String>> currentModifiers = new ArrayList<>();
            for (PsiElement modifier : modifiers) {
                if (modifier.getText().trim().isEmpty()) {
                    continue;
                }
                ComboBox<String> modifierBox = new ComboBox<>(FXCollections.observableArrayList(PsiModifier.MODIFIERS));
                modifierBox.getSelectionModel().select(modifier.getText());
                currentModifiers.add(modifierBox);
                rowBox.getChildren().add(modifierBox);
            }
            variableModifierComboBoxes.add(currentModifiers);

            // Type
            String variableType = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> variable.getType().getPresentableText());
            TextField variableTypeField = getField(variableType);
            rowBox.getChildren().add(variableTypeField);

            // Name
            String variableName = ApplicationManager.getApplication().runReadAction((Computable<String>) variable::getName);
            TextField nameField = getField(variableName);
            variableNameTextFields.add(nameField);
            rowBox.getChildren().add(nameField);

            // Add an equals sign label and a text field for the initial value.
            // The equals sign and text field are only shown when the variable has an initial value,
            // otherwise a button is shown to add an initial value.

            // '=' label.
            Label equalsSign = new Label(" = ");
            rowBox.getChildren().add(equalsSign);

            // Initial value field.
            TextField initialValueField = getField();
            variableInitialValueTextFields.add(initialValueField);
            rowBox.getChildren().add(initialValueField);

            // Add initial value button.
            Button addInitialValueButton = new Button("Set Initial Value");
            rowBox.getChildren().add(addInitialValueButton);

            // When the button is pressed it is hidden and the text field is made visible.
            addInitialValueButton.setOnAction(e -> {
                addInitialValueButton.setVisible(false);
                addInitialValueButton.setManaged(false);
                equalsSign.setVisible(true);
                equalsSign.setManaged(true);
                initialValueField.setVisible(true);
                initialValueField.setManaged(true);
                initialValueField.setText("<Initial Value>");
                initialValueField.selectAll();
                initialValueField.requestFocus();
            });

            // If the variable has an initial value then hide the button and set the initial value field
            // to the initial value.
            boolean variableHasInitializer =  ApplicationManager.getApplication().runReadAction((Computable<Boolean>) variable::hasInitializer);
            if (variableHasInitializer) {
                String variableInitialValue = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> variable.getInitializer().getText());
                initialValueField.setText(variableInitialValue);

                addInitialValueButton.setVisible(false);
                addInitialValueButton.setManaged(false);
            }

            // Otherwise hide the initial value field and the equals sign.
            else {
                equalsSign.setVisible(false);
                equalsSign.setManaged(false);
                initialValueField.setVisible(false);
                initialValueField.setManaged(false);
            }

            areaRowBox.getChildren().add(rowBox);
        }

        // The row for adding a new variable.
        HBox newVariableRow = new HBox();
        newVariableRow.setSpacing(5);

        // The access modifier dropdown.
        newVariableAccessModifierBox = new ComboBox<>(FXCollections.observableArrayList("private", "protected", "public", "None"));
        newVariableAccessModifierBox.getSelectionModel().selectFirst();
        newVariableRow.getChildren().add(newVariableAccessModifierBox);

        // Static or non-stick dropdown.
        newVariableStaticModifierBox = new ComboBox<>(FXCollections.observableArrayList("non-static", "static"));
        newVariableStaticModifierBox.getSelectionModel().selectFirst();
        newVariableRow.getChildren().add(newVariableStaticModifierBox);

        // Type text field.
        newVariableTypeField = getField("<Type>");
        newVariableRow.getChildren().add(newVariableTypeField);

        // Name field.
        newVariableNameField = getField("<Name>");
        newVariableRow.getChildren().add(newVariableNameField);

        // '=' label.
        Label equalsSign = new Label(" = ");
        newVariableRow.getChildren().add(equalsSign);

        // Initial value field.
        newVariableInitialValueField = getField("<Initial Value>");
        newVariableRow.getChildren().add(newVariableInitialValueField);

        // The button to add a variable.
        addVariableButton = new Button("Add Variable");
        addVariableButton.setOnAction(addVariableHandler);
        newVariableRow.getChildren().add(addVariableButton);
        areaRowBox.getChildren().add(newVariableRow);

        // Build the root component of the area.
        VBox area = new VBox();
        Label label = new Label("Variables");
        area.getChildren().add(label);
        area.getChildren().add(areaRowBox);

        return area;
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
        addMethodButton = new Button("Add Method");
        addMethodButton.setOnAction(event -> {
            ui.setSceneToEmptyMethodEditingScene();
        });
        areaRowBox.getChildren().add(addMethodButton);

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


    @Override
    public void handle(KeyEvent event) {
        // Get the focused area and component.
        Area currentArea = areaOrdering.get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedArea = getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);
        Node focusOwner = focusOwnerProperty().get();

        // Consume the event in certain situations to prevent undesirable effects.
        boolean enterPressed = event.getCode() == ENTER;
        boolean tabPressed = event.getCode() == TAB;
        boolean leftOrRightPressed = event.getCode() == LEFT || event.getCode() == RIGHT;
        boolean textFieldFocused = focusOwner instanceof TextField;
        boolean comboBoxFocused = focusOwner instanceof ComboBox;
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
            if(focusOwner instanceof Button) {
                ((Button)focusOwner).fire();
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
                                ui.setSceneToMethodEditingScene(selectedMethod);
                                break;
                            default:
                                keyboardFocusInfo.setFocusLevel(KeyboardFocusInfo.FocusLevel.COLUMN);
                                keyboardFocusInfo.setFocusedColumn(0);
                        }
                        break;
                    }
                    case COLUMN: {

                        // Apply the changes for the row unless it's the last row
                        // because that adds a new element.
                        int numRowsInCurrentArea = focusedRowArea.getChildren().size();
                        if (keyboardFocusInfo.getFocusedRow() != numRowsInCurrentArea - 1) {
                            // Do a rename operation if applicable.
                            handleRename();

                            // Make the changes to a variable if applicable.
                            if (currentArea == Area.VARIABLE) {
                                setVariableInitialValue();
                                setVariableModifiers();
                            }

                            // Rebuild the UI.
                            buildClassOutlineScene();
                        }

                        // Move the focus to row selection and rebuild the ui.
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
                    // Skip labels and hidden components.
                    Node elementToBeFocused;
                    do {
                        moveFocusRightOneColumn();
                        HBox row = (HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow());
                        elementToBeFocused = row.getChildren().get(keyboardFocusInfo.getFocusedColumn());
                    }
                    while (elementToBeFocused instanceof Label || !elementToBeFocused.isVisible());

                    // If the focused component is a text field then highlight the text.
                    selectAllTextIfNavigatedToTextField();
                }
                else {
                    moveFocusDownForAreaOrRow();
                }
            }
            else {
                if (keyboardFocusInfo.getFocusLevel() == KeyboardFocusInfo.FocusLevel.COLUMN) {
                    // Skip labels and hidden components.
                    Node elementToBeFocused;
                    do {
                        moveFocusLeftOneColumn();
                        HBox row = (HBox) focusedRowArea.getChildren().get(keyboardFocusInfo.getFocusedRow());
                        elementToBeFocused = row.getChildren().get(keyboardFocusInfo.getFocusedColumn());
                    }
                    while (elementToBeFocused instanceof Label || !elementToBeFocused.isVisible());

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
                        WriteCommandAction.writeCommandAction(ui.getProject()).run(variableToDelete::delete);

                        // Wait until the number of variables in the class changes.
                        Utilities.waitForNumberOfVariablesInClassToChange(variables.size(), currentClass);

                        // Rebuild the UI.
                        buildClassOutlineScene();
                        break;
                    }
                    case METHOD: {
                        PsiMethod methodToDelete = methods.get(keyboardFocusInfo.getFocusedRow());
                        WriteCommandAction.writeCommandAction(ui.getProject()).run(methodToDelete::delete);

                        // Wait until the number of methods in the class changes.
                        Utilities.waitForNumberOfMethodsInClassToChange(methods.size(), currentClass);

                        // Rebuild the UI.
                        buildClassOutlineScene();
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

        // Get the IntelliJ reference to the element of the row to rename.
        List<PsiNamedElement> psiNamedElements = dataAreas.get(keyboardFocusInfo.getFocusedAreaIndex());
        PsiNamedElement psiElement = psiNamedElements.get(keyboardFocusInfo.getFocusedRow());

        // Get the current name of the element in the text field.
        TextField currentNameField = null;
        switch (areaOrdering.get(keyboardFocusInfo.getFocusedAreaIndex())) {
            case VARIABLE:
                currentNameField = variableNameTextFields.get(keyboardFocusInfo.getFocusedRow());
                break;
            case METHOD:
                currentNameField = methodNameTextFields.get(keyboardFocusInfo.getFocusedRow());
                break;
        }

        // Define the function to rename the variable.
        String textFieldName = currentNameField.getText();
        Runnable renameVariableAction = () ->
        {
            for (PsiReference reference : ReferencesSearch.search(psiElement)) {
                reference.handleElementRename(textFieldName);
            }
            psiElement.setName(textFieldName);
        };

        // Rename the element if the name changed.
        String originalName = ApplicationManager.getApplication().runReadAction((Computable<String>) psiElement::getName);
        if (!textFieldName.equals(originalName)) {
            WriteCommandAction.runWriteCommandAction(ui.getProject(), renameVariableAction);
        }
    }


    private void setVariableInitialValue() {

        // Get the current initial value in the source and the value in the initial value text field.
        PsiField currentVariable = variables.get(keyboardFocusInfo.getFocusedRow());
        String textFieldInitialValue = variableInitialValueTextFields.get(keyboardFocusInfo.getFocusedRow()).getText();

        // Define the function to change the initial value.
        Runnable setInitialValueAction = () ->
        {
            PsiExpression newInitialValue = null;

            // Create a new PsiExpression from the new initial value if it is non-empty.
            if (!textFieldInitialValue.trim().isEmpty()) {
                newInitialValue = PsiElementFactory.getInstance(ui.getProject()).createExpressionFromText(textFieldInitialValue, currentVariable.getInitializer());
            }
            currentVariable.setInitializer(newInitialValue);
        };

        // If the initial value changed then update the source.
        String originalInitialValue = "";
        boolean variableHasInitializer = ApplicationManager.getApplication().runReadAction((Computable<Boolean>) currentVariable::hasInitializer);
        if (variableHasInitializer) {
            originalInitialValue = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> currentVariable.getInitializer().getText());
        }
        if (!textFieldInitialValue.equals(originalInitialValue)) {
            WriteCommandAction.runWriteCommandAction(ui.getProject(), setInitialValueAction);
        }
    }


    private void setVariableModifiers() {

        // Get the current initial value in the source and the value in the initial value text field.
        PsiField currentVariable = variables.get(keyboardFocusInfo.getFocusedRow());
        PsiModifierList currentModifierList = ApplicationManager.getApplication().runReadAction((Computable<PsiModifierList>) currentVariable::getModifierList);

        // Build a new modifier list from the modifier combo boxes of the current variable.
        List<ComboBox<String>> currentVariableModifierBoxes = variableModifierComboBoxes.get(keyboardFocusInfo.getFocusedRow());

        // Define the function to change the list of modifiers for the variable.
        WriteCommandAction.runWriteCommandAction(ui.getProject(), () ->
        {
            // Remove all current modifiers from the variable.
            for (PsiElement modifier : currentModifierList.getChildren()) {
                if (modifier.getText().trim().isEmpty()) {
                    continue;
                }
                currentModifierList.setModifierProperty(modifier.getText(), false);
            }

            // Set new modifiers based on the modifier boxes for the variable.
            for (ComboBox<String> modifierBox : currentVariableModifierBoxes) {
                currentModifierList.setModifierProperty(modifierBox.getValue(), true);
            }
        });
    }


    private void moveFocusDownForAreaOrRow() {
        moveFocusForAreaOrRow(1);
    }


    private void moveFocusUpForAreaOrRow() {
        moveFocusForAreaOrRow(-1);
    }


    private void moveFocusForAreaOrRow(int indexIncrement) {
        List<VBox> areas = getAreas();
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
        List<VBox> areas = getAreas();
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
        setNullStyleRecursively((VBox)getRoot());

        // Get the area of focus.
        VBox areaOfFocus = getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());

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
        if (focusOwnerProperty().get() instanceof TextField) {
            TextField focusedTextField = (TextField) focusOwnerProperty().get();
            focusedTextField.selectAll();
        }
    }


    protected void setKeyboardFocus() {
        VBox focusedArea = getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
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
        VBox focusedArea = getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
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

        // Access Modifier
        if (!newVariableAccessModifierBox.getValue().equals("None")) {
            sourceText += newVariableAccessModifierBox.getValue() + " ";
        }

        // Static / Non-Static Modifier
        if (newVariableStaticModifierBox.getValue().equals("static")) {
            sourceText += newVariableStaticModifierBox.getValue() + " ";
        }

        // Type
        sourceText += newVariableTypeField.getText() + " ";

        // Name
        sourceText += newVariableNameField.getText();

        // Add the initial value if it is non-empty.
        if (newVariableInitialValueField.getText().trim().length() > 0) {
            sourceText += " = " + newVariableInitialValueField.getText();
        }

        sourceText += ";";

        return sourceText;
    }


    public HBox buildMethodRow(PsiMethod method) {
        HBox rowBox = new HBox();
        rowBox.setSpacing(5);

        // Modifiers
        PsiElement[] modifiers = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> method.getModifierList().getChildren());
        for (PsiElement modifier : modifiers) {
            // Skip whitespace. For some reason the list of modifiers includes whitespace.
            if (modifier instanceof PsiWhiteSpace) {
                continue;
            }
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
        TextField methodNameField = getField(methodName);
        methodNameTextFields.add(methodNameField);
        rowBox.getChildren().add(methodNameField);

        return rowBox;
    }


    private VBox getMethodFullParametersComponent(PsiMethod method) {
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


    /**
     * This handler is attached to text fields so they can be exited with some key combination.
     * @return
     */
    private EventHandler<KeyEvent> getExitTextFieldHandler() {
        return event -> {
            VBox focusedArea = getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
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
}
