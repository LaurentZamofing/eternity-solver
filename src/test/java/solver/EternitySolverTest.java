package solver;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.PuzzleFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour le solveur EternitySolver.
 */
public class EternitySolverTest {

    private EternitySolver solver;
    private PlacementValidator validator;
    private CellConstraints[][] cellConstraints;
    private EternitySolver.Statistics stats;

    @BeforeEach
    void setUp() {
        // Reset global state before each test to avoid interference
        EternitySolver.resetGlobalState();

        solver = new EternitySolver();
        stats = new EternitySolver.Statistics();

        // Initialize cell constraints for a 3x3 board using factory method
        cellConstraints = CellConstraints.createConstraintsMatrix(3, 3);

        validator = new PlacementValidator(cellConstraints, stats, "ascending");
    }

    @Test
    void testFitsWithBorderConstraint() {
        Board board = new Board(3, 3);
        int[] candidateEdges = {0, 1, 2, 0}; // N=0, E=1, S=2, W=0

        // Position (0,0) - coin supérieur gauche: N et W doivent être 0
        assertTrue(validator.fits(board, 0, 0, candidateEdges));

        // Position (0,0) avec N != 0 devrait échouer
        assertFalse(validator.fits(board, 0, 0, new int[]{1, 1, 2, 0}));

        // Position (0,0) avec W != 0 devrait échouer
        assertFalse(validator.fits(board, 0, 0, new int[]{0, 1, 2, 1}));
    }

    @Test
    void testFitsWithAdjacentPieces() {
        Board board = new Board(3, 3);
        Piece piece1 = new Piece(1, new int[]{0, 5, 3, 0}); // N=0, E=5, S=3, W=0

        board.place(0, 0, piece1, 0);

        // Pièce adjacente à l'est doit avoir W=5 pour correspondre
        int[] candidate = {0, 1, 2, 5}; // W=5
        assertTrue(validator.fits(board, 0, 1, candidate));

        // Pièce avec W != 5 devrait échouer
        assertFalse(validator.fits(board, 0, 1, new int[]{0, 1, 2, 4}));

        // Pièce adjacente au sud doit avoir N=3 pour correspondre
        int[] candidateSouth = {3, 1, 2, 0}; // N=3
        assertTrue(validator.fits(board, 1, 0, candidateSouth));

        // Pièce avec N != 3 devrait échouer
        assertFalse(validator.fits(board, 1, 0, new int[]{2, 1, 2, 0}));
    }

    @Test
    void testFindNextCellEmptyBoard() {
        Board board = new Board(3, 3);
        int[] cell = solver.findNextCell(board);

        assertNotNull(cell);
        assertEquals(0, cell[0]);
        assertEquals(0, cell[1]);
    }

    @Test
    void testFindNextCellPartiallyFilled() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});

        board.place(0, 0, piece, 0);
        board.place(0, 1, piece, 0);

        int[] cell = solver.findNextCell(board);
        assertNotNull(cell);
        assertEquals(0, cell[0]);
        assertEquals(2, cell[1]);
    }

    @Test
    void testFindNextCellFullBoard() {
        Board board = new Board(2, 2);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                board.place(r, c, piece, 0);
            }
        }

        int[] cell = solver.findNextCell(board);
        assertNull(cell);
    }

    @Test
    void testSolve1x1Puzzle() {
        Board board = new Board(1, 1);
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 0, 0, 0}));

        boolean solved = solver.solve(board, pieces);
        assertTrue(solved);
        assertFalse(board.isEmpty(0, 0));
    }

    @Test
    void testSolve1x1PuzzleImpossible() {
        Board board = new Board(1, 1);
        Map<Integer, Piece> pieces = new HashMap<>();
        // Pièce avec bords non-zéro ne peut pas être placée
        pieces.put(1, new Piece(1, new int[]{1, 1, 1, 1}));

        boolean solved = solver.solve(board, pieces);
        assertFalse(solved);
    }

    @Test
    void testSolve2x2SimplePuzzle() {
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Créer un puzzle 2x2 simple avec solution évidente
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0})); // top-left
        pieces.put(2, new Piece(2, new int[]{0, 0, 1, 1})); // top-right
        pieces.put(3, new Piece(3, new int[]{1, 1, 0, 0})); // bottom-left
        pieces.put(4, new Piece(4, new int[]{1, 0, 0, 1})); // bottom-right

        boolean solved = solver.solve(board, pieces);
        assertTrue(solved);

        // Vérifier que toutes les cases sont remplies
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                assertFalse(board.isEmpty(r, c), "Cell (" + r + "," + c + ") should be filled");
            }
        }
    }

    @Test
    void testSolve3x3Example() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();

        boolean solved = solver.solve(board, pieces);
        assertTrue(solved, "The 3x3 example puzzle should be solvable");

        // Vérifier que toutes les cases sont remplies
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                assertFalse(board.isEmpty(r, c), "Cell (" + r + "," + c + ") should be filled");
            }
        }

        // Vérifier que les contraintes sont respectées
        verifyBoardConstraints(board);
    }

    @Test
    void testSolveImpossiblePuzzle() {
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Créer un puzzle impossible (toutes les pièces ont des bords incompatibles)
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 2, 0}));
        pieces.put(3, new Piece(3, new int[]{0, 3, 3, 0}));
        pieces.put(4, new Piece(4, new int[]{0, 4, 4, 0}));

        boolean solved = solver.solve(board, pieces);
        assertFalse(solved, "This puzzle should be impossible to solve");
    }

    /**
     * Vérifie que toutes les contraintes du board sont respectées.
     */
    private void verifyBoardConstraints(Board board) {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                assertNotNull(p, "Cell (" + r + "," + c + ") should not be empty");

                int[] edges = p.edges;

                // Vérifier les bords extérieurs
                if (r == 0) assertEquals(0, edges[0], "North border should be 0");
                if (r == board.getRows() - 1) assertEquals(0, edges[2], "South border should be 0");
                if (c == 0) assertEquals(0, edges[3], "West border should be 0");
                if (c == board.getCols() - 1) assertEquals(0, edges[1], "East border should be 0");

                // Vérifier les contraintes avec les pièces adjacentes
                if (r > 0) {
                    Placement north = board.getPlacement(r - 1, c);
                    assertEquals(north.edges[2], edges[0], "North edge should match");
                }
                if (c > 0) {
                    Placement west = board.getPlacement(r, c - 1);
                    assertEquals(west.edges[1], edges[3], "West edge should match");
                }
            }
        }
    }
}
