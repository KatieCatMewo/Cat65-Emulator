package org.kittykat.cat65;

import javafx.application.Application;
import javafx.stage.Stage;
import org.kittykat.cat65.core.AudioManager;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.DebugData;
import org.kittykat.cat65.core.expansion.ExpansionData;
import org.kittykat.cat65.core.expansion.ExpansionManager;

public class Cat65 extends Application {
    public static final String RESOURCE_PATH = "/org/kittykat/cat65/";

    public static final int    SAMPLE_RATE = 48_000;
    public static final double SAMPLE_PERIOD_NANOS = 1_000_000_000d / SAMPLE_RATE;
    public static final float  SPACING = 8f;

    @Override
    public void stop() {
        MainManager.stop();
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void start(Stage mainStage) {
        try {
            ExpansionManager.init();
            ExpansionData.init();
            DebugData.init();

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
