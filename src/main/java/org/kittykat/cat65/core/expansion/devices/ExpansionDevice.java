package org.kittykat.cat65.core.expansion.devices;

import javafx.stage.Stage;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.extraChips.ExtraChip;
import org.kittykat.cat65.settings.ExpansionPort;
import org.kittykat.cat65.window.WindowController;

public abstract class ExpansionDevice extends ExtraChip {
    protected WindowController windowController = null;

    protected Stage window = null;

    public ExpansionDevice(int mirrorAddressMask) {
        super(mirrorAddressMask);
    }

    public boolean getNMI() {
        return true;
    }

    public static ExpansionDevice fromEnum(ExpansionPort device) {
        return switch (device) {
            case _2A05 -> new AudioCard();
            case _Vgc7 -> new VideoCard();
            case _Disconnected -> new DisconnectedPort();
        };
    }

    public void updateWindow() {
        if (windowController != null) {
            windowController.updateWindow();
        }
    }

    public void openWindow() {
        if (windowController == null) {
            windowController = makeWindow();
            if (windowController == null) return;
            MainManager.mainWindowController.openWindow(windowController);
        } else {
            Stage s = windowController.getWindow().getStage();
            s.setIconified(false);
            s.requestFocus();
        }
    }
    protected WindowController makeWindow() {
        return null;
    }

    /**
     * ! important !<br>
     * this method should only respond if the window is its own window
     * @param window the window that is supposed to be closed
     */
    public void closeWindow(WindowController window) {
        if (window == windowController) {
            windowController = null;
        }
    }

    public void forceCloseWindow() {
        if (windowController != null) {
            windowController.getWindow().close();
        }
    }
}
