package org.kittykat.cat65;

import javafx.application.Application;
import javafx.stage.Stage;
import org.kittykat.cat65.core.AudioManager;
import org.kittykat.cat65.core.MainManager;

public class Cat65 extends Application {
    public static final String RESOURCE_PATH = "/org/kittykat/cat65/";

    @Override
    public void stop() {
        MainManager.stop();
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void start(Stage mainStage) {
        try {
            MainManager.startClockThread();
            AudioManager.startAudioThread();
            MainManager.startUi(mainStage);
        } catch (Exception e) {
            System.err.println("[!] Uncaught exception in Cat65.start()");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.err.println("[!] please start it from Launcher and not from Cat65!");
        System.exit(-1);
    }
}
