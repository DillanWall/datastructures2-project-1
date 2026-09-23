package leafdetector.processing;

public class UnionFind {
    private final int[] parent;
    private final int[] size;

    public UnionFind(int count) {
        parent = new int[count];
        size = new int[count];
        for (int i = 0; i < count; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int item) {
        int root = item;
        while (root != parent[root]) {
            root = parent[root];
        }
        while (item != root) {
            int next = parent[item];
            parent[item] = root;
            item = next;
        }
        return root;
    }

    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (size[firstRoot] < size[secondRoot]) {
            int swap = firstRoot;
            firstRoot = secondRoot;
            secondRoot = swap;
        }
        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        return true;
    }

    public int sizeOf(int item) {
        return size[find(item)];
    }
}
