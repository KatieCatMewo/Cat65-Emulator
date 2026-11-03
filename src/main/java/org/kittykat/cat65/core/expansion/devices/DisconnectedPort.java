package org.kittykat.cat65.core.expansion.devices;

public class DisconnectedPort extends ExpansionDevice {
    public DisconnectedPort() {
        super(0b0000_0000_0000);
    }

    @Override
    protected int get(int relAddress, boolean internal) {
        return 0x00;
    }
    @Override
    protected void set(int relAddress, int value, boolean internal) {}
}
