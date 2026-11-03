package org.kittykat.cat65.ui.charts;

import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.EmuHelper;

public abstract class DebugChart extends LineChart<Number, Number> {
    private final XYChart.Series<Number, Number> debugLineSeries = new XYChart.Series<>();
    private final double minY;
    private final double maxY;

    public DebugChart(int maxX, int xTickUnit, double minY, double maxY, double yTickUnit) {
        super(EmuHelper.makeNumberAxis(0, maxX, xTickUnit),
              EmuHelper.makeNumberAxis(minY,   maxY, yTickUnit));

        this.minY = minY;
        this.maxY = maxY;
        getData().add(debugLineSeries);

        setAnimated(false);
        setCreateSymbols(false);
        setLegendVisible(false);
        VBox.setVgrow(this, Priority.ALWAYS);

        getStyleClass().add("debug-chart");
    }

    protected void setDebugLine(int x) {
        ObservableList<XYChart.Data<Number, Number>> debugLine = debugLineSeries.getData();
        debugLine.clear();
        addPoint(debugLine, x, minY);
        addPoint(debugLine, x, maxY);
    }

    protected static void addPoint(ObservableList<XYChart.Data<Number, Number>> series, int x, double y) {
        series.add(new XYChart.Data<>(x, y));
    }
}
