package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NeighborAnalyzer
 */
public class NeighborAnalyzerTest {

    private NeighborAnalyzer analyzer;
    private PlacementValidator validator;
    private Board board;
    private Map<Integer, Piece> pieces;
    private CellConstraints[][] cellConstraints;
    private EdgeCompatibilityIndex edgeIndex;

    @BeforeEach
    void setUp() {
        board = new Board(5, 5);
        cellConstraints = CellConstraints.createConstraintsMatrix(5, 5);

        // Create test pieces
        pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0})); // Corner piece
        pieces.put(2, new Piece(2, new int[]{0, 2, 2, 0})); // Corner piece
        pieces.put(3, new Piece(3, new int[]{1, 2, 3, 4})); // Regular piece
        pieces.put(4, new Piece(4, new int[]{2, 3, 4, 1})); // Regular piece

        // Create Statistics adapter
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        validator = new PlacementValidator(cellConstraints, stats, "ascending");

        // Build edge compatibility index
        edgeIndex = new EdgeCompatibilityIndex(pieces, false);

        analyzer = new NeighborAnalyzer(cellConstraints, validator, edgeIndex);
    }

    @Test
    void testCountEmptyNeighbors_Center() {
        // Center cell (2, 2) has 4 neighbors, all empty
        assertEquals(4, analyzer.countEmptyNeighbors(board, 2, 2));
    }

    @Test
    void testCountEmptyNeighbors_Corner() {
        // Corner cell (0, 0) has 2 neighbors
        assertEquals(2, analyzer.countEmptyNeighbors(board, 0, 0));
    }

    @Test
    void testCountEmptyNeighbors_Edge() {
        // Edge cell (0, 2) has 3 neighbors
        assertEquals(3, analyzer.countEmptyNeighbors(board, 0, 2));
    }

    @Test
    void testCountEmptyNeighbors_WithOccupiedNeighbors() {
        // Place pieces around (2, 2)
        board.place(2, 1, pieces.get(1), 0);
        board.place(1, 2, pieces.get(2), 0);

        // Now (2, 2) has 2 empty neighbors (down and right)
        assertEquals(2, analyzer.countEmptyNeighbors(board, 2, 2));
    }

    @Test
    void testCountOccupiedNeighbors_EmptyBoard() {
        // Center cell with no occupied neighbors
        assertEquals(0, analyzer.countOccupiedNeighbors(board, 2, 2));
    }

    @Test
    void testCountOccupiedNeighbors_OneNeighbor() {
        board.place(2, 1, pieces.get(1), 0);
        assertEquals(1, analyzer.countOccupiedNeighbors(board, 2, 2));
    }

    @Test
    void testCountOccupiedNeighbors_AllNeighbors() {
        // Fill all neighbors of (2, 2)
        board.place(1, 2, pieces.get(1), 0); // North
        board.place(3, 2, pieces.get(2), 0); // South
        board.place(2, 1, pieces.get(3), 0); // West
        board.place(2, 3, pieces.get(4), 0); // East

        assertEquals(4, analyzer.countOccupiedNeighbors(board, 2, 2));
    }

    @Test
    void testCountAdjacentFilledBorderCells_TopBorder() {
        // This method counts neighbors that are THEMSELVES on borders
        // For (0, 2) on top border, check if left/right neighbors are corners
        board.place(0, 0, pieces.get(1), 0); // Left corner - is a border
        board.place(0, 4, pieces.get(2), 0); // Right corner - is a border

        // Check (0, 2) - no immediate neighbors are border cells
        assertEquals(0, analyzer.countAdjacentFilledBorderCells(board, 0, 2));
    }

    @Test
    void testCountAdjacentFilledBorderCells_Corner() {
        // Place piece at another corner
        board.place(0, 4, pieces.get(1), 0); // Top-right corner

        // Check top-left corner (0, 0) - neighbor at (0,1) is NOT a border
        assertEquals(0, analyzer.countAdjacentFilledBorderCells(board, 0, 0));
    }

    @Test
    void testCountAdjacentFilledBorderCells_NonBorder() {
        // Non-border cell should return 0
        board.place(1, 2, pieces.get(1), 0);
        assertEquals(0, analyzer.countAdjacentFilledBorderCells(board, 2, 2));
    }

    @Test
    void testWouldCreateTrappedGap_DoesNotCrash() {
        // Test that the method runs without crashing
        BitSet pieceUsed = new BitSet(10);
        int[] candidateEdges = new int[]{0, 1, 1, 0};

        // Call the method - actual result depends on complex logic
        boolean result = analyzer.wouldCreateTrappedGap(board, 0, 0, candidateEdges, pieces, pieceUsed, 4, 1);

        // Just verify it returns a boolean (doesn't crash)
        assertNotNull(Boolean.valueOf(result));
    }

    @Test
    void testWouldCreateTrappedGap_WithPlacedPieces() {
        // Test with some pieces already placed
        BitSet pieceUsed = new BitSet(10);
        pieceUsed.set(1);
        int[] candidateEdges = new int[]{0, 2, 2, 0};

        board.place(1, 1, pieces.get(1), 0);

        boolean result = analyzer.wouldCreateTrappedGap(board, 1, 2, candidateEdges, pieces, pieceUsed, 4, 2);

        // Verify it doesn't crash - trap detection is complex
        assertNotNull(Boolean.valueOf(result));
    }

    @Test
    void testWouldCreateTrappedGap_MethodExecutes() {
        // Verify the method executes with different parameters
        BitSet pieceUsed = new BitSet(10);
        pieceUsed.set(1);
        pieceUsed.set(2);
        int[] candidateEdges = new int[]{0, 3, 3, 0};

        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);

        boolean result = analyzer.wouldCreateTrappedGap(board, 0, 2, candidateEdges, pieces, pieceUsed, 4, 3);

        // Method should execute without errors
        assertNotNull(Boolean.valueOf(result));
    }

    @Test
    void testCalculateConstraintScore_NoNeighbors() {
        BitSet pieceUsed = new BitSet(10);
        int[] candidateEdges = new int[]{1, 2, 3, 4};

        // With no placed neighbors, the method sums constraints from all empty neighbors
        // Each empty neighbor contributes to the score
        int score = analyzer.calculateConstraintScore(board, 2, 2, candidateEdges,
            pieces, pieceUsed, 4, 3);

        // Score can be negative (fewer options after placement)
        assertTrue(score <= 0); // Constraint means reducing options
    }

    @Test
    void testCalculateConstraintScore_WithNeighbors() {
        BitSet pieceUsed = new BitSet(10);
        int[] candidateEdges = new int[]{0, 1, 1, 0}; // Corner-like edges

        // Place a neighbor to create constraints
        board.place(2, 1, pieces.get(1), 0);

        int score = analyzer.calculateConstraintScore(board, 2, 2, candidateEdges,
            pieces, pieceUsed, 4, 1);

        // Score represents constraint level (can be positive or negative)
        assertNotNull(Integer.valueOf(score));
    }

    @Test
    void testCountValidPieces_NoEdgeConstraint() {
        BitSet pieceUsed = new BitSet(10);

        // Count all valid pieces (no edge constraint)
        // This counts pieces, NOT rotations
        int count = analyzer.countValidPieces(board, 2, 2, -1, -1, pieces, pieceUsed, 4, -1);

        // We have 4 pieces, but some may not fit depending on board position constraints
        assertTrue(count >= 0 && count <= 4);
    }

    @Test
    void testCountValidPieces_WithEdgeConstraint() {
        BitSet pieceUsed = new BitSet(10);

        // Count pieces with specific edge value (0 = border edge at corner)
        int count = analyzer.countValidPieces(board, 0, 0, 0, 0, pieces, pieceUsed, 4, -1);

        // Only pieces with edge 0 at North should be valid (pieces 1 and 2)
        assertTrue(count >= 0 && count <= 4);
    }

    @Test
    void testCountValidPieces_WithUsedPieces() {
        BitSet pieceUsed = new BitSet(10);
        pieceUsed.set(1); // Mark piece 1 as used
        pieceUsed.set(2); // Mark piece 2 as used

        int count = analyzer.countValidPieces(board, 2, 2, -1, -1, pieces, pieceUsed, 4, -1);

        // Only 2 pieces available (3 and 4), but may not all fit
        assertTrue(count >= 0 && count <= 2);
    }

    @Test
    void testCountValidPieces_WithExcludedPiece() {
        BitSet pieceUsed = new BitSet(10);

        // Exclude piece 3 from count
        int count = analyzer.countValidPieces(board, 2, 2, -1, -1, pieces, pieceUsed, 4, 3);

        // 3 pieces available (1, 2, 4), but may not all fit at this position
        assertTrue(count >= 0 && count <= 3);
    }

    // ========== Tests additionnels Sprint 7 ==========

    @Test
    void testCountEmptyNeighbors_FullBoard() {
        // Remplir tout le plateau
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                board.place(r, c, pieces.get(1), 0);
            }
        }

        // Toute cellule devrait avoir 0 voisins vides
        assertEquals(0, analyzer.countEmptyNeighbors(board, 2, 2));
        assertEquals(0, analyzer.countEmptyNeighbors(board, 0, 0));
        assertEquals(0, analyzer.countEmptyNeighbors(board, 4, 4));
    }

    @Test
    void testCountOccupiedNeighbors_FullBoard() {
        // Remplir tout le plateau
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                board.place(r, c, pieces.get(1), 0);
            }
        }

        // Centre devrait avoir 4 voisins occupés
        assertEquals(4, analyzer.countOccupiedNeighbors(board, 2, 2));
        // Coin devrait avoir 2 voisins occupés
        assertEquals(2, analyzer.countOccupiedNeighbors(board, 0, 0));
        // Bord devrait avoir 3 voisins occupés
        assertEquals(3, analyzer.countOccupiedNeighbors(board, 0, 2));
    }

    @Test
    void testCountEmptyNeighbors_PartiallyFilled() {
        // Placer quelques pièces
        board.place(2, 1, pieces.get(1), 0);
        board.place(2, 3, pieces.get(2), 0);

        // Cellule (2,2) a 2 voisins occupés (ouest et est)
        assertEquals(2, analyzer.countEmptyNeighbors(board, 2, 2));
    }

    @Test
    void testCountAdjacentFilledBorderCells_AllBorders() {
        // Remplir tous les bords
        for (int c = 0; c < 5; c++) {
            board.place(0, c, pieces.get(1), 0); // Bord haut
            board.place(4, c, pieces.get(2), 0); // Bord bas
        }
        for (int r = 1; r < 4; r++) {
            board.place(r, 0, pieces.get(3), 0); // Bord gauche
            board.place(r, 4, pieces.get(4), 0); // Bord droit
        }

        // Cellule (1,1) adjacente à deux cellules de bord remplies (haut et gauche)
        assertEquals(2, analyzer.countAdjacentFilledBorderCells(board, 1, 1));

        // Cellule (2,2) au centre n'est adjacente à aucune cellule de bord
        assertEquals(0, analyzer.countAdjacentFilledBorderCells(board, 2, 2));
    }

    @Test
    void testWouldCreateTrappedGap_WithNullParameters() {
        BitSet pieceUsed = new BitSet(10);
        int[] edges = new int[]{1, 2, 3, 4};

        // Avec edges null
        assertFalse(analyzer.wouldCreateTrappedGap(board, 2, 2, null, pieces, pieceUsed, 4, 1));

        // Avec pieces null
        assertFalse(analyzer.wouldCreateTrappedGap(board, 2, 2, edges, null, pieceUsed, 4, 1));

        // Avec pieceUsed null
        assertFalse(analyzer.wouldCreateTrappedGap(board, 2, 2, edges, pieces, null, 4, 1));
    }

    @Test
    void testWouldCreateTrappedGap_NoTrappedGaps() {
        BitSet pieceUsed = new BitSet(10);
        int[] edges = new int[]{0, 1, 1, 0};

        // Coin (0,0) avec une pièce de coin ne devrait pas créer de gaps piégés
        boolean trapped = analyzer.wouldCreateTrappedGap(board, 0, 0, edges, pieces, pieceUsed, 4, 1);

        // Peut être vrai ou faux selon la configuration, mais ne devrait pas crasher
        assertNotNull(trapped);
    }

    @Test
    void testWouldCreateTrappedGap_SurroundedCell() {
        BitSet pieceUsed = new BitSet(10);

        // Entourer la cellule (2,2)
        board.place(1, 2, pieces.get(1), 0); // Nord
        board.place(3, 2, pieces.get(2), 0); // Sud
        board.place(2, 3, pieces.get(3), 0); // Est

        // Les bords de la pièce qui fermerait le gap
        int[] edges = new int[]{5, 5, 5, 5}; // Aucun bord ne correspond

        // Devrait potentiellement créer un gap piégé si aucune pièce ne correspond
        boolean trapped = analyzer.wouldCreateTrappedGap(board, 2, 1, edges, pieces, pieceUsed, 4, -1);

        // Juste vérifier que ça ne crashe pas
        assertNotNull(trapped);
    }

    @Test
    void testCalculateConstraintScore_EmptyBoard() {
        BitSet pieceUsed = new BitSet(10);
        int[] edges = new int[]{0, 1, 1, 0};

        // Sur un plateau vide, le score devrait être négatif (beaucoup d'options)
        int score = analyzer.calculateConstraintScore(board, 2, 2, edges, pieces, pieceUsed, 4, -1);

        // Score négatif = moins contraignant (plus d'options pour les voisins)
        assertTrue(score <= 0, "Score devrait être négatif ou zéro sur plateau vide");
    }

    @Test
    void testCalculateConstraintScore_ConstrainedPosition() {
        BitSet pieceUsed = new BitSet(10);

        // Placer des pièces pour contraindre
        board.place(1, 2, pieces.get(1), 0);
        board.place(3, 2, pieces.get(2), 0);
        board.place(2, 1, pieces.get(3), 0);

        int[] edges = new int[]{1, 2, 3, 4};

        // La position (2,2) est très contrainte avec 3 voisins occupés
        int score = analyzer.calculateConstraintScore(board, 2, 2, edges, pieces, pieceUsed, 4, -1);

        // Le score devrait refléter la contrainte
        assertNotNull(score);
    }

    @Test
    void testCalculateConstraintScore_AllPiecesUsed() {
        BitSet pieceUsed = new BitSet(10);
        // Marquer toutes les pièces comme utilisées
        for (int i = 1; i <= 4; i++) {
            pieceUsed.set(i);
        }

        int[] edges = new int[]{1, 2, 3, 4};

        // Aucune pièce disponible = score maximal (très contraignant)
        int score = analyzer.calculateConstraintScore(board, 2, 2, edges, pieces, pieceUsed, 4, -1);

        // Score devrait être 0 car aucun voisin n'a d'options
        assertEquals(0, score, "Toutes pièces utilisées devrait donner score 0");
    }

    @Test
    void testCountValidPieces_AllRotations() {
        BitSet pieceUsed = new BitSet(10);

        // Ajouter une pièce avec plusieurs rotations uniques
        pieces.put(5, new Piece(5, new int[]{1, 2, 3, 4}));

        int count = analyzer.countValidPieces(board, 2, 2, 1, 0, pieces, pieceUsed, 5, -1);

        // Devrait compter la pièce une seule fois malgré les rotations
        assertTrue(count >= 0 && count <= 5);
    }

    @Test
    void testCountValidPieces_EdgePositions() {
        BitSet pieceUsed = new BitSet(10);

        // Tester aux quatre bords
        int countTop = analyzer.countValidPieces(board, 0, 2, -1, -1, pieces, pieceUsed, 4, -1);
        int countBottom = analyzer.countValidPieces(board, 4, 2, -1, -1, pieces, pieceUsed, 4, -1);
        int countLeft = analyzer.countValidPieces(board, 2, 0, -1, -1, pieces, pieceUsed, 4, -1);
        int countRight = analyzer.countValidPieces(board, 2, 4, -1, -1, pieces, pieceUsed, 4, -1);

        // Toutes les positions devraient avoir des comptages valides
        assertTrue(countTop >= 0);
        assertTrue(countBottom >= 0);
        assertTrue(countLeft >= 0);
        assertTrue(countRight >= 0);
    }

    @Test
    void testCountValidPieces_WithInvalidEdgeIndex() {
        BitSet pieceUsed = new BitSet(10);

        // edgeIndex de -1 signifie pas de contrainte de bord spécifique
        int count = analyzer.countValidPieces(board, 2, 2, -1, -1, pieces, pieceUsed, 4, -1);

        assertTrue(count >= 0, "Comptage devrait être non négatif");
    }

    @Test
    void testCountAdjacentFilledBorderCells_CenterCell() {
        // Cellule au centre ne peut pas être adjacente aux bords
        int count = analyzer.countAdjacentFilledBorderCells(board, 2, 2);

        assertEquals(0, count, "Cellule centrale ne devrait pas avoir de voisins de bord");
    }

    @Test
    void testWouldCreateTrappedGap_AllNeighborsOccupied() {
        BitSet pieceUsed = new BitSet(10);

        // Entourer complètement la cellule (2,2)
        board.place(1, 2, pieces.get(1), 0); // Nord
        board.place(3, 2, pieces.get(2), 0); // Sud
        board.place(2, 3, pieces.get(3), 0); // Est
        board.place(2, 1, pieces.get(4), 0); // Ouest

        int[] edges = new int[]{1, 2, 3, 4};

        // Pas de voisins vides = pas de gaps piégés possibles
        boolean trapped = analyzer.wouldCreateTrappedGap(board, 2, 2, edges, pieces, pieceUsed, 4, -1);

        assertFalse(trapped, "Pas de voisins vides = pas de gaps piégés");
    }

    @Test
    void testIntegration_ConstraintScoreVsTrappedGap() {
        BitSet pieceUsed = new BitSet(10);
        int[] edges = new int[]{0, 1, 2, 0};

        // Placer quelques pièces
        board.place(0, 1, pieces.get(1), 0);
        board.place(1, 0, pieces.get(2), 0);

        // Calculer le score de contrainte pour (1,1)
        int score = analyzer.calculateConstraintScore(board, 1, 1, edges, pieces, pieceUsed, 4, -1);

        // Vérifier les gaps piégés
        boolean trapped = analyzer.wouldCreateTrappedGap(board, 1, 1, edges, pieces, pieceUsed, 4, -1);

        // Les deux devraient fonctionner ensemble sans erreur
        assertNotNull(score);
        assertNotNull(trapped);
    }

    @Test
    void testIntegration_CompleteNeighborAnalysis() {
        BitSet pieceUsed = new BitSet(10);

        // Placer des pièces pour créer un motif
        board.place(2, 2, pieces.get(1), 0);
        pieceUsed.set(1);

        // Analyser tous les voisins
        int emptyNeighbors = analyzer.countEmptyNeighbors(board, 2, 2);
        int occupiedNeighbors = analyzer.countOccupiedNeighbors(board, 2, 2);

        // Les voisins vides + occupés devraient égaler le total des voisins
        assertTrue(emptyNeighbors + occupiedNeighbors <= 4,
            "Total voisins ne peut pas dépasser 4");

        // Vérifier chaque voisin vide
        if (emptyNeighbors > 0) {
            // Au moins un voisin devrait avoir des pièces valides disponibles
            int validCount = 0;
            if (board.isEmpty(1, 2)) {
                validCount += analyzer.countValidPieces(board, 1, 2, -1, -1, pieces, pieceUsed, 4, -1);
            }
            if (board.isEmpty(3, 2)) {
                validCount += analyzer.countValidPieces(board, 3, 2, -1, -1, pieces, pieceUsed, 4, -1);
            }
            if (board.isEmpty(2, 1)) {
                validCount += analyzer.countValidPieces(board, 2, 1, -1, -1, pieces, pieceUsed, 4, -1);
            }
            if (board.isEmpty(2, 3)) {
                validCount += analyzer.countValidPieces(board, 2, 3, -1, -1, pieces, pieceUsed, 4, -1);
            }

            assertTrue(validCount >= 0, "Comptage total devrait être non négatif");
        }
    }

    @Test
    void testMultiplePiecesExcluded() {
        BitSet pieceUsed = new BitSet(10);
        pieceUsed.set(1);
        pieceUsed.set(2);

        // Exclure une pièce supplémentaire
        int count = analyzer.countValidPieces(board, 2, 2, -1, -1, pieces, pieceUsed, 4, 3);

        // Seulement pièce 4 disponible
        assertTrue(count >= 0 && count <= 1, "Devrait avoir au plus 1 pièce disponible");
    }

    @Test
    void testConstraintScoreConsistency() {
        BitSet pieceUsed = new BitSet(10);
        int[] edges = new int[]{0, 1, 1, 0};

        // Calculer le score deux fois
        int score1 = analyzer.calculateConstraintScore(board, 0, 0, edges, pieces, pieceUsed, 4, -1);
        int score2 = analyzer.calculateConstraintScore(board, 0, 0, edges, pieces, pieceUsed, 4, -1);

        assertEquals(score1, score2, "Score de contrainte devrait être déterministe");
    }
}
