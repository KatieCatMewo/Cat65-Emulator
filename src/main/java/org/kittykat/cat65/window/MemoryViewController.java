package org.kittykat.cat65.window;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.MainManager;

import java.util.Locale;
import java.util.logging.Logger;

public class MemoryViewController extends WindowController {
    private static final String WINDOW_NAME = "Cat65 Memory Viewer";
    private static final double MAX_FONT_SIZE      = 12d;
    private static final double FONT_SIZE_CONSTANT = MAX_FONT_SIZE / 550d;

    @FXML
    @SuppressWarnings("unused")
    private ListView<String> memView;
    private final ObservableList<String> lines = FXCollections.observableArrayList();

    /*
     * this basically just filters out an annoying warning message
     * the thing it's warning about fixes itself, but it still fills the error log
     * (claude wrote this code, but it works, so I'm keeping it)
     */
    private static final Logger CONTROLS_LOGGER = Logger.getLogger("javafx.scene.control");
    static {
        CONTROLS_LOGGER.setFilter(record -> (record.getMessage() == null) || !record.getMessage().contains("index exceeds maxCellCount"));
    }

    public MemoryViewController() {
        super("%s [CPU]".formatted(WINDOW_NAME), "memory-viewer");

        memView.setItems(lines);
        for (int l = 0x000; l <= 0xfff; l++) {
            lines.add(getVisualizerLine(l));
        }
        memView.widthProperty().addListener((obs, oldWidth, newWidth) -> updateFontSize(newWidth.doubleValue()));
        updateFontSize(memView.getWidth());
    }

    private void updateFontSize(double width) {
        double newSize = Math.min(MAX_FONT_SIZE, width * FONT_SIZE_CONSTANT);
        memView.setStyle(String.format(Locale.ROOT, "-fx-font-size: %.3f;", newSize));
    }

    public String getVisualizerLine(int lineNum) {
        int address = lineNum << 4;
        StringBuilder hex = new StringBuilder("$%04x:".formatted(address));
        StringBuilder ascii = new StringBuilder("  |  ");
        for (int a = 0; a < 16; a++) {
            int b = MainManager.get(address + a, false);
            hex.append("%s %02x".formatted((a == 8) ? " " : "", b));
            char chr = EmuHelper.convertToASCII(b);
            ascii.append("%s%c".formatted((a == 8) ? " " : "", chr));
        }
        return hex.toString() + ascii;
    }
    public void updateVisualizerLine(int lineNum) {
        lines.set(lineNum, getVisualizerLine(lineNum));
    }

    @Override
    public void updateWindow() {
        ListViewSkin<?> skin = (ListViewSkin<?>) memView.getSkin();
        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().getFirst();

        IndexedCell<?> firstCell = flow.getFirstVisibleCell();
        if (firstCell == null) return;

        int first = Math.max(0x000, firstCell.getIndex());
        int last  = Math.min(0xfff, flow.getLastVisibleCell().getIndex());

        for (int line = first; line <= last; line++) {
            updateVisualizerLine(line);
        }
    }
}
