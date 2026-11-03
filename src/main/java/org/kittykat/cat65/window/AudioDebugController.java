package org.kittykat.cat65.window;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.EmuHelper;
import org.kittykat.cat65.core.AudioManager;
import org.kittykat.cat65.core.DebugData;
import org.kittykat.cat65.settings.AudioDebugMode;
import org.kittykat.cat65.ui.charts.AudioBufferChart;
import org.kittykat.cat65.ui.charts.DebugLineChart;

public class AudioDebugController extends WindowController {
    @FXML private VBox container;
    @FXML private ComboBox<AudioDebugMode> cbx_debugMode;
    @FXML private Slider slider_scale;

    private final ObjectProperty<AudioDebugMode> debugModeProperty = new SimpleObjectProperty<>(AudioDebugMode.Oscilloscope);
    private final AudioBufferChart audioOutputChart = new AudioBufferChart(DebugData.audioSeries);
    private final DebugLineChart audioBufferChart = new DebugLineChart(AudioManager.SAMPLER_BUFFER_SIZE, 256, 2);

    public AudioDebugController() {
        super("Audio Debug", "audio-debug");

        VBox.setVgrow(audioOutputChart, Priority.ALWAYS);
        container.getChildren().add(audioOutputChart);

        EmuHelper.setupComboBox(cbx_debugMode, debugModeProperty, AudioDebugMode.class, AudioDebugMode.stringConverter);
        debugModeProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != oldVal) {
                ObservableList<Node> children = container.getChildren();
                children.clear();
                switch (newVal) {
                    case Oscilloscope -> {
                        slider_scale.setVisible(true);
                        children.add(audioOutputChart);
                    }
                    case Buffer_Level -> {
                        slider_scale.setVisible(false);
                        children.add(audioBufferChart);
                    }
                }
            }
        });
        slider_scale.valueProperty().bindBidirectional(DebugData.audioScaleProperty);
    }

    @Override
    public void updateWindow() {
        audioBufferChart.add(AudioManager.READ_BUFFER_SIZE, DebugData.getAudioBufferFillLevel());
    }
}
