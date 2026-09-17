package org.kittykat.cat65.core.expansionDevices;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.Cat65;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.CMU;
import org.kittykat.cat65.core.expansionDevices.audio.AudioExpansion;
import org.kittykat.cat65.core.expansionDevices.audio.HighpassRC;
import org.kittykat.cat65.ui.window.audioCard.AudioChannelWindow;

public class AudioCard extends AudioExpansion {
    public static final int[] NOISE_FREQ_VALUES = {
            0b1111_1111_1011, 0b1111_1111_0111, 0b1111_1110_1111, 0b1111_1101_1111,
            0b1111_1011_1111, 0b1111_1001_1111, 0b1111_0111_1111, 0b1111_0101_1111,
            0b1111_0011_0011, 0b1111_0000_0011, 0b1110_1000_0011, 0b1110_0000_0011,
            0b1101_0000_0111, 0b1100_0000_0111, 0b1000_0000_0111, 0b0000_0001_0111
    };
    private static final int CHANNEL_COUNT = 8;

    private final AudioChannelWindow channelView = new AudioChannelWindow(this);

    private int ctrl = 0b0000_0000_0000;
    private int noiseVol  = 0x0;
    private int noiseFreq = 0x0;
    private int tri01Freq = 0x000;
    private int tri02Freq = 0x000;
    private final int[] pulseVol  = {0x0,   0x0,   0x0,   0x0,   0x0};
    private final int[] pulseFreq = {0x000, 0x000, 0x000, 0x000, 0x000};

    private int noiseCounter = 0x000;
    private int noiseLFSR    = 0x0000;
    private final int[] triCounters = {0x000,    0x000};
    private final int[] triStates   = {0b0_0000, 0b0_0000};
    private final int[] pulseCounters = {0x000, 0x000, 0x000, 0x000, 0x000};
    private final int[] pulseStates   = {0b000, 0b000, 0b000, 0b000, 0b000};

    private final HighpassRC filter = new HighpassRC(82_000f, 1e-7f); // 82kOhm, 100nF

    public AudioCard(int port) {
        super(0b0000_0000_1111, port);
        makeWindow();
    }

    @Override
    public void clock() {
        if (++noiseCounter > 0xfff) {
            noiseCounter = NOISE_FREQ_VALUES[noiseFreq];
            int bit      = ((noiseLFSR >> 13) ^ (noiseLFSR >> 14)) & 0x0001;
            noiseLFSR    = ((noiseLFSR << 1) | (bit ^ 0b1));
        }
        for (int t = 0; t < triCounters.length; t++) {
            if (++triCounters[t] > 0xfff) {
                triCounters[t] = (t == 0) ? tri01Freq : tri02Freq;
                triStates[t]   = (triStates[t] + 1) & 0b1_1111;
            }
        }
        for (int p = 0; p < pulseCounters.length; p++) {
            if (++pulseCounters[p] > 0xfff) {
                pulseCounters[p] = pulseFreq[p];
                pulseStates[p]   = (pulseStates[p] + 1) & 0b111;
            }
        }
    }

    @Override
    public float getAudioSample() {
        float sample = 0f;

        if (channelView.isChannelEnabled(0) & (noiseVol > 0)) {
            sample += (((noiseLFSR >> 15) & 0x0001) * digitalToAnalog4Bit(noiseVol));
        }
        for (int t = 0; t < triStates.length; t++) {
            if (channelView.isChannelEnabled(1 + t) & ((ctrl & (0b1000_0000_0000 >> t)) != 0)) {
                int triBits = triStates[t] & 0b0_1111;
                if (triStates[t] > 0b0_1111) triBits ^= 0b1111;
                sample += digitalToAnalog4Bit(triBits);
            }
        }
        for (int p = 0; p < pulseStates.length; p++) {
            if (channelView.isChannelEnabled(3 + p) & (pulseVol[p] > 0)) {
                int duty = (ctrl >> (8 - (2 * p))) & 0b11;
                if (pulseStates[p] <= (duty ^ 0b11)) {
                    sample += digitalToAnalog4Bit(pulseVol[p]);
                }
            }
        }
        return filter.process(sample / CHANNEL_COUNT);
    }

