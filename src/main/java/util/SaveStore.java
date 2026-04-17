package util;

import model.Piece;

import java.util.Map;

/**
 * Abstraction over persisted solver state.
 *
 * <p>Introduced to decouple callers from {@link SaveManager}'s static
 * methods so that tests can inject a fake or an in-memory store.
 * Production code uses {@link SaveManager#defaultStore()} which
 * simply delegates to the existing static implementation; the file
 * layout is unchanged.</p>
 *
 * <p>Only the methods actually consumed by solver code are exposed.
 * The legacy {@code saveBestState}/{@code loadBestState} pair remains
 * accessible via the static API.</p>
 */
public interface SaveStore {

    /** True if a save file exists for the given thread id. */
    boolean hasThreadState(int threadId);

    /** Loads a thread-specific save, or null if none exists. */
    Object[] loadThreadState(int threadId, Map<Integer, Piece> allPieces);

    /** Persists a thread-specific save. */
    void saveThreadState(model.Board board, Map<Integer, Piece> allPieces,
                         int depth, int threadId, long randomSeed);

    /** True if a global "best state" save exists. */
    boolean hasSavedState();

    /** Loads the global best-state save, or null. */
    Object[] loadBestState(Map<Integer, Piece> allPieces);
}
