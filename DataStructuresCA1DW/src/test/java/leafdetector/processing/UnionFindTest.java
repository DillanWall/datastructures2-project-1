package leafdetector.processing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnionFindTest {
    @Test
    void unionsConnectedItemsIntoOneSet() {
        UnionFind unionFind = new UnionFind(8);

        assertTrue(unionFind.union(1, 2));
        assertTrue(unionFind.union(2, 3));
        assertTrue(unionFind.union(6, 7));

        assertEquals(unionFind.find(1), unionFind.find(3));
        assertEquals(3, unionFind.sizeOf(1));
        assertNotEquals(unionFind.find(1), unionFind.find(6));
    }
}
