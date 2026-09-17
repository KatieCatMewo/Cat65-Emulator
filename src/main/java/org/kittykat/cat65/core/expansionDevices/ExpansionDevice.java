package org.kittykat.cat65.core.expansionDevices;

import javafx.stage.Stage;
import org.kittykat.cat65.core.extraChips.ExtraChip;
import org.kittykat.cat65.settings.ExpansionPort;

public abstract class ExpansionDevice extends ExtraChip {
    protected final int portNum;

    protected Stage window = null;

    public ExpansionDevice(int mirrorAddressMask, int portNum) {
        super(mirrorAddressMask);
        this.portNum = portNum;
    }

    public boolean getNMI() {
        return true;
    }

    public static ExpansionDevice fromEnum(ExpansionPort device, int port) {
        return switch (device) {
            case _2A05 -> new AudioCard(port);
            case _Vgc7 -> new VideoCard(port);
            case _Disconnected -> new DisconnectedPort(port);
        };
    }

    public void updateWindow() {}

    protected void makeWindow() {
        window = new Stage();
    }
    protected void setWindowTitle(String title) {
        window.setTitle("%s [$%x000] :3c".formatted(title, portNum));
    }
    public void showWindow() {
        if (window != null) {
            window.show();
        }
    }
    public void hideWindow() {
        if (window != null) {
            window.close();
        }
    }
    public boolean doesNotHaveWindow() {
        return window == null;
    }
}
