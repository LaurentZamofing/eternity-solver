/**
 * Core data model for the Eternity II puzzle.
 * <p>
 * This package contains the fundamental data structures representing the puzzle
 * board, pieces, and their placements.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link model.Board} - Represents the puzzle grid (16x16 for Eternity II)</li>
 *   <li>{@link model.Piece} - Represents a puzzle piece with 4 edge values</li>
 *   <li>{@link model.Placement} - Represents a placed piece with rotation</li>
 * </ul>
 *
 * <h2>Board Representation</h2>
 * <p>
 * The {@link model.Board} class provides the main interface for puzzle state:
 * <ul>
 *   <li>Grid-based representation (rows × columns)</li>
 *   <li>Piece placement with rotation support (0°, 90°, 180°, 270°)</li>
 *   <li>Score calculation based on matching edges</li>
 *   <li>Efficient isEmpty() checks and getPlacement() queries</li>
 * </ul>
 *
 * <h3>Coordinate System</h3>
 * <pre>
 *       0   1   2   ... cols-1
 *     ┌───┬───┬───┬─────────┐
 *   0 │   │   │   │         │
 *     ├───┼───┼───┼─────────┤
 *   1 │   │   │   │         │
 *     ├───┼───┼───┼─────────┤
 *  ...│   │   │   │         │
 *     └───┴───┴───┴─────────┘
 * rows-1
 * </pre>
 *
 * <h2>Piece Structure</h2>
 * <p>
 * Each {@link model.Piece} has:
 * <ul>
 *   <li><b>ID</b>: Unique piece identifier (1-256 for Eternity II)</li>
 *   <li><b>Edges</b>: Array of 4 edge values [North, East, South, West]</li>
 * </ul>
 *
 * <h3>Edge Values</h3>
 * <ul>
 *   <li><b>0</b>: Gray edge (border constraint)</li>
 *   <li><b>1-22</b>: Color pattern IDs</li>
 * </ul>
 *
 * <p>Edge matching rule: Two adjacent pieces match if their touching edges have
 * the same value. For example:
 * <pre>
 * Piece A: edges = [1, 5, 3, 2]  (East=5)
 * Piece B: edges = [1, 7, 4, 5]  (West=5)
 * → Pieces A and B match horizontally (A.East == B.West)
 * </pre>
 *
 * <h2>Placement and Rotation</h2>
 * <p>
 * The {@link model.Placement} class represents a placed piece:
 * <ul>
 *   <li><b>Piece ID</b>: Which piece is placed</li>
 *   <li><b>Rotation</b>: 0, 1, 2, or 3 (× 90° clockwise)</li>
 *   <li><b>Rotated Edges</b>: Edge array after rotation applied</li>
 * </ul>
 *
 * <h3>Rotation Transformation</h3>
 * <p>Rotation is applied clockwise:
 * <table border="1">
 *   <caption>Edge Rotation</caption>
 *   <tr>
 *     <th>Rotation</th>
 *     <th>Degrees</th>
 *     <th>Transformation</th>
 *   </tr>
 *   <tr>
 *     <td>0</td>
 *     <td>0°</td>
 *     <td>[N, E, S, W] → [N, E, S, W]</td>
 *   </tr>
 *   <tr>
 *     <td>1</td>
 *     <td>90° CW</td>
 *     <td>[N, E, S, W] → [W, N, E, S]</td>
 *   </tr>
 *   <tr>
 *     <td>2</td>
 *     <td>180°</td>
 *     <td>[N, E, S, W] → [S, W, N, E]</td>
 *   </tr>
 *   <tr>
 *     <td>3</td>
 *     <td>270° CW</td>
 *     <td>[N, E, S, W] → [E, S, W, N]</td>
 *   </tr>
 * </table>
 *
 * <h2>Score Calculation</h2>
 * <p>
 * The board score is calculated by {@link model.Board#calculateScore()}:
 * <ul>
 *   <li>Returns int[2]: [current score, max possible score]</li>
 *   <li>Only counts <b>internal edges</b> (non-border adjacencies)</li>
 *   <li>+1 point for each matching internal edge</li>
 *   <li>Border edges (value 0) must match board edges but don't count toward score</li>
 * </ul>
 *
 * <h3>Example Score Calculation</h3>
 * <pre>
 * 2x2 Board:
 * ┌─────┬─────┐
 * │  A  │  B  │  A.East = 5, B.West = 5 → Match! (+1)
 * ├─────┼─────┤
 * │  C  │  D  │  C.East = 3, D.West = 7 → No match (0)
 * └─────┴─────┘  A.South = 2, C.North = 2 → Match! (+1)
 *                B.South = 4, D.North = 4 → Match! (+1)
 *
 * Total Score: 3 out of 4 internal edges = 3/4
 * </pre>
 *
 * <h2>Constraints</h2>
 * <p>
 * Hard constraints for valid puzzle state:
 * <ul>
 *   <li><b>Border constraint</b>: All edges touching the board border must be gray (0)</li>
 *   <li><b>Unique pieces</b>: Each piece can be placed at most once</li>
 *   <li><b>Edge matching</b>: Adjacent pieces must have matching edge values</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create board
 * Board board = new Board(4, 4);
 *
 * // Create piece (ID=1, edges=[0,5,3,0])
 * Piece piece = new Piece(1, new int[]{0, 5, 3, 0});
 *
 * // Place at (0,0) with 90° rotation
 * board.place(0, 0, piece, 1);
 *
 * // Check placement
 * Placement p = board.getPlacement(0, 0);
 * System.out.println("Piece " + p.getPieceId() + " at rotation " + p.getRotation());
 *
 * // Calculate score
 * int[] score = board.calculateScore();
 * System.out.println("Score: " + score[0] + "/" + score[1]);
 *
 * // Remove piece
 * board.remove(0, 0);
 * }</pre>
 *
 * <h2>Immutability</h2>
 * <p>
 * The {@link model.Piece} class is immutable - once created, a piece's edges cannot
 * be modified. This allows safe sharing of Piece objects across threads and
 * eliminates defensive copying.
 *
 * <p>
 * The {@link model.Placement} class is also immutable - it represents a snapshot
 * of a piece's placement at a specific moment.
 *
 * <p>
 * The {@link model.Board} class is mutable - it maintains the puzzle state and
 * supports place/remove operations. For thread-safe access, external synchronization
 * is required.
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>{@link model.Piece} - Thread-safe (immutable)</li>
 *   <li>{@link model.Placement} - Thread-safe (immutable)</li>
 *   <li>{@link model.Board} - Not thread-safe (requires external synchronization)</li>
 * </ul>
 *
 * @see solver Solver algorithms package
 * @see util Utility classes for board management
 * @since 1.0
 * @author Eternity Solver Team
 */
package model;
