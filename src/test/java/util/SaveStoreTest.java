package util;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SaveStoreTest {

    @Test
    void defaultStoreIsNonNullSingleton() {
        SaveStore first = SaveManager.defaultStore();
        SaveStore second = SaveManager.defaultStore();
        assertNotNull(first, "defaultStore() must not return null");
        assertSame(first, second, "defaultStore() must be a stable singleton");
    }

    /**
     * Verifies a SaveStore fake can be injected to replace the static
     * filesystem backend — this is the whole point of introducing the
     * interface. No disk I/O happens in this test.
     */
    @Test
    void fakeStoreInterceptsCalls() {
        AtomicBoolean saveCalled = new AtomicBoolean(false);
        SaveStore fake = new SaveStore() {
            @Override public boolean hasThreadState(int threadId) { return threadId == 42; }
            @Override public Object[] loadThreadState(int threadId, Map<Integer, Piece> allPieces) {
                return new Object[] { new Board(1, 1), new HashMap<Integer, Integer>(), 0, 123L };
            }
            @Override public void saveThreadState(Board b, Map<Integer, Piece> p, int d, int t, long s) {
                saveCalled.set(true);
            }
            @Override public boolean hasSavedState() { return true; }
            @Override public Object[] loadBestState(Map<Integer, Piece> allPieces) { return null; }
        };

        assertTrue(fake.hasThreadState(42));
        assertFalse(fake.hasThreadState(0));

        fake.saveThreadState(new Board(1, 1), new HashMap<>(), 10, 1, 99L);
        assertTrue(saveCalled.get(), "fake saveThreadState should have been invoked");

        Object[] state = fake.loadThreadState(42, new HashMap<>());
        assertNotNull(state);
        assertEquals(4, state.length);

        assertTrue(fake.hasSavedState());
        assertNull(fake.loadBestState(new HashMap<>()));
    }
}
