package org.kittykat.cat65.core;

import javafx.beans.property.*;
import org.kittykat.cat65.settings.NewLineVariant;
import org.kittykat.cat65.window.SerialTerminalController;

public abstract class ConfigManager {
    // 1kHz -> 1ms = 1_000_000ns
    // 1MHz -> 1µs = 1_000ns
    public static final int MIN_CLOCK_PERIOD = 100;          // 10 MHz
    public static final int MAX_CLOCK_PERIOD = 100_000_000;  // 10 Hz
    public static final int DEFAULT_CLOCK_PERIOD = 1_000;    //  1 MHz
    public static final IntegerProperty clockPeriodProperty = new SimpleIntegerProperty(DEFAULT_CLOCK_PERIOD);
    public static int clockPeriod = clockPeriodProperty.get();

    public static final ObjectProperty<NewLineVariant> terminalNewLineVariant = new SimpleObjectProperty<>(NewLineVariant.CR);

    public static final BooleanProperty vsyncProperty = new SimpleBooleanProperty(true);
    public static boolean vsync = vsyncProperty.get();
    public static final BooleanProperty syncVideoClocksProperty = new SimpleBooleanProperty(true);
    public static boolean syncVideoClocks = syncVideoClocksProperty.get();

    public static final int MIN_MASTER_VOL     = 0;
    public static final int MAX_MASTER_VOL     = 100;
    public static final int DEFAULT_MASTER_VOL = MAX_MASTER_VOL;
    public static final IntegerProperty masterVolumeProperty = new SimpleIntegerProperty(DEFAULT_MASTER_VOL);
    public static float masterVolume = masterVolumeProperty.get() / 100f;

    static {
        clockPeriodProperty.addListener((obs, oldVal, newVal) -> clockPeriod = (int) newVal);
        vsyncProperty.addListener((obs, oldVal, newVal) -> vsync = newVal);
        syncVideoClocksProperty.addListener((obs, oldVal, newVal) -> syncVideoClocks = newVal);
        masterVolumeProperty.addListener((obs, oldVal, newVal) -> masterVolume = ((float) newVal) / 100f);
    }

    public static double getTargetClockSpeed() {
        return (1_000_000_000d / ConfigManager.clockPeriodProperty.get());
    }

    public static void outputSerial(char c) {
        SerialTerminalController terminal = MainManager.mainWindowController.terminal;
        if (terminal != null) terminal.print(c);
    }
    public static boolean isSerialBufferEmpty() {
        SerialTerminalController terminal = MainManager.mainWindowController.terminal;
        if (terminal != null) {
            return terminal.isBufferEmpty();
        }
        return true;
    }
    public static char pollSerial() {
        char c = '\0';
        SerialTerminalController terminal = MainManager.mainWindowController.terminal;
        if (terminal != null) {
            c = terminal.pollChar();
        }
        return c;
    }
}
