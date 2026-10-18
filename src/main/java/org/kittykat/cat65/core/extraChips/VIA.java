package org.kittykat.cat65.core.extraChips;

import org.kittykat.cat65.core.CMU;

public class VIA extends ExtraChip {
    private int ora = 0x00;
    private int ddra = 0x00;
    private int orb = 0x00;
    private int ddrb = 0x00;

    private boolean t1Running = false;
    private boolean t1SetIRQ  = false;
    private int t1Latch   = 0x0000;
    private int t1Counter = 0x0000;
    private boolean t2Running = false;
    private boolean t2SetIRQ  = false;
    private int t2Latch   = 0x0000;
    private int t2Counter = 0x0000;

    private int shiftReg = 0x00;

    private int auxiliaryCTRL  = 0b0000_0000;
    private int peripheralCTRL = 0b0000_0000;  // does nothing (handshake lines unconnected)
    private int irqFlags  = 0b0000_0000;
    private int irqEnable = 0b0000_0000;

    public VIA() {
        super(0b0000_0000_1111);
    }

    @Override
    public void reset() {
        // Counters, Latches and the Shift Register are not reset
        ora =  0x00;
        ddra = 0x00;
        orb =  0x00;
        ddrb = 0x00;
        auxiliaryCTRL  = 0b0000_0000;
        peripheralCTRL = 0b0000_0000;
        irqFlags  = 0b0000_0000;
        irqEnable = 0b0000_0000;
    }
    @Override
    public void clock() {
        if (t1Running) {
            t1Counter = (t1Counter - 1) & 0xffff;
            if (t1Counter == 0xffff) {
                // ACR7 is ignored since that just determines if PB7 (unconnected) would be used
                t1Counter = t1Latch + 1;
                if (t1SetIRQ) {
                    irqFlags |= 0b0100_0000;
                }
                if ((auxiliaryCTRL & 0b0100_0000) == 0) {
                    t1SetIRQ = false;
                }
            }
        }
        if (t2Running) {
            // PB6 is never asserted by the LCD so the Pulse Counting Mode does nothing at all
            if ((auxiliaryCTRL & 0b0010_0000) == 0) {
                t2Counter = (t2Counter - 1) & 0xffff;
                if (t2Counter == 0xffff) {
                    if (t2SetIRQ) {
                        irqFlags |= 0b0010_0000;
                    }
                    t2SetIRQ = false;
                }
            }
        }

        // ToDo: clock the Shift Register
    }
    @Override
    public boolean getIRQ() {
        // IRQs are active-low
        return (irqFlags & irqEnable) == 0;
    }

    @Override
    protected int get(int relAddress, boolean cpu) {
        return switch (relAddress) {
            case 0x0 -> {
                int IRB = 0x00;
                // latched inputs read as 0x00
                if ((auxiliaryCTRL & 0b0000_0010) == 0) {
                    IRB = CMU.getPB(orb & ddrb);
                }
                yield (orb & ddrb) | (IRB & ~ddrb);
            }
            case 0x1,
                 0xf -> {
                int IRA = 0x00;
                // latched inputs read as 0x00
                if ((auxiliaryCTRL & 0b0000_0001) == 0) {
                    IRA = CMU.getPA(ora & ddra);
                }
                yield IRA;
            }
            case 0x2 -> ddrb;
            case 0x3 -> ddra;
            case 0x4 -> {
                clearT1IRQ(cpu);
                yield t1Counter & 0x00ff;
            }
            case 0x5 -> (t1Counter & 0xff00) >> 8;
            case 0x6 -> t1Latch & 0x00ff;
            case 0x7 -> (t1Latch & 0xff00) >> 8;
            case 0x8 -> {
                clearT2IRQ(cpu);
                yield t2Counter & 0x00ff;
            }
            case 0x9 -> (t2Counter & 0xff00) >> 8;
            case 0xa -> shiftReg;
            case 0xb -> auxiliaryCTRL;
            case 0xc -> peripheralCTRL;
            case 0xd -> irqFlags  | ((irqFlags & irqEnable) != 0 ? 0x80 : 0x00);
            case 0xe -> irqEnable | 0x80;
            default  -> 0x00;
        };
    }
    @Override
    protected void set(int relAddress, int value, boolean cpu) {
        switch (relAddress) {
            case 0x0 -> {
                orb = value;
                updatePBO();
            }
            case 0x1,
                 0xf -> {
                ora = value;
                updatePAO();
            }
            case 0x2 -> {
                ddrb = value;
                updatePBO();
            }
            case 0x3 -> {
                ddra = value;
                updatePAO();
            }
            case 0x4,
                 0x6 -> t1Latch = (t1Latch & 0xff00) | value;
            case 0x5 -> {
                t1Latch = (t1Latch & 0x00ff) | (value << 8);
                t1Counter = t1Latch;
                clearT1IRQ(cpu);
                t1Running = true;
                t1SetIRQ  = true;
            }
            case 0x7 -> {
                t1Latch = (t1Latch & 0x00ff) | (value << 8);
                clearT1IRQ(cpu);
            }
            case 0x8 -> t2Latch = (t2Latch & 0xff00) | value;
            case 0x9 -> {
                t2Counter = t2Latch | (value << 8);
                clearT2IRQ(cpu);
                t2Running = true;
                t2SetIRQ  = true;
            }
            case 0xa -> shiftReg       = value;
            case 0xb -> auxiliaryCTRL  = value;
            case 0xc -> peripheralCTRL = value;
            case 0xd -> irqFlags      &= ~(value & 0x7f);
            case 0xe -> {
                if ((value & 0x80) != 0) {
                    // bit7 == 1
                    irqEnable |= value & 0x7f;
                } else {
                    // bit7 == 0
                    irqEnable &= ~(value & 0x7f);
                }
            }
        }
    }

    private void clearT1IRQ(boolean cpu) {
        if (cpu) {
            irqFlags &= 0b1011_1111;
        }
    }
    private void clearT2IRQ(boolean cpu) {
        if (cpu) {
            irqFlags &= 0b1101_1111;
        }
    }

    private void updatePAO() {
        CMU.setPA(ora & ddra);
    }
    private void updatePBO() {
        CMU.setPB(orb & ddrb);
    }
}
