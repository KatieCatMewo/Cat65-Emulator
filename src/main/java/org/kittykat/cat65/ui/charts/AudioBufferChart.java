package org.kittykat.cat65.ui.charts;

import javafx.scene.chart.XYChart;
import org.kittykat.cat65.core.DebugData;

public class AudioBufferChart extends DebugChart {
    public AudioBufferChart(XYChart.Series<Number, Number> series) {
        super(DebugData.VISUAL_BUFFER_SIZE, DebugData.VISUAL_BUFFER_SIZE / 8, -1d, 1d, .25d);
        getData().add(series);
        setDebugLine(DebugData.VISUAL_BUFFER_SIZE / 2);
        getStyleClass().add("debug-audio-chart");
    }
}