    @Override
    protected int get(int relAddress, boolean cpu) {
        if (relAddress < 0x06) {
            return (short) switch (relAddress) {
                case 0x0 -> 0x02 | ((ctrl >> 4) & 0x0f0);
                case 0x1 -> ctrl & 0x0ff;
                case 0x2 -> (noiseVol << 4) | noiseFreq;
                case 0x3 -> ((tri01Freq & 0xf00) >> 4) | (tri02Freq >> 8);
                case 0x4 -> tri01Freq & 0x0ff;
                case 0x5 -> tri02Freq & 0x0ff;
                default  -> CMU.getMDR();
            };
        } else {
            int p = (relAddress - 0x6) >> 1;
            if (p < 5) {
                return (short) (((relAddress & 0b0001) == 0) ?
                        ((pulseVol[p] << 4) | (pulseFreq[p] >> 8)) :
                        (pulseFreq[p] & 0x0ff));
            }
        }
        return 0x00;
    }
    @Override
    protected void set(int relAddress, int value, boolean cpu) {
        if (relAddress < 0x6) {
            switch (relAddress) {
                case 0x0 -> ctrl = (ctrl & 0x0ff) | ((value & 0xf0) << 4);
                case 0x1 -> ctrl = (ctrl & 0xf00) | value;
                case 0x2 -> {
                    noiseVol = (value & 0xf0) >> 4;
                    noiseFreq = value & 0x0f;
                }
                case 0x3 -> {
                    tri01Freq = (tri01Freq & 0x0ff) | ((value & 0xf0) << 4);
                    tri02Freq = (tri02Freq & 0x0ff) | ((value & 0x0f) << 8);
                }
                case 0x4 -> tri01Freq = (tri01Freq & 0xf00) | value;
                case 0x5 -> tri02Freq = (tri02Freq & 0xf00) | value;
            }
        } else {
            int p = (relAddress - 0x6) >> 1;
            if (p < 5) {
                if ((relAddress & 0b0001) == 0) {
                    pulseVol[p] = (value & 0xf0) >> 4;
                    pulseFreq[p] = (pulseFreq[p] & 0x0ff) | ((value & 0x0f) << 8);
                } else {
                    pulseFreq[p] = (pulseFreq[p] & 0xf00) | value;
                }
            }
        }
    }

    private int getTriVolume(int t) {
        return ((ctrl & (0b1000_0000_0000 >> t)) != 0) ? 0xf : 0x0;
    }
    public int[] getVolumeLevels() {
        int[] levels = new int[8];
        levels[0] = noiseVol;
        levels[1] = getTriVolume(0);
        levels[2] = getTriVolume(1);
        System.arraycopy(pulseVol, 0, levels, 3, pulseVol.length);
        return levels;
    }
    public int[] getFrequencies() {
        int[] freq = new int[8];
        freq[0] = noiseFreq;
        freq[1] = tri01Freq;
        freq[2] = tri02Freq;
        System.arraycopy(pulseFreq, 0, freq, 3, pulseFreq.length);
        return freq;
    }
    public int[] getCounters() {
        int[] counters = new int[8];
        counters[0] = noiseCounter;
        System.arraycopy(triCounters, 0, counters, 1, triCounters.length);
        System.arraycopy(pulseCounters, 0, counters, 3, pulseCounters.length);
        return counters;
    }
    public int[] getStates() {
        int[] states = new int[8];
        states[0] = (noiseLFSR & 0xffff);
        System.arraycopy(triStates, 0, states, 1, triStates.length);
        System.arraycopy(pulseStates, 0, states, 3, pulseStates.length);
        return states;
    }
    public int[] getDutyCycles() {
        int[] duty = new int[]{-1, -1, -1, 0, 0, 0, 0, 0};
        for (int p = 0; p < pulseStates.length; p++) {
            duty[3 + p] = (ctrl >> (8 - (2 * p))) & 0b11;
        }
        return duty;
    }

    @Override
    public void updateWindow() {
        channelView.updateWindow();
    }

    @Override
    protected void makeWindow() {
        super.makeWindow();

        VBox root = new VBox(Cat65.SPACING, channelView);
        root.setId("root");

        Scene scene = new Scene(root);
        EmuHelper.applyCSS(scene);

        setWindowTitle("2A65 Audio Card");
        window.setScene(scene);
        window.setResizable(false);
        window.setAlwaysOnTop(true);
    }
}
