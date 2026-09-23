package leafdetector.model;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public record LeafCluster(
        int id,
        int rank,
        int root,
        int pixelCount,
        int minX,
        int minY,
        int maxX,
        int maxY,
        Color displayColor
) {
    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public Point2D centre() {
        return new Point2D((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }

    public boolean contains(int x, int y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public LeafCluster withRank(int newRank) {
        return new LeafCluster(id, newRank, root, pixelCount, minX, minY, maxX, maxY, displayColor);
    }
}
