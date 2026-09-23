package leafdetector.processing;

import leafdetector.model.LeafCluster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TspPathPlanner {
    private TspPathPlanner() {
    }

    public static List<LeafCluster> nearestNeighbour(List<LeafCluster> clusters, LeafCluster start) {
        if (clusters.isEmpty() || start == null) {
            return List.of();
        }

        List<LeafCluster> unvisited = new ArrayList<>(clusters);
        List<LeafCluster> path = new ArrayList<>();
        LeafCluster current = start;
        path.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            LeafCluster next = nearest(current, unvisited);
            path.add(next);
            unvisited.remove(next);
            current = next;
        }
        return path;
    }

    private static LeafCluster nearest(LeafCluster current, List<LeafCluster> candidates) {
        return candidates.stream()
                .min(Comparator.comparingDouble(candidate -> current.centre().distance(candidate.centre())))
                .orElseThrow();
    }
}
