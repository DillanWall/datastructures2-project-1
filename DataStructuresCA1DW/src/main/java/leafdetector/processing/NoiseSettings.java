package leafdetector.processing;

public class NoiseSettings {
    private final int minPixels;
    private final int maxPixels;
    private final boolean iqrFiltering;

    public NoiseSettings(int minPixels, int maxPixels, boolean iqrFiltering) {
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
        this.iqrFiltering = iqrFiltering;
    }

    public static NoiseSettings defaults() {
        return new NoiseSettings(12, 20_000, false);
    }

    public int minPixels() {
        return minPixels;
    }

    public int maxPixels() {
        return maxPixels;
    }

    public boolean iqrFiltering() {
        return iqrFiltering;
    }
}
