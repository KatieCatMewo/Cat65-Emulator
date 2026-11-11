package org.kittykat.cat65.window;

import javafx.stage.Stage;

import java.util.ArrayList;

public class WindowManager extends WindowController {
    protected final ArrayList<WindowController> windows = new ArrayList<>();

    public WindowManager(String title, String fxml) {
        this(title, fxml, null);
    }
    public WindowManager(String title, String fxml, Stage stage) {
        super(title, fxml, stage);
    }

    public void openWindow(WindowController window) {
        synchronized (windows) {
            windows.add(window);
        }
    }
    public void closeWindow(WindowController window) {
        synchronized (windows) {
            for (WindowController wc : windows) {
                if (wc instanceof WindowManager manager) {
                    manager.closeWindow(window);
                }
            }
            windows.remove(window);
        }
    }

    public void updateWindow() {
        synchronized (windows) {
            for (WindowController wc : windows) {
                wc.updateWindow();
            }
        }
    }

    public void onClose() {
        super.onClose();
        synchronized (windows) {
            for (int w = windows.size() - 1; w >= 0; w--) {
                windows.get(w).window.close();
            }
        }
    }
}
