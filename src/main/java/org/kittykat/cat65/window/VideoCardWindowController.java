package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.expansion.devices.VideoCard;

public class VideoCardWindowController extends WindowController {
    @FXML private ImageView screen;

    public VideoCardWindowController(VideoCard videoCard) {
        super("Vgc7 Video Card", "video-card");
        WritableImage image = new WritableImage(videoCard.getPixelBuffer());
        screen.setImage(image);
    }

    @FXML
    private void openVideoConfig() {
        MainManager.mainWindowController.openWindow(new ConfigWindowController("video"));
    }
}
