package org.kittykat.cat65;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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

    public static int boolBit(boolean b) {
        return b ? 1 : 0;
    }

    public static int fromTwosComp(int v) {
        return (v & 0x7f) - (v & 0x80);
    }

    public static NumberAxis makeNumberAxis(double min, double max, double tickUnit) {
        NumberAxis axis = new NumberAxis();
        axis.setLowerBound(min);
        axis.setUpperBound(max);
        axis.setTickUnit(tickUnit);
        return axis;
    }

    public static void setupIntSpinner(Spinner<Integer> spinner, int min, int max, int initial, int step) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial, step);
        spinner.setValueFactory(factory);
        StringConverter<Integer> converter = factory.getConverter();
        factory.setConverter(new StringConverter<>() {
            @Override public String toString(Integer v) {
                return converter.toString(v);
            }
            @Override public Integer fromString(String s) {
                try {
                    return converter.fromString(s);
                } catch (NumberFormatException e) {
                    spinner.cancelEdit();
                    return factory.getValue();
                }
            }
        });
        spinner.focusedProperty().addListener((obs, was, is) -> {
            if (!is) spinner.commitValue();
        });
    }
    public static ChangeListener<Number> bindSpinner(Spinner<Integer> spinner, IntegerProperty property) {
        ChangeListener<Number> listener = (obs, oldValue, newValue)
                -> spinner.getValueFactory().setValue(newValue.intValue());
        property.addListener(listener);
        spinner.getValueFactory().valueProperty().addListener(
                (obs, oldValue, newValue) -> property.set(newValue)
        );
        return listener;
    }

    public static <E extends Enum<E>> void setupComboBox(ComboBox<E> comboBox, ObjectProperty<E> property,
                                                         Class<E> enumClass, StringConverter<E> converter) {
        comboBox.getItems().addAll(enumClass.getEnumConstants());
        comboBox.valueProperty().bindBidirectional(property);
        comboBox.setConverter(converter);
    }

    public static String formatFrequency(double frequency) {
        String unit = "Hz";
        if (frequency >= 1_000_000d) {
            frequency /= 1_000_000d;
            unit = "MHz";
        } else if (frequency >= 1_000d) {
            frequency /= 1_000d;
            unit = "kHz";
        }
        return "%.2f %s".formatted(frequency, unit);
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
