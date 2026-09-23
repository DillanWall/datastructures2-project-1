package leafdetector.benchmark;

import leafdetector.processing.ImageAnalyser;
import leafdetector.processing.MaskSettings;
import leafdetector.processing.NoiseSettings;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;

@State(Scope.Benchmark)
public class ImageAnalyserBenchmark {
    private final WritableImage image = syntheticImage();

    @Benchmark
    public int analyseSyntheticImage() {
        return ImageAnalyser.analyse(
                image,
                List.of(Color.ORANGE, Color.CRIMSON),
                MaskSettings.defaults(),
                new NoiseSettings(3, 10_000, false)
        ).clusters().size();
    }

    private static WritableImage syntheticImage() {
        WritableImage image = new WritableImage(400, 300);
        for (int y = 0; y < 300; y++) {
            for (int x = 0; x < 400; x++) {
                Color colour = ((x / 20 + y / 20) % 9 == 0) ? Color.ORANGE : Color.web("#36553a");
                image.getPixelWriter().setColor(x, y, colour);
            }
        }
        return image;
    }
}
