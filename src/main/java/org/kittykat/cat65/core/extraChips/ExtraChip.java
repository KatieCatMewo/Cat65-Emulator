package org.kittykat.cat65.core.extraChips;

public abstract class ExtraChip {
    private final int bitMask;

    public ExtraChip(int mirrorAddressMask) {
        bitMask = mirrorAddressMask;
    }

    public void reset() {}
    public void clock() {}

    public boolean getIRQ() {
        return true;
    }

    public int read(int address, boolean internal) {
        return (get(getRelativeAddress(address), internal) & 0xff);
    }
    protected abstract int get(int relAddress, boolean internal);

    public void write(int address, int value, boolean internal) {
        set(getRelativeAddress(address), value & 0xff, internal);
    }
    protected abstract void set(int relAddress, int value, boolean internal);

    protected int getRelativeAddress(int address) {
        return (address & bitMask);
    }
}
