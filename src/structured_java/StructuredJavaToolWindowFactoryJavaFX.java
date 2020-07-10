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
import static structured_java.UserInterfaceUtilities.buildMethodRow;
import static structured_java.UserInterfaceUtilities.getField;


public class StructuredJavaToolWindowFactoryJavaFX implements ToolWindowFactory, EventHandler<KeyEvent> {

    private static final String DEFAULT_HIGHLIGHT_COLOR = "-fx-background-color: #308cfc;";
    private static final List<Area> areaOrdering = Arrays.asList(Area.VARIABLE, Area.METHOD, Area.ENUM, Area.INNER_CLASS);

    private Project project;
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

    public KeyboardFocusInfo getKeyboardFocusInfo () { return keyboardFocusInfo; };

    public TextField getNewVariableField() {
        return classOutlineScene.getNewVariableTextField();
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
            buildClassOutlineScene();
            setSceneToClassOutline();
        }));

        component.getParent().add(fxPanel);
    }

    protected void buildClassOutlineScene() {
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
        VBox variablesArea = buildVariablesArea(classOutlineScene);
        VBox methodsArea = buildMethodsArea();
        VBox enumsArea = buildEnumsArea();
        VBox innerClassesArea = buildInnerClassesArea();

        // The component holding the variables.
        if (!variables.isEmpty()) {
            root.getChildren().add(variablesArea);
        }

        // The component holding the methods.
        if (!methods.isEmpty()) {
            root.getChildren().add(methodsArea);
        }

        // The component holding inner classes that are enums.
        if (!enums.isEmpty()) {
            root.getChildren().add(enumsArea);
        }

        // The component holding non-enum inner classes.
        if (!innerClasses.isEmpty()) {
            root.getChildren().add(innerClassesArea);
        }

        // Add a key handler to the scene.
        classOutlineScene.addEventFilter(KeyEvent.KEY_PRESSED, this);
    }

    @Override
    public void handle(KeyEvent event) {
        if (fxPanel.getScene() instanceof  ClassOutlineScene) {
            handleClassOutLineScene(event);
        }
        else if (fxPanel.getScene() instanceof MethodEditingScene) {
            handleMethodEditingScene(event);
        }
    }

    private void handleClassOutLineScene(KeyEvent event) {
        // Get the focused area and row area.
        VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
        VBox focusedRowArea = (VBox) focusedArea.getChildren().get(1);
        Area currentArea = areaOrdering.get(keyboardFocusInfo.getFocusedAreaIndex());

        // Consume the event if the focused component is NOT a text field,
        // or if the event is ENTER or TAB.
        if (!(classOutlineScene.focusOwnerProperty().get() instanceof TextField) ||
            event.getCode() == ENTER ||
            event.getCode() == TAB) {
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
            if (fxPanel.getScene() instanceof ClassOutlineScene && classOutlineScene.getAddVariableButton().isFocused()) {
                classOutlineScene.getAddVariableButton().fire();
            }
            else if (fxPanel.getScene() instanceof MethodEditingScene && methodEditingScene.getBackButton().isFocused()) {
                methodEditingScene.getBackButton().fire();
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
                        // Rename the variable if the focused column is the last column, which should be the name.
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
                        buildClassOutlineScene();
                        setSceneToClassOutline();
                    }
                }
            }
        }

        // Highlight the currently focused row.
        highlightFocusedComponent();

        // Set the keyboard focus to the correct element.
        setKeyboardFocus();
    }


    private void handleMethodEditingScene(KeyEvent event) {
        if (methodEditingScene.getBackButton().isFocused() && event.getCode() == ENTER) {
            methodEditingScene.getBackButton().fire();
        }
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


    protected void setKeyboardFocus() {
        VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
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
        VBox focusedArea = classOutlineScene.getAreas().get(keyboardFocusInfo.getFocusedAreaIndex());
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


    private VBox buildVariablesArea(ClassOutlineScene scene) {
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
                TextField textField = getField(field);
                rowBox.getChildren().add(textField);
            }

            areaRowBox.getChildren().add(rowBox);
        }

        // The row for adding a new variable.
        HBox newVariableRow = new HBox();

        // The text field for the new variable.
        TextField newVariableField = new TextField();
        newVariableRow.getChildren().add(newVariableField);
        scene.setNewVariableTextField(newVariableField);

        // The button to add a variable.
        Button addVariableButton = new Button("Add Variable");
        addVariableButton.setOnAction(new AddVariableHandler(this));
        newVariableRow.getChildren().add(addVariableButton);
        areaRowBox.getChildren().add(newVariableRow);
        scene.setAddVariableButton(addVariableButton);

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
        for (int i = 0; i < methods.size(); i++) {
            PsiMethod method = methods.get(i);
            HBox rowBox = buildMethodRow(method);

            // Add the row component to the area component.
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
        Button backButton = new Button("Back");
        backButton.setOnAction(event -> {
            buildClassOutlineScene();
            setSceneToClassOutline();
        });
        root.getChildren().add(backButton);

        // Method header
        HBox methodHeader = buildMethodRow(method);
        root.getChildren().add(methodHeader);

        // Build the new scene object.
        methodEditingScene = new MethodEditingScene(root);
        methodEditingScene.setBackButton(backButton);
        methodEditingScene.addEventFilter(KeyEvent.KEY_PRESSED, this);
    }


    public void setSceneToClassOutline() {
        fxPanel.setScene(classOutlineScene);
        setKeyboardFocus();
        highlightFocusedComponent();
    }


    public void setSceneToMethodEditing() {
        fxPanel.setScene(methodEditingScene);
        methodEditingScene.getBackButton().requestFocus();
    }
}