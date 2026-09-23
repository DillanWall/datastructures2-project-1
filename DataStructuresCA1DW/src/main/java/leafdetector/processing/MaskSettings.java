package leafdetector.processing;

public class MaskSettings {
    private final double hueTolerance;
    private final double saturationTolerance;
    private final double brightnessTolerance;
    private final double rgbTolerance;
    private final int maxAnalysisSize;

    public MaskSettings(double hueTolerance, double saturationTolerance, double brightnessTolerance,
                        double rgbTolerance, int maxAnalysisSize) {
        this.hueTolerance = hueTolerance;
        this.saturationTolerance = saturationTolerance;
        this.brightnessTolerance = brightnessTolerance;
        this.rgbTolerance = rgbTolerance;
        this.maxAnalysisSize = maxAnalysisSize;
    }

    public static MaskSettings defaults() {
        return new MaskSettings(35.0, 0.45, 0.45, 0.34, 700);
    }

    public double hueTolerance() {
        return hueTolerance;
    }

    public double saturationTolerance() {
        return saturationTolerance;
    }

    public double brightnessTolerance() {
        return brightnessTolerance;
    }

    public double rgbTolerance() {
        return rgbTolerance;
    }

    public int maxAnalysisSize() {
        return maxAnalysisSize;
    }
}
