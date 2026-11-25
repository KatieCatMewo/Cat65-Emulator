package org.kittykat.cat65.window;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.expansion.devices.VideoCard;

public class VideoCardWindowController extends WindowManager {
    private static final String WINDOW_NAME = "Vgc7 Video Card (%1.1fx)";
    private static final int WIDTH  = VideoCard.H_VISIBLE / 2;
    private static final int HEIGHT = VideoCard.V_VISIBLE / 2;
    private static final double SCREEN_PADDING = 8d;

    private final VideoCard videoCard;
    private int scale = 0;

    @FXML private VBox screenContainer;
    @FXML private ImageView screen;

    public VideoCardWindowController(VideoCard videoCard) {
        super(WINDOW_NAME.formatted(1f), "video-card");
        this.videoCard = videoCard;
        WritableImage image = new WritableImage(videoCard.getPixelBuffer());
        screen.setImage(image);

        ChangeListener<Number> sizeListener = (obs, oldVal, newVal) -> updateScreenLayout();
        screenContainer.widthProperty().addListener(sizeListener);
        screenContainer.heightProperty().addListener(sizeListener);
        updateScreenLayout();
    }

    @FXML
    private void openVideoConfig() {
        MainManager.mainWindowController.openWindow(new ConfigWindowController("video"));
    }
    @FXML
    private void viewPalette() {
        openWindow(new PaletteViewManager(videoCard));
    }

    private void updateScreenLayout() {
        double availableWidth  = screenContainer.getWidth()  - (2 * SCREEN_PADDING);
        double availableHeight = screenContainer.getHeight() - (2 * SCREEN_PADDING);

        int scale = Math.max(1, (int) Math.floor(Math.min(availableWidth / WIDTH, availableHeight / HEIGHT)));
        int width = WIDTH * scale;
        int height = HEIGHT * scale;
        if (scale != this.scale) {
            this.scale = scale;
            window.setTitle(WINDOW_NAME.formatted(scale / 2f));
            screen.setFitWidth(width);
            screen.setFitHeight(height);
        }
        screen.setLayoutX(SCREEN_PADDING + Math.max(0, (availableWidth - width) / 2));
        screen.setLayoutY(SCREEN_PADDING + Math.max(0, (availableHeight - height) / 2));
    }
}
