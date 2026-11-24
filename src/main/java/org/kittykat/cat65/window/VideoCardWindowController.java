package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.expansion.devices.VideoCard;

import static org.kittykat.cat65.core.expansion.devices.VideoCard.H_VISIBLE;
import static org.kittykat.cat65.core.expansion.devices.VideoCard.V_VISIBLE;

public class VideoCardWindowController extends WindowManager {
    private static final double SCREEN_PADDING = 8d;

    private final VideoCard videoCard;

    @FXML private VBox screenContainer;
    @FXML private ImageView screen;

    public VideoCardWindowController(VideoCard videoCard) {
        super("Vgc7 Video Card", "video-card");
        this.videoCard = videoCard;
        WritableImage image = new WritableImage(videoCard.getPixelBuffer());
        screen.setImage(image);

        screenContainer.widthProperty().addListener((obs, oldVal, newVal) -> updateScreenLayout());
        screenContainer.heightProperty().addListener((obs, oldVal, newVal) -> updateScreenLayout());
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
        double availableWidth = screenContainer.getWidth() - (2 * SCREEN_PADDING);
        double availableHeight = screenContainer.getHeight() - (2 * SCREEN_PADDING);

        int scale = (int) Math.floor(Math.min(availableWidth / H_VISIBLE, availableHeight / V_VISIBLE));
        scale = Math.max(1, scale);

        int width = H_VISIBLE * scale;
        int height = V_VISIBLE * scale;
        screen.setFitWidth(width);
        screen.setFitHeight(height);
        screen.setLayoutX(SCREEN_PADDING + Math.max(0, (availableWidth - width) / 2));
        screen.setLayoutY(SCREEN_PADDING + Math.max(0, (availableHeight - height) / 2));
    }
}
