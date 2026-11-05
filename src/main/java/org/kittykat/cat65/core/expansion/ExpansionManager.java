package org.kittykat.cat65.core.expansion;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.kittykat.cat65.core.expansion.devices.DisconnectedPort;
import org.kittykat.cat65.core.expansion.devices.ExpansionDevice;
import org.kittykat.cat65.core.expansion.devices.audio.AudioExpansion;
import org.kittykat.cat65.settings.ExpansionPort;
import org.kittykat.cat65.window.WindowController;

public abstract class ExpansionManager {
    public static final ObjectProperty<ExpansionPort> ex04Property = new SimpleObjectProperty<>(ExpansionPort._Disconnected);
    public static final ObjectProperty<ExpansionPort> ex07Property = new SimpleObjectProperty<>(ExpansionPort._Disconnected);

    public static volatile ExpansionDevice ex04 = new DisconnectedPort();
    public static volatile ExpansionDevice ex07 = new DisconnectedPort();

    static {
        ex04Property.addListener((obs, oldVal, newVal) -> setEx04(newVal));
        ex07Property.addListener((obs, oldVal, newVal) -> setEx07(newVal));
    }

    private static void setEx04(ExpansionPort device) {
        ex04.forceCloseWindow();
        ex04 = getDevice(device);
    }
    private static void setEx07(ExpansionPort device) {
        ex07.forceCloseWindow();
        ex07 = getDevice(device);
    }
    private static ExpansionDevice getDevice(ExpansionPort device) {
        return ExpansionDevice.fromEnum(device);
    }

    public static void openWindow(int i) {
        ExpansionDevice device = (i == 0) ? ex04 : ex07;
        device.openWindow();
    }
    public static void closeWindow(WindowController window) {
        ex04.closeWindow(window);
        ex07.closeWindow(window);
    }
    public static void updateWindows() {
        ex04.updateWindow();
        ex07.updateWindow();
    }

    public static void clock() {
        ex04.clock();
        ex07.clock();
    }
    public static void reset() {
        ex04.reset();
        ex07.reset();
    }

    public static boolean getIRQ() {
        return ex04.getIRQ() && ex07.getIRQ();
    }
    public static boolean getNMI() {
        return ex04.getNMI() && ex07.getNMI();
    }

    public static int get(int address, boolean internal) {
        int select = address >> 12;
        return switch (select) {
            case 0x4 -> ex04.read(address - 0x4000, internal);
            case 0x7 -> ex07.read(address - 0x7000, internal);
            default -> 0x00;
        };
    }
    public static void set(int address, int value, boolean internal) {
        int select = address >> 12;
        switch (select) {
            case 0x4 -> ex04.write(address - 0x4000, value, internal);
            case 0x7 -> ex07.write(address - 0x7000, value, internal);
        }
    }

    public static float getAudioSample() {
        int count = 0;
        float sample = 0f;

        if (ex04 instanceof AudioExpansion audio04) {
            count++;
            sample += audio04.getAudioSample();
        }
        if (ex07 instanceof AudioExpansion audio07) {
            count++;
            sample += audio07.getAudioSample();
        }

        if (count == 0) return 0f;
        return sample / count;
    }
}
