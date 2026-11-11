package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.kittykat.cat65.core.expansion.devices.VideoCard;

public class PaletteViewManager extends WindowController {
    private final VideoCard videoCard;

    @FXML private Label lbl_bank;
    @FXML private GridPane grid_preview;

    public PaletteViewManager(VideoCard videoCard) {
        super("Vgc7 Palette", "palette-viewer");
        this.videoCard = videoCard;
        getWindow().getStage().setResizable(false);
    }

    @Override
    public void updateWindow() {
        lbl_bank.setText("Palette Bank: %d".formatted(videoCard.getColormapBank()));

        for (Node n : grid_preview.getChildren()) {
            Integer i = GridPane.getColumnIndex(n);
            if (i == null) {
                i = 0;
            }
            int p = videoCard.getPaletteValue(i);
            if (n instanceof Label l) {
                l.setText("$%02x".formatted(p));
            } else if (n instanceof Region r) {
                r.setStyle("-fx-background-color: #%06x".formatted(videoCard.getOutputColor(p) & 0x00_ffffff));
            }
        }
    }
}
