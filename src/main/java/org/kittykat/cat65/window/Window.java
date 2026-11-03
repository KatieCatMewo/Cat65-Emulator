package org.kittykat.cat65.window;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kittykat.cat65.EmuHelper;

import java.io.IOException;

public class Window {
    private final Stage stage;
    private final WindowController controller;

    @SuppressWarnings("CallToPrintStackTrace")
    public Window(String title, String fxml, WindowController controller, Stage windowStage) {
        this.controller = controller;
        this.stage = (windowStage == null) ? new Stage() : windowStage;
        try {
            FXMLLoader loader = new FXMLLoader(EmuHelper.loadResource("fxml/window/%s.fxml".formatted(fxml)));
            loader.setController(controller);
            stage.setScene(new Scene(loader.load()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load fxml/window/%s.fxml".formatted(fxml));
        }
        stage.setTitle(title);
        stage.setOnCloseRequest(event -> close());
        stage.show();
    }

    public void close() {
        controller.onClose();
        stage.close();
    }

    public void setTitle(String title) {
        stage.setTitle(title);
    }

    public Stage getStage() {
        return stage;
    }
}
