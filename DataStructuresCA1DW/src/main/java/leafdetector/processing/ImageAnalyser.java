package leafdetector.processing;

import leafdetector.model.LeafCluster;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ImageAnalyser {
    private ImageAnalyser() {
    }

    public static AnalysisResult analyse(Image source, List<Color> selectedColours,
                                         MaskSettings maskSettings, NoiseSettings noiseSettings) {
        if (source == null) {
            throw new IllegalArgumentException("An image is required.");
        }
        if (selectedColours == null || selectedColours.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one leaf colour.");
        }

        WritableImage scaled = scaleForAnalysis(source, maskSettings.maxAnalysisSize());
        int width = (int) scaled.getWidth();
        int height = (int) scaled.getHeight();
        boolean[] mask = buildMask(scaled, selectedColours, maskSettings);
        UnionFind unionFind = connectWhitePixels(mask, width, height);
        int[] rootByPixel = collectRoots(mask, unionFind);
        List<LeafCluster> clusters = buildClusters(mask, rootByPixel, width, height, noiseSettings);
        Map<Integer, LeafCluster> clusterByRoot = new HashMap<>();
        for (LeafCluster cluster : clusters) {
            clusterByRoot.put(cluster.root(), cluster);
        }
        WritableImage blackWhite = createBlackWhiteImage(mask, width, height);
        return new AnalysisResult(scaled, blackWhite, mask, rootByPixel, width, height, clusters, clusterByRoot);
    }

    public static WritableImage createBlackWhiteOnly(Image source, List<Color> selectedColours, MaskSettings settings) {
        WritableImage scaled = scaleForAnalysis(source, settings.maxAnalysisSize());
        boolean[] mask = buildMask(scaled, selectedColours, settings);
        return createBlackWhiteImage(mask, (int) scaled.getWidth(), (int) scaled.getHeight());
    }

    public static WritableImage colourSingleSet(AnalysisResult result, int root) {
        WritableImage image = createBlackWhiteImage(result.whiteMask(), result.width(), result.height());
        PixelWriter writer = image.getPixelWriter();
        Color highlight = Color.web("#f4d03f");
        for (int i = 0; i < result.rootByPixel().length; i++) {
            if (result.rootByPixel()[i] == root) {
                writer.setColor(i % result.width(), i / result.width(), highlight);
            }
        }
        return image;
    }

    public static WritableImage colourAllSets(AnalysisResult result) {
        WritableImage image = new WritableImage(result.width(), result.height());
        PixelWriter writer = image.getPixelWriter();
        Map<Integer, Color> colours = new HashMap<>();
        Random random = new Random(42);
        for (int i = 0; i < result.rootByPixel().length; i++) {
            int root = result.rootByPixel()[i];
            if (root < 0) {
                writer.setColor(i % result.width(), i / result.width(), Color.BLACK);
            } else {
                Color colour = colours.computeIfAbsent(root, ignored -> Color.hsb(random.nextDouble() * 360.0, 0.72, 0.95));
                writer.setColor(i % result.width(), i / result.width(), colour);
            }
        }
        return image;
    }

    public static int rootAt(AnalysisResult result, double imageX, double imageY) {
        int x = clamp((int) Math.round(imageX), 0, result.width() - 1);
        int y = clamp((int) Math.round(imageY), 0, result.height() - 1);
        return result.rootByPixel()[y * result.width() + x];
    }

    private static WritableImage scaleForAnalysis(Image source, int maxSize) {
        int sourceWidth = (int) source.getWidth();
        int sourceHeight = (int) source.getHeight();
        double scale = Math.min(1.0, maxSize / (double) Math.max(sourceWidth, sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        WritableImage scaled = new WritableImage(width, height);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = scaled.getPixelWriter();
        for (int y = 0; y < height; y++) {
            int sourceY = clamp((int) (y / scale), 0, sourceHeight - 1);
            for (int x = 0; x < width; x++) {
                int sourceX = clamp((int) (x / scale), 0, sourceWidth - 1);
                writer.setColor(x, y, reader.getColor(sourceX, sourceY));
            }
        }
        return scaled;
    }

    private static boolean[] buildMask(Image image, List<Color> selectedColours, MaskSettings settings) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        boolean[] mask = new boolean[width * height];
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = reader.getColor(x, y);
                mask[y * width + x] = selectedColours.stream().anyMatch(sample -> isLeafLike(pixel, sample, settings));
            }
        }
        return mask;
    }

    private static boolean isLeafLike(Color pixel, Color sample, MaskSettings settings) {
        double hueDistance = hueDistance(pixel.getHue(), sample.getHue());
        double saturationDistance = Math.abs(pixel.getSaturation() - sample.getSaturation());
        double brightnessDistance = Math.abs(pixel.getBrightness() - sample.getBrightness());
        double rgbDistance = Math.sqrt(
                Math.pow(pixel.getRed() - sample.getRed(), 2)
                        + Math.pow(pixel.getGreen() - sample.getGreen(), 2)
                        + Math.pow(pixel.getBlue() - sample.getBlue(), 2));
        return hueDistance <= settings.hueTolerance()
                && saturationDistance <= settings.saturationTolerance()
                && brightnessDistance <= settings.brightnessTolerance()
                && rgbDistance <= settings.rgbTolerance();
    }

    private static double hueDistance(double first, double second) {
        double distance = Math.abs(first - second);
        return Math.min(distance, 360.0 - distance);
    }

    private static UnionFind connectWhitePixels(boolean[] mask, int width, int height) {
        UnionFind unionFind = new UnionFind(mask.length);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (!mask[index]) {
                    continue;
                }
                if (x > 0 && mask[index - 1]) {
                    unionFind.union(index, index - 1);
                }
                if (y > 0 && mask[index - width]) {
                    unionFind.union(index, index - width);
                }
            }
        }
        return unionFind;
    }

    private static int[] collectRoots(boolean[] mask, UnionFind unionFind) {
        int[] roots = new int[mask.length];
        for (int i = 0; i < mask.length; i++) {
            roots[i] = mask[i] ? unionFind.find(i) : -1;
        }
        return roots;
    }

    private static List<LeafCluster> buildClusters(boolean[] mask, int[] roots, int width, int height, NoiseSettings settings) {
        Map<Integer, MutableBounds> bounds = new HashMap<>();
        for (int i = 0; i < mask.length; i++) {
            if (!mask[i]) {
                continue;
            }
            int x = i % width;
            int y = i / width;
            bounds.computeIfAbsent(roots[i], ignored -> new MutableBounds()).include(x, y);
        }

        List<MutableCluster> accepted = bounds.entrySet().stream()
                .map(entry -> entry.getValue().toCluster(entry.getKey()))
                .filter(cluster -> cluster.pixelCount >= settings.minPixels())
                .filter(cluster -> cluster.pixelCount <= settings.maxPixels())
                .toList();

        Set<Integer> outlierRoots = settings.iqrFiltering() ? outlierRoots(accepted) : Set.of();
        List<LeafCluster> clusters = new ArrayList<>();
        int id = 1;
        Random random = new Random(7);
        for (MutableCluster cluster : accepted) {
            if (!outlierRoots.contains(cluster.root)) {
                clusters.add(new LeafCluster(
                        id++,
                        0,
                        cluster.root,
                        cluster.pixelCount,
                        cluster.minX,
                        cluster.minY,
                        cluster.maxX,
                        cluster.maxY,
                        Color.hsb(random.nextDouble() * 360.0, 0.7, 0.95)
                ));
            }
        }

        clusters.sort(Comparator.comparingInt(LeafCluster::pixelCount).reversed());
        List<LeafCluster> ranked = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            ranked.add(clusters.get(i).withRank(i + 1));
        }
        return ranked;
    }

    private static Set<Integer> outlierRoots(List<MutableCluster> clusters) {
        if (clusters.size() < 4) {
            return Set.of();
        }
        List<Integer> sizes = clusters.stream().map(cluster -> cluster.pixelCount).sorted().toList();
        double q1 = percentile(sizes, 0.25);
        double q3 = percentile(sizes, 0.75);
        double iqr = q3 - q1;
        double lower = Math.max(0, q1 - 1.5 * iqr);
        double upper = q3 + 1.5 * iqr;
        Set<Integer> roots = new HashSet<>();
        for (MutableCluster cluster : clusters) {
            if (cluster.pixelCount < lower || cluster.pixelCount > upper) {
                roots.add(cluster.root);
            }
        }
        return roots;
    }

    private static double percentile(List<Integer> values, double percentile) {
        double index = percentile * (values.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return values.get(lower);
        }
        double fraction = index - lower;
        return values.get(lower) * (1.0 - fraction) + values.get(upper) * fraction;
    }

    private static WritableImage createBlackWhiteImage(boolean[] mask, int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int i = 0; i < mask.length; i++) {
            writer.setColor(i % width, i / width, mask[i] ? Color.WHITE : Color.BLACK);
        }
        return image;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class MutableBounds {
        private int pixelCount;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;

        private void include(int x, int y) {
            pixelCount++;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        private MutableCluster toCluster(int root) {
            return new MutableCluster(root, pixelCount, minX, minY, maxX, maxY);
        }
    }

    private record MutableCluster(int root, int pixelCount, int minX, int minY, int maxX, int maxY) {
    }
}
