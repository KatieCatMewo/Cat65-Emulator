package org.kittykat.cat65.window;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.ConfigManager;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.DebugData;
import org.kittykat.cat65.core.expansion.ExpansionManager;

import java.io.File;
import java.util.ArrayList;

import static org.kittykat.cat65.core.ConfigManager.*;

public class MainWindowController extends WindowController {
    private static final String WINDOW_NAME = "Cat65 Emulator";

    private final ArrayList<WindowController> windows = new ArrayList<>();
    public SerialTerminalController terminal = null;

    private final FileChooser romChooser = new FileChooser();
    private File romDir = new File(System.getProperty("user.home"));

    @FXML private MenuItem mi_pause;
    @FXML private Button btn_pause;
    @FXML private Label lbl_paused;
    @FXML private Spinner<Integer> spn_clock;
    @FXML private Label lbl_targetFrequency;
    @FXML private Label lbl_currentFrequency;

    public MainWindowController(Stage mainStage) {
        super(WINDOW_NAME, "main", mainStage);
        mainStage.setResizable(false);
        initRomChooser();
        initClockSpinner();
        startUiThread();
        openSerialTerminal();
        mainStage.requestFocus();
        updatePausedState();
    }

    private void initRomChooser() {
        romChooser.setTitle("Open ROM");
        romChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ROMs", "*.c65", "*.rom", "*.bin"),
                new FileChooser.ExtensionFilter("all files", "*")
        );
    }
    private void initClockSpinner() {
        EmuHelper.setupIntSpinner(spn_clock, MIN_CLOCK_PERIOD, MAX_CLOCK_PERIOD, clockPeriodProperty.get(), 1);
        EmuHelper.bindSpinner(spn_clock, clockPeriodProperty);
        spn_clock.valueProperty().addListener((obs, oldVal, newVal) ->
                lbl_targetFrequency.setText("(%s)".formatted(EmuHelper.formatFrequency(1_000_000_000d / newVal)))
        );
    }

    private void startUiThread() {
        AnimationTimer UITimer = new AnimationTimer() {
            @SuppressWarnings("FieldCanBeLocal")
            private final long updateTime = (long) (1_000_000_000L / 60d);
            private long lastUpdate = 0L;

            @Override
            public void handle(long now) {
                if ((now - lastUpdate) < updateTime) return;
                if (!MainManager.running) stop();

                lastUpdate = now;
                synchronized (windows) {
                    DebugData.uiUpdate();
                    ExpansionManager.updateWindows();
                    for (WindowController wc : windows) {
                        wc.updateWindow();
                    }
                    updateWindow();
                }
            }
        };
        UITimer.start();
    }

    @Override
    public void updateWindow() {
        double target = ConfigManager.getTargetClockSpeed();
        double current = DebugData.getCurrentClockSpeed();
        String unit = "Hz";
        if (target >= 1_000_000d) {
            current /= 1_000_000d;
            unit = "MHz";
        } else if (target >= 1_000d) {
            current /= 1_000d;
            unit = "kHz";
        }
        lbl_currentFrequency.setText("(Current: %.2f %s)".formatted(current, unit));
    }

    public void openWindow(WindowController window) {
        synchronized (windows) {
            windows.add(window);
        }
    }
    public void closeWindow(WindowController window) {
        synchronized (windows) {
            windows.remove(window);
        }
        ExpansionManager.closeWindow(window);
        if (window == terminal) {
            terminal = null;
        }
    }

    @FXML
    @Override
    public void onClose() {
        for (int w = windows.size() - 1; w >= 0; w--) {
            windows.get(w).window.close();
        }
        MainManager.stop();
    }

    private void updatePausedState() {
        window.setTitle("%s%s".formatted(WINDOW_NAME, MainManager.paused ? " (Paused)" : ""));
        lbl_paused.setVisible(MainManager.paused);
        String str = MainManager.paused ? "Resume" : "Pause";
        mi_pause.setText(str);
        btn_pause.setText(str);
    }

    @FXML
    private void openConfig() {
        openWindow(new ConfigWindowController("general"));
    }

    @FXML
    private void openROM() {
        romChooser.setInitialDirectory(romDir);
        File file = romChooser.showOpenDialog(getWindow().getStage());
        if (file != null) {
            romDir = file.getParentFile();
            MainManager.loadROM(file);
        }
    }

    @FXML
    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    private void togglePause() {
        MainManager.paused = !MainManager.paused;
        updatePausedState();
    }
    @FXML
    private void step() {
        MainManager.step = true;
    }
    @FXML
    private void onReset() {
        MainManager.reset = true;
    }

    @FXML
    private void openSerialTerminal() {
        if (terminal == null) {
            terminal = new SerialTerminalController();
            openWindow(terminal);
        } else {
            Stage s = terminal.getWindow().getStage();
            s.setIconified(false);
            s.requestFocus();
        }
    }
    @FXML
    private void openLCD() {
        // ToDo: open LCD Window
    }

    @FXML
    private void viewEx04() {
        ExpansionManager.openWindow(0);
    }
    @FXML
    private void viewEx07() {
        ExpansionManager.openWindow(1);
    }
    @FXML
    private void openExpansionSetup() {
        openWindow(new ConfigWindowController("expansion"));
    }

    @FXML
    private void openCpuStatus() {
        openWindow(new CPUStatusController(MainManager.getCpu()));
    }
    @FXML
    private void openMemoryView() {
        openWindow(new MemoryViewController());
    }

    @FXML
    private void openAudioDebug() {
        openWindow(new AudioDebugController());
    }
}
