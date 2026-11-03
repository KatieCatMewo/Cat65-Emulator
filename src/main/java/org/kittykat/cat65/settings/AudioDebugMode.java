package org.kittykat.cat65.settings;

import javafx.util.StringConverter;

public enum AudioDebugMode {
    Oscilloscope,
    Buffer_Level;

    public static final StringConverter<AudioDebugMode> stringConverter = new StringConverter<>() {
        @Override
        public String toString(AudioDebugMode object) {
            return object.toString().replaceAll("_", " ");
        }
        @Override
        public AudioDebugMode fromString(String string) {
            return AudioDebugMode.valueOf(string.strip().replaceAll(" ", "_"));
        }
    };
}
