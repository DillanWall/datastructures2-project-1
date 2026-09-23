package leafdetector.processing;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageAnalyserTest {
    @Test
    void analysisFindsSeparateColourMatchedComponents() {
        WritableImage image = new WritableImage(8, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 8; x++) {
                image.getPixelWriter().setColor(x, y, Color.DARKGREEN);
            }
        }
        image.getPixelWriter().setColor(1, 1, Color.ORANGE);
        image.getPixelWriter().setColor(2, 1, Color.ORANGE);
        image.getPixelWriter().setColor(6, 2, Color.ORANGE);

        AnalysisResult result = ImageAnalyser.analyse(
                image,
                List.of(Color.ORANGE),
                new MaskSettings(4, 0.05, 0.05, 0.05, 50),
                new NoiseSettings(1, 10, false));

        assertEquals(2, result.clusters().size());
        assertEquals(2, result.clusters().get(0).pixelCount());
        assertEquals(1, result.clusters().get(1).pixelCount());
    }
}
