package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.cpu.CPU;

public class CPUStatusController extends WindowController {
    private final CPU cpu;

    @SuppressWarnings("unused")
    @FXML private VBox statusText;

    public CPUStatusController(CPU cpu) {
        super("CPU Status", "cpu-status");
        window.getStage().setResizable(false);
        this.cpu = cpu;
    }

    @Override
    public void updateWindow() {
        String str;
        for (Node node : statusText.getChildren()) {
            if (node instanceof Label label) {
                String regName = label.getId().substring(4);

                @SuppressWarnings("deprecation")
                int value = switch (regName) {
                    case "A"   -> cpu.getA();
                    case "X"   -> cpu.getX();
                    case "Y"   -> cpu.getY();
                    case "S"   -> cpu.getS();
                    case "P"   -> cpu.getP();
                    case "PC"  -> cpu.getPC();
                    case "MDR" -> cpu.getMDR();
                    default        -> 0x00;
                };

                if (regName.equals("PC")) {
                    str = "%3s:  %5d  |  %04x  |  %s  |  \"%s\"".formatted(regName, value, value,
                            EmuHelper.getBinary(value, true), EmuHelper.getAsciiString(value, true));
                } else {
                    str = "%3s:  %5d  |    %02x  |          %s  |   \"%s\"".formatted(regName, value, value,
                            EmuHelper.getBinary(value, false), EmuHelper.getAsciiString(value, false));
                }
                label.setText(str);
            }
        }
    }
}
