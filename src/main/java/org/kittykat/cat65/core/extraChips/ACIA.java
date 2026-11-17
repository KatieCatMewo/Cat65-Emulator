package org.kittykat.cat65.core.extraChips;

import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.ConfigManager;

/**
 * I hated this -w-<br>
 * why are there two datasheets with different amounts of information???<br>
 * the datasheet for the w65c51<b><i><u>s</u></i></b> documents pretty much all information except for some little bugs
 * because those bugs are only on the w65c51<b><i><u>n</u></i></b>...<br>
 * but the datasheet for the <i>N</i> version does not document <b>nearly</b> as much information for a lot of things!
 * >:c
**/
public class ACIA extends ExtraChip {
    private static final int[] WORLD_LENGTHS = {8, 7, 6, 5};
    private static final int[] WORLD_LENGTH_MASKS = {0xff, 0x7f, 0x3f, 0x1f};

    private static final float  CYCLES_PER_BAUD_UPDATE = .5f;
    private static final double BAUD_GEN_MHZ = 1.8432d;
    private static final double BAUD_GEN_TIME_VAL = BAUD_GEN_MHZ / ConfigManager.DEFAULT_UPDATE_MHZ;
    private static final int[]  BAUD_GEN_DIVISORS = {
               16, 36_864, 24_576, 16_769, 13_704, 12_288, 6_144, 3_072,
            1_536,  1_024,    768,    512,    384,    256,   192,    96
    };
    private static final double[] BAUD_GEN_TIME_VALUES = {
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[0],  BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[1],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[2],  BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[3],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[4],  BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[5],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[6],  BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[7],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[8],  BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[9],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[10], BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[11],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[12], BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[13],
            BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[14], BAUD_GEN_TIME_VAL / BAUD_GEN_DIVISORS[15],
    };
    private double baudTimeAccumulator = 0d;

    private int transmitBuffer = 0x00;
    private boolean transmitting = false;
    private float transmitCycleCounter = 0f;
    private float receiveCycleCounter  = 0f;

    private int status = 0b0001_0000;
    private int cmd    = 0b0000_0000;
    private int ctrl   = 0b0000_0000;
    private int receiveData = 0x00;

    public ACIA() {
        super(0b0000_0000_0011);
    }

    private void checkReceivedChar(char c) {
        // check if DTR is 1 and the Receiver Clock Source uses the baud rate
        // the external Rx Clock is disconnected so if RCS is 0, the Rx Clock is basically 0 Hz
        if (((cmd & 0b0000_0001) != 0) && ((ctrl &0b0001_0000) != 0)) {
            updateRDR(c);

            // receiver echo mode
            if ((cmd & 0b0001_1100) == 0b0001_0000) transmitChar(c);
        }
    }
    private void updateRDR(char chr) {
        receiveData = (chr & 0xff);
        if ((status & 0b0000_1000) != 0) {
            status |= 0b0000_0100;  // overrun
        }
        status |= 0b0000_1000;  // RDR full
        checkIRQ((cmd & 0b0000_0010) == 0);  // if RDRF can assert IRQs
    }

    private boolean areInterruptsEnabled() {
        return ((cmd & 0b0000_0001) != 0);
    }
    private void checkIRQ(boolean condition) {
        if (areInterruptsEnabled() && condition) {
            status |= 0b1000_0000;
        }
    }

    @Override
    public void update() {
        baudTimeAccumulator += BAUD_GEN_TIME_VALUES[ctrl & 0b0000_1111];
        while (baudTimeAccumulator > CYCLES_PER_BAUD_UPDATE) {
            // receiver update
            if (receiveCycleCounter > 0f) receiveCycleCounter -= CYCLES_PER_BAUD_UPDATE;
            if (receiveCycleCounter <= 0f) {
                if (!ConfigManager.isSerialBufferEmpty()) {
                    checkReceivedChar(ConfigManager.pollSerial());
                    receiveCycleCounter = getTransmitLength();
                }
            }
            // transmitter update
            if (transmitting) {
                if (transmitCycleCounter > 0f) transmitCycleCounter -= CYCLES_PER_BAUD_UPDATE;
                if (transmitCycleCounter <= 0f) {
                    transmitting = false;
                    ConfigManager.outputSerial((char) transmitBuffer);
                }
            }
            baudTimeAccumulator -= CYCLES_PER_BAUD_UPDATE;
        }
    }
    @Override
    public void reset() {
        status &= 0b0110_0000;
        status |= 0b0001_0000;
        ctrl = 0b0000_0000;
        cmd  = 0b0000_0000;
    }
    @Override
    public boolean getIRQ() {
        return (status & 0b1000_0000) == 0;
    }

    @Override
    protected int get(int relAddress, boolean internal) {
        int value = switch (relAddress) {
            case 0b00 -> receiveData;
            case 0b01 -> status | 0b0001_0000;  // stuck TDRE bit
            case 0b10 -> cmd;
            case 0b11 -> ctrl;
            default   -> 0x00;
        };
        if (internal) {
            switch (relAddress) {
                case 0b00 -> status &= 0b1111_0000;  // clear certain status register bits on RDR read
                case 0b01 -> status &= 0b0111_1111;  // clear interrupt flag on status read
            }
        }
        return value;
    }
    @Override
    protected void set(int relAddress, int value, boolean internal) {
        switch (relAddress) {
            case 0b00 -> {
                // only enable transmitter if DTR is 1
                if ((cmd & 0b0000_0001) != 0) transmitChar(value);
            }
            case 0b01 -> {
                // soft reset
                status &= 0b1111_1011;
                cmd &= 0b1110_0000;
            }
            case 0b10 -> cmd = value;
            case 0b11 -> ctrl = value;
        }
    }

    private void transmitChar(int c) {
        transmitBuffer = c & WORLD_LENGTH_MASKS[(ctrl & 0b0110_0000) >> 5];  // apply the word length
        transmitCycleCounter = getTransmitLength();
        transmitting = true;
        // would reset TDRE but the 65c51n has that bit stuck to 1
    }
    private float getTransmitLength() {
        int wordLen    = (ctrl & 0b0110_0000) >> 5;  // word length bits
        boolean parity = (cmd & 0b0010_0000) != 0;   // parity enabled?
        // start bit + data bits + parity bit + stop bit(s)
        return 1f + WORLD_LENGTHS[wordLen] + EmuHelper.boolBit(parity) + getStopBitCount(wordLen, parity);
    }
    private float getStopBitCount(int wordLen, boolean parity) {
        if ((ctrl & 0b1000_0000) == 0) {
            // SBN is 0
            return 1f;  // only 1 stop bit
        } else {
            // SBN is 1, apply special rules
            // (only correctly documented in the 65c51s not n)
            if ((wordLen == 0b11) && !parity) {
                // WL = 5 and no parity
                return 1.5f;  // 1.5 stop bits
            } else if (wordLen == 0b00 && parity) {
                // WL = 8 and parity
                return 1f;  // only 1 stop bit to limit frame length
            }
            return 2f;  // 2 stop bits otherwise
        }
    }
}
