package org.kittykat.cat65.core.expansionDevices;

public class DisconnectedPort extends ExpansionDevice {
    public DisconnectedPort(int port) {
        super(0b0000_0000_0000, port);
    }

    @Override
    protected int get(int relAddress, boolean cpu) {
        return 0x00;
    }
    @Override
    protected void set(int relAddress, int value, boolean cpu) {}
}
