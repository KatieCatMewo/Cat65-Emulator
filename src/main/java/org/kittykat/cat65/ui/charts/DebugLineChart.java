package org.kittykat.cat65.ui.charts;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

public class DebugLineChart extends DebugChart {
    private static final int POINT_COUNT = 240;
    private int x = 0;

    public DebugLineChart(double maxY, double yTickUnit, int lineCount) {
        super(POINT_COUNT, 30, 0, maxY, yTickUnit);

        if (lineCount < 1) {
            throw new IllegalArgumentException("Chart can not have less than 1 Line!");
        }
        for (int l = 0; l < lineCount; l++) {
            getData().add(new XYChart.Series<>());
        }
        getStyleClass().add("debug-line-chart");

        disableAutoRanging();
    }

    public void add(double... points) {
        ObservableList<XYChart.Series<Number, Number>> data = getData();
        int dataSize = data.size() - 1;

        if (dataSize != points.length) {
            throw new RuntimeException("Expected %d Data Point(s), got %d instead!".formatted(dataSize, points.length));
        }

        int nextX = (x + 1) % POINT_COUNT;
        for (int s = 0; s < dataSize; s++) {
            ObservableList<XYChart.Data<Number, Number>> series = data.get(s + 1).getData();
            if (series.size() >= POINT_COUNT) series.removeFirst();
            addPoint(series, x, points[s]);
        }
        setDebugLine(nextX);
        x = nextX;
    }
}
