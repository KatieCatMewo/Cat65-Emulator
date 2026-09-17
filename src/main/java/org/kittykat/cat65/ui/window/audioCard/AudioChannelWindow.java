package org.kittykat.cat65.ui.window.audioCard;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.core.expansionDevices.AudioCard;
import org.kittykat.cat65.ui.window.WindowWithTitle;

import java.util.Arrays;

public class AudioChannelWindow extends WindowWithTitle {
    private static final String[] CHANNEL_NAMES = {" Noise ", " Tri 1 ", " Tri 2 ", "Pulse 1", "Pulse 2", "Pulse 3", "Pulse 4", "Pulse 5"};
    private static final String[] DUTY_CYCLES   = {"50.0", "37.5", "25.0", "12.5"};

    private final AudioCard audioCard;
    private final VBox channelView;

    private boolean masterEnable  = true;
    private final boolean[] channelStates = new boolean[CHANNEL_NAMES.length];

    public AudioChannelWindow(AudioCard audioCard) {
        super("Audio Registers");
        this.audioCard = audioCard;

        channelView = new VBox(Cat65.SPACING);
        ObservableList<Node> children = channelView.getChildren();

        Label lbl_master = new Label("Channel  |   Volume   |            Frequency            |    Counter    |        State        |     Duty     ");
        lbl_master.getStyleClass().add("register-view");
        CheckBox master = new CheckBox();
        master.setSelected(true);
        master.setOnAction(event -> {
            masterEnable = master.isSelected();
            ObservableList<Node> c = channelView.getChildren();
            int i = 0;
            for (Node n : c) {
                if (i++ != 0) {
                    n.setDisable(!masterEnable);
                }
            }
        });
        children.add(new HBox(Cat65.SPACING, lbl_master, master));

        for (int c = 0; c < CHANNEL_NAMES.length; c++) {
            Label lbl_reg = new Label();
            lbl_reg.getStyleClass().add("register-view");

            final int channel = c;
            CheckBox channelEnable = new CheckBox();
            channelEnable.setSelected(true);
            channelEnable.setOnAction(event -> {
                boolean enabled = channelEnable.isSelected();
                channelStates[channel] = enabled;
                lbl_reg.setDisable(!enabled);
            });
            children.add(new HBox(Cat65.SPACING, lbl_reg, channelEnable));
        }
        Arrays.fill(channelStates, true);

        getChildren().add(channelView);
    }

    public boolean isChannelEnabled(int c) {
        return masterEnable & channelStates[c];
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
                            str = "%s  |  $%1x  (%2d)  |     $%1x : $%03x (%9sHz)     |  $%03x (%4d)  |  %%%s  |              ".formatted(CHANNEL_NAMES[c],
                                    vol, vol, freq, noiseFreq, getFrequencyString(hz),
                                    count, count, EmuHelper.getBinary(state, 16));
                        } else {
                            hz = getFrequency(freq, (c <= 2) ? 32 : 8);
                            str = "%s  |  $%1x  (%2d)  |  $%03x (%9sHz  %8s)  |  $%03x (%4d)  |      $%02x  (%2d)      |  %s ".formatted(CHANNEL_NAMES[c],
                                    vol, vol, freq, getFrequencyString(hz), EmuHelper.getNote(hz), count, count, state, state,
                                    (duty >= 0) ? "%%%s (%s%%)".formatted(EmuHelper.getBinary(duty, 2), DUTY_CYCLES[duty]) : "           ");
                        }
                        label.setText(str);
                    }
                    c++;
                }
            }
        }
    }

    private static double getFrequency(int value, int divisor) {
        return CMU.getTargetClockSpeed() / (divisor * (4096 - value));
    }
    public static String getFrequencyString(double freq) {
        return "%.2f".formatted(freq);
    }
}
