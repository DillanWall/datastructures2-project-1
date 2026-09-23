package leafdetector.processing;

import leafdetector.model.LeafCluster;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TspPathPlannerTest {
    @Test
    void nearestNeighbourStartsWithChosenClusterAndVisitsAllClusters() {
        LeafCluster start = cluster(1, 0, 0);
        LeafCluster near = cluster(2, 10, 0);
        LeafCluster far = cluster(3, 100, 0);

        List<LeafCluster> path = TspPathPlanner.nearestNeighbour(List.of(far, near, start), start);

        assertEquals(List.of(start, near, far), path);
    }

    private static LeafCluster cluster(int id, int x, int y) {
        return new LeafCluster(id, id, id, 10, x, y, x + 2, y + 2, Color.BLUE);
    }
}
