package solver;

/**
 * Strategy interface for different piece placement approaches during backtracking.
 *
 * Implementations of this interface encapsulate different strategies for finding
 * and placing pieces on the puzzle board. This enables the Strategy design pattern
 * for separating placement logic from the main backtracking algorithm.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public interface PlacementStrategy {

    /**
     * Attempts to place a piece using this strategy's approach.
     *
     * This method should:
     * 1. Identify which cell(s) to try next (if applicable)
     * 2. Select piece(s) to try placing
     * 3. Attempt placements and recursively continue solving
     * 4. Handle backtracking if placements fail
     *
     * @param context The backtracking context containing board state, pieces, and statistics
     * @param solver The solver instance (needed for recursive calls and helper methods)
     * @return true if this strategy found a solution, false otherwise
     */
    boolean tryPlacement(BacktrackingContext context, EternitySolver solver);
}
