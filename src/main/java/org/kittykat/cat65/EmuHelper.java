package org.kittykat.cat65;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class EmuHelper {
    public static final URL CSS = loadResource("style.css");

    public static final double LOG2 = Math.log(2d);

    private static final String[] NOTES = {"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "};

    public static URL loadResource(String path) {
        return EmuHelper.class.getResource("%s%s".formatted(Cat65.RESOURCE_PATH, path));
    }
    public static byte[] readResourceFile(String path) throws IOException {
        try (InputStream in = EmuHelper.class.getResourceAsStream(Cat65.RESOURCE_PATH + path)) {
            if (in == null) {
                throw new FileNotFoundException("\"%s%s\" does not exist!".formatted(Cat65.RESOURCE_PATH, path));
            }
            return in.readAllBytes();
        }
    }

    public static String getBinary(int value, int bits) {
        return "%%%ds".formatted(bits).formatted(Integer.toBinaryString(value)).replaceAll(" ", "0");
    }
    public static String getBinary(int value, boolean is16bit) {
        value &= 0xffff;
        return getBinary(value, 16).substring(is16bit ? 0 : 8, 16);
    }
    public static String getAsciiString(int value, boolean is16bit) {
        if (is16bit) {
            char cl = convertToASCII( value       & 0xff);
            char ch = convertToASCII((value >> 8) & 0xff);
            return String.valueOf(ch) + cl;
        } else {
            return String.valueOf(convertToASCII(value));
        }
    }

    public static char convertToASCII(int value) {
        char c = (char) value;
        if ((c < 0x20) || (c >= 0x7f)) {
            return (char) 0xb7;
        }
        return c;
    }

    public static int fromBCD(int v) {
        return (v & 0x0f) + (((v & 0xf0) >> 4) * 10);
    }
    public static int toBCD(int v) {
        return (v % 10) + ((v / 10) << 4);
    }

    public static int boolBit(boolean b) {
        return b ? 1 : 0;
    }

    public static int fromTwosComp(int v) {
        return (v & 0x7f) - (v & 0x80);
    }

    public static Button makeButton(String text, EventHandler<ActionEvent> eventHandler) {
        Button btn = new Button(text);
        btn.setOnAction(eventHandler);
        return btn;
    }
    public static <E extends Enum<E>> ComboBox<E> makeSetting(ObjectProperty<E> property, Class<E> enumClass, StringConverter<E> converter) {
        ComboBox<E> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(enumClass.getEnumConstants());
        comboBox.valueProperty().bindBidirectional(property);
        comboBox.setConverter(converter);
        return comboBox;
    }

    public static GridPane makeGridBase() {
        GridPane grid = new GridPane(Cat65.SPACING, Cat65.SPACING);
        grid.getStyleClass().add("grid-pane");
        grid.setGridLinesVisible(false);
        return grid;
    }
    public static RowConstraints makeRowConstraint() {
        RowConstraints row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        row.setFillHeight(true);
        return row;
    }
    public static ColumnConstraints makeColumnConstraint(HPos horizontalAlignment) {
        ColumnConstraints col = new ColumnConstraints();
        col.setHgrow(Priority.ALWAYS);
        col.setFillWidth(true);
        col.setHalignment(horizontalAlignment);
        return col;
    }
    public static void setupColumns(GridPane grid, int columns, HPos horizontalAlignment) {
        ObservableList<ColumnConstraints> columnConstraints = grid.getColumnConstraints();
        float columnPercent = 100f / columns;
        for (int c = 0; c < columns; c++) {
            ColumnConstraints col = makeColumnConstraint(horizontalAlignment);
            col.setPercentWidth(columnPercent);
            columnConstraints.add(col);
        }
    }
    public static GridPane makeGrid(int rows, int columns, HPos horizontalAlignment) {
        GridPane grid = makeGridBase();

        ObservableList<RowConstraints> rowConstraints = grid.getRowConstraints();
        float rowPercent = 100f / rows;
        for (int r = 0; r < rows; r++) {
            RowConstraints row = makeRowConstraint();
            row.setPercentHeight(rowPercent);
            rowConstraints.add(row);
        }
        setupColumns(grid, columns, horizontalAlignment);
        return grid;
    }
    public static GridPane makeFreeGrid(int rows, int columns, HPos horizontalAlignment) {
        GridPane grid = makeGridBase();

        for (int r = 0; r < rows; r++) {
            grid.getRowConstraints().add(makeRowConstraint());
        }
        setupColumns(grid, columns, horizontalAlignment);
        return grid;
    }

    public static NumberAxis makeNumberAxis(double min, double max, double tickUnit) {
        NumberAxis axis = new NumberAxis();
        axis.setAutoRanging(false);
        axis.setLowerBound(min);
        axis.setUpperBound(max);
        axis.setTickUnit(tickUnit);
        return axis;
    }

    public static void applyCSS(Scene scene) {
        if (CSS != null) {
            scene.getStylesheets().add(CSS.toExternalForm());
        } else {
            System.err.println("[!] Could not load the stylesheet...");
        }
    }

    public static String getNote(double freq) {
        if (freq < 20d) {
            return "-  -  --c";
        }

        double midi = 69 + (12 * (Math.log(freq / 440d) / LOG2));

        int note  = (int) Math.round(midi);
        int cents = (int) ((midi - note) * 100);
        boolean negativeCents = cents < 0;

        int noteIndex = Math.floorMod(note, 12);
        int octave    = note / 12 - 1;

        return "%s%2d %s%02dc".formatted(NOTES[noteIndex], octave, negativeCents? "-" : "+", Math.abs(cents));
    }
}
