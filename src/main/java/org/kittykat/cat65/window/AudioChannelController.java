package org.kittykat.cat65.window;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.ConfigManager;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.expansion.devices.AudioCard;

public class AudioChannelController extends WindowController {
    private static final String[] CHANNEL_NAMES = {" Noise ", " Tri 1 ", " Tri 2 ", "Pulse 1", "Pulse 2", "Pulse 3", "Pulse 4", "Pulse 5"};
    private static final String[] DUTY_CYCLES   = {"50.0", "37.5", "25.0", "12.5"};

    private final AudioCard audioCard;
    @FXML private VBox channelView;
    @FXML private CheckBox check_master;

    public AudioChannelController(AudioCard audioCard) {
        super("2A65 Audio Registers", "audio-card");
        this.audioCard = audioCard;

        window.getStage().setResizable(false);
        ObservableList<Node> children = channelView.getChildren();

        check_master.setOnAction(event -> {
            audioCard.masterEnable = check_master.isSelected();
            ObservableList<Node> c = channelView.getChildren();
            int i = 0;
            for (Node n : c) {
                if (i++ != 0) {
                    n.setDisable(!audioCard.masterEnable);
                }
            }
        });

        for (int c = 0; c < CHANNEL_NAMES.length; c++) {
            Label lbl_reg = new Label();
            lbl_reg.getStyleClass().add("register");

            final int channel = c;
            CheckBox channelEnable = new CheckBox();
            channelEnable.setSelected(true);
            channelEnable.setOnAction(event -> {
                boolean enabled = channelEnable.isSelected();
                audioCard.channelStates[channel] = enabled;
                lbl_reg.setDisable(!enabled);
            });
            children.add(new HBox(8d, lbl_reg, channelEnable));
        }
    }

    @FXML
    private void openAdioConfig() {
        MainManager.mainWindowController.openWindow(new ConfigWindowController("audio"));
    }

    @Override
    public void updateWindow() {
        int[] volumeLevels = audioCard.getVolumeLevels();
        int[] frequencies  = audioCard.getFrequencies();
        int[] counters     = audioCard.getCounters();
        int[] states       = audioCard.getStates();
        int[] dutyCycles   = audioCard.getDutyCycles();

        int c = -1;
        int vol;
        int freq;
        int count;
        int state;
        int duty;
        int noiseFreq;
        double hz;
        String str;
        for (Node node : channelView.getChildren()) {
            if (node instanceof HBox hBox) {
                if (hBox.getChildren().getFirst() instanceof Label label) {
                    if (c >= 0) {
                        vol   = volumeLevels[c];
                        freq  = frequencies[c];
                        count = counters[c];
                        state = states[c];
                        duty  = dutyCycles[c];

                        if (c == 0) {
                            noiseFreq = AudioCard.NOISE_FREQ_VALUES[freq];
                            hz = getFrequency(noiseFreq, 1);
                            str = "%s  |  $%1x  (%2d)  |     $%1x : $%03x (%9sHz)     |  $%03x (%4d)  |  %%%s  |              ".formatted(
                                    CHANNEL_NAMES[c], vol, vol, freq, noiseFreq, getFrequencyString(hz),
                                    count, count, EmuHelper.getBinary(state, 16)
                            );
                        } else {
                            hz = getFrequency(freq, (c <= 2) ? 32 : 8);
                            str = "%s  |  $%1x  (%2d)  |  $%03x (%9sHz  %8s)  |  $%03x (%4d)  |      $%02x  (%2d)      |  %s ".formatted(
                                    CHANNEL_NAMES[c], vol, vol, freq, getFrequencyString(hz), EmuHelper.getNote(hz),
                                    count, count, state, state,
                                    (duty >= 0) ? "%%%s (%s%%)".formatted(EmuHelper.getBinary(duty, 2), DUTY_CYCLES[duty]) : "           "
                            );
                        }
                        label.setText(str);
                    }
                    c++;
                }
            }
        }
    }

    private static double getFrequency(int value, int divisor) {
        return ConfigManager.getTargetClockSpeed() / (divisor * (4096 - value));
    }
    public static String getFrequencyString(double freq) {
        return "%.2f".formatted(freq);
    }
}
