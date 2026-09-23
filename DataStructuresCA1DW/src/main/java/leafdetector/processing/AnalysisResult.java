package leafdetector.processing;

import leafdetector.model.LeafCluster;
import javafx.scene.image.WritableImage;

import java.util.List;
import java.util.Map;

public record AnalysisResult(
        WritableImage scaledOriginal,
        WritableImage blackWhiteImage,
        boolean[] whiteMask,
        int[] rootByPixel,
        int width,
        int height,
        List<LeafCluster> clusters,
        Map<Integer, LeafCluster> clusterByRoot
) {
}
