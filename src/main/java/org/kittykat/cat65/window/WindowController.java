package org.kittykat.cat65.window;

import javafx.stage.Stage;
import org.kittykat.cat65.core.MainManager;

public abstract class WindowController {
    protected final Window window;

    public WindowController(String title, String fxml) {
        this(title, fxml, null);
    }
    public WindowController(String title, String fxml, Stage stage) {
        this.window = new Window(title, fxml, this, stage);
    }

    public void updateWindow() {}

    public void onClose() {
        MainManager.mainWindowController.closeWindow(this);
    }

    public Window getWindow() {
        return window;
    }
}
