package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.expansion.ExpansionManager;
import org.kittykat.cat65.settings.ExpansionPort;
import org.kittykat.cat65.settings.NewLineVariant;

import static org.kittykat.cat65.core.ConfigManager.*;

public class ConfigWindowController extends WindowController {
    @FXML private TabPane tabPane;

    @FXML private Spinner<Integer> spn_clock;
    @FXML private Label lbl_frequency;

    @FXML private ComboBox<NewLineVariant> cbx_newLineMode;

    @FXML private ComboBox<ExpansionPort> cbx_ex04;
    @FXML private ComboBox<ExpansionPort> cbx_ex07;

    @FXML private CheckBox check_vsync;
    @FXML private CheckBox check_syncVideoClocks;

    @FXML private Spinner<Integer> spn_volume;
    @FXML private Slider slider_volume;

    public ConfigWindowController(String initialTab) {
        super("Cat65 Settings", "config");
        tabPane.getSelectionModel().select(getTabIndex(initialTab));
        initGeneralTab();
        initSerialTab();
        initExpansionTab();
        initVideoTab();
        initAudioTab();
    }

    public static int getTabIndex(String tab) {
        return switch (tab) {
            case "general"   -> 0;
            case "serial"    -> 1;
            case "expansion" -> 2;
            case "video"     -> 3;
            case "audio"     -> 4;
            default -> throw new RuntimeException("Config tab %s does not exist".formatted(tab));
        };
    }

    private void initGeneralTab() {
        EmuHelper.setupIntSpinner(spn_clock, MIN_CLOCK_PERIOD, MAX_CLOCK_PERIOD, DEFAULT_CLOCK_PERIOD, 1);
        EmuHelper.bindSpinner(spn_clock, clockPeriodProperty);
        spn_clock.valueProperty().addListener((obs, oldVal, newVal) -> updateFrequencyLabel(newVal));
        updateFrequencyLabel(clockPeriodProperty.get());
    }
    private void initSerialTab() {
        EmuHelper.setupComboBox(cbx_newLineMode, terminalNewLineVariant, NewLineVariant.class, NewLineVariant.stringConverter);
    }
    private void initExpansionTab() {
        EmuHelper.setupComboBox(cbx_ex04, ExpansionManager.ex04Property, ExpansionPort.class, ExpansionPort.stringConverter);
        EmuHelper.setupComboBox(cbx_ex07, ExpansionManager.ex07Property, ExpansionPort.class, ExpansionPort.stringConverter);
    }
    private void initVideoTab() {
        check_vsync.selectedProperty().bindBidirectional(vsyncProperty);
        check_syncVideoClocks.selectedProperty().bindBidirectional(syncVideoClocksProperty);
    }
    private void initAudioTab() {
        EmuHelper.setupIntSpinner(spn_volume, MIN_MASTER_VOL, MAX_MASTER_VOL, masterVolumeProperty.get(), 1);
        EmuHelper.bindSpinner(spn_volume, masterVolumeProperty);

        slider_volume.valueProperty().bindBidirectional(masterVolumeProperty);
    }

    private void updateFrequencyLabel(int period) {
        lbl_frequency.setText("(%s)".formatted(EmuHelper.formatFrequency(1_000_000_000f / period)));
    }
}
