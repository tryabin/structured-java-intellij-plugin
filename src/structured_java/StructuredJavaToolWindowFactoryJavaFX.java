package structured_java;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiMethod;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class StructuredJavaToolWindowFactoryJavaFX implements ToolWindowFactory {

    private Project project;
    private ClassOutlineScene classOutlineScene;
    private MethodEditingScene methodEditingScene;
    private JFXPanel fxPanel;

    public Project getProject() {
        return project;
    }

    public ClassOutlineScene getClassOutlineScene() {
        return classOutlineScene;
    }

    public MethodEditingScene getMethodEditingScene() {
        return methodEditingScene;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        fxPanel = new JFXPanel();
        JComponent component = toolWindow.getComponent();

        // Start the Structured Java tool window UI.
        DumbService.getInstance(project).smartInvokeLater(() -> Platform.runLater(() -> {
            // Build the scenes and start on the class outline scene.
            methodEditingScene = new MethodEditingScene(new VBox(), this);
            classOutlineScene = new ClassOutlineScene(new VBox(), this);
            setSceneToClassOutlineScene();
        }));

        component.getParent().add(fxPanel);
    }
    

    public void setSceneToClassOutlineScene() {
        classOutlineScene = new ClassOutlineScene(new VBox(), this);
        fxPanel.setScene(classOutlineScene);
        methodEditingScene.removeEventHandler(KeyEvent.KEY_PRESSED, methodEditingScene);
    }


    public void setSceneToEmptyMethodEditingScene() {
        methodEditingScene = new MethodEditingScene(new VBox(), this);
        switchToMethodEditingScene();
    }


    public void setSceneToMethodEditingScene(PsiMethod method) {
        methodEditingScene = new MethodEditingScene(new VBox(), method, this);
        switchToMethodEditingScene();
    }


    private void switchToMethodEditingScene() {
        fxPanel.setScene(methodEditingScene);
        classOutlineScene.removeEventHandler(KeyEvent.KEY_PRESSED, classOutlineScene);
        methodEditingScene.getBackButton().requestFocus();
    }
}