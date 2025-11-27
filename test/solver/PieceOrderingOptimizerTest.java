package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour PieceOrderingOptimizer.
 * Teste l'ordonnancement et la sélection de pièces pour une résolution efficace.
 */
@DisplayName("PieceOrderingOptimizer Tests")
public class PieceOrderingOptimizerTest {

    private PieceOrderingOptimizer optimizer;
    private PlacementValidator validator;
    private NeighborAnalyzer neighborAnalyzer;
    private EdgeCompatibilityIndex edgeIndex;
    private Board board;
    private Map<Integer, Piece> pieces;
    private BitSet pieceUsed;
    private CellConstraints[][] cellConstraints;

    @BeforeEach
    void setUp() {
        // Créer un plateau de test 5x5
        board = new Board(5, 5);
        cellConstraints = CellConstraints.createConstraintsMatrix(5, 5);

        // Créer des pièces de test
        pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0})); // Coin haut-gauche
        pieces.put(2, new Piece(2, new int[]{0, 2, 2, 0})); // Coin haut-droit
        pieces.put(3, new Piece(3, new int[]{1, 2, 3, 4})); // Pièce régulière
        pieces.put(4, new Piece(4, new int[]{2, 3, 4, 1})); // Pièce régulière
        pieces.put(5, new Piece(5, new int[]{3, 4, 1, 2})); // Pièce régulière

        // Créer les dépendances
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        validator = new PlacementValidator(cellConstraints, stats, "ascending");
        edgeIndex = new EdgeCompatibilityIndex(pieces, false);
        neighborAnalyzer = new NeighborAnalyzer(cellConstraints, validator, edgeIndex);

        // Créer l'optimiseur
        optimizer = new PieceOrderingOptimizer(edgeIndex, validator, neighborAnalyzer);

        // Initialiser le bitset des pièces utilisées
        pieceUsed = new BitSet(10);
    }

    @Test
    @DisplayName("getValidPlacements - Position coin avec pièces de coin")
    void testGetValidPlacements_Corner() {
        List<DomainManager.ValidPlacement> placements =
            optimizer.getValidPlacements(board, 0, 0, pieces, pieceUsed, 5);

        assertNotNull(placements);
        assertFalse(placements.isEmpty(), "Devrait trouver des placements valides pour le coin");

        // Vérifier que seules les pièces de coin sont incluses
        Set<Integer> pieceIds = new HashSet<>();
        for (DomainManager.ValidPlacement vp : placements) {
            pieceIds.add(vp.pieceId);
        }
        assertTrue(pieceIds.contains(1) || pieceIds.contains(2),
            "Devrait inclure au moins une pièce de coin");
    }

    @Test
    @DisplayName("getValidPlacements - Pièces utilisées sont exclues")
    void testGetValidPlacements_ExcludesUsedPieces() {
        pieceUsed.set(1); // Marquer pièce 1 comme utilisée

        List<DomainManager.ValidPlacement> placements =
            optimizer.getValidPlacements(board, 0, 0, pieces, pieceUsed, 5);

        // Vérifier que la pièce 1 n'est pas dans les placements
        for (DomainManager.ValidPlacement vp : placements) {
            assertNotEquals(1, vp.pieceId, "La pièce utilisée ne devrait pas apparaître");
        }
    }

    @Test
    @DisplayName("getValidPlacements - Position centrale")
    void testGetValidPlacements_CenterPosition() {
        List<DomainManager.ValidPlacement> placements =
            optimizer.getValidPlacements(board, 2, 2, pieces, pieceUsed, 5);

        assertNotNull(placements);
        // Position centrale a moins de contraintes, devrait avoir plus d'options
    }

    @Test
    @DisplayName("countValidPlacements - Compte correctement")
    void testCountValidPlacements() {
        int count = optimizer.countValidPlacements(board, 0, 0, pieces, pieceUsed, 5);

        assertTrue(count >= 0, "Le compte devrait être non négatif");

        // Vérifier que c'est cohérent avec getValidPlacements
        List<DomainManager.ValidPlacement> placements =
            optimizer.getValidPlacements(board, 0, 0, pieces, pieceUsed, 5);
        assertEquals(placements.size(), count,
            "countValidPlacements devrait retourner la même taille que getValidPlacements");
    }

    @Test
    @DisplayName("countUniquePieces - Compte les pièces uniques")
    void testCountUniquePieces() {
        int uniqueCount = optimizer.countUniquePieces(board, 0, 0, pieces, pieceUsed, 5);

        assertTrue(uniqueCount >= 0, "Le compte devrait être non négatif");
        assertTrue(uniqueCount <= 5, "Ne peut pas avoir plus de pièces que le total");
    }

    @Test
    @DisplayName("countUniquePieces - Avec pièces utilisées")
    void testCountUniquePieces_WithUsedPieces() {
        pieceUsed.set(1);
        pieceUsed.set(2);

        int uniqueCount = optimizer.countUniquePieces(board, 0, 0, pieces, pieceUsed, 5);

        assertTrue(uniqueCount <= 3, "Devrait compter au maximum 3 pièces (5 - 2 utilisées)");
    }

    @Test
    @DisplayName("orderPiecesByLeastConstraining - Ordonne par LCV")
    void testOrderPiecesByLeastConstraining() {
        List<Integer> availablePieces = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> ordered = optimizer.orderPiecesByLeastConstraining(
            board, 2, 2, availablePieces, pieces, pieceUsed, 5);

        assertNotNull(ordered);
        assertFalse(ordered.isEmpty(), "Devrait retourner une liste ordonnée");
        assertTrue(ordered.size() <= availablePieces.size(),
            "Liste ordonnée ne devrait pas être plus grande que la liste d'entrée");
    }

    @Test
    @DisplayName("orderPiecesByLeastConstraining - Gère les pièces utilisées")
    void testOrderPiecesByLeastConstraining_WithUsedPieces() {
        pieceUsed.set(3);
        List<Integer> availablePieces = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> ordered = optimizer.orderPiecesByLeastConstraining(
            board, 2, 2, availablePieces, pieces, pieceUsed, 5);

        assertFalse(ordered.contains(3), "Ne devrait pas inclure les pièces utilisées");
    }

    @Test
    @DisplayName("orderByDifficulty - Sans scores de difficulté")
    void testOrderByDifficulty_NoScores() {
        List<Integer> pieceIds = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> ordered = optimizer.orderByDifficulty(pieceIds, null);

        assertNotNull(ordered);
        assertEquals(pieceIds.size(), ordered.size(),
            "Devrait retourner une copie de la même taille");
    }

    @Test
    @DisplayName("orderByDifficulty - Avec scores de difficulté")
    void testOrderByDifficulty_WithScores() {
        List<Integer> pieceIds = Arrays.asList(1, 2, 3, 4, 5);
        Map<Integer, Integer> difficultyScores = new HashMap<>();
        difficultyScores.put(1, 10);  // Facile
        difficultyScores.put(2, 50);  // Difficile
        difficultyScores.put(3, 30);  // Moyen
        difficultyScores.put(4, 20);  // Facile-moyen
        difficultyScores.put(5, 40);  // Moyen-difficile

        List<Integer> ordered = optimizer.orderByDifficulty(pieceIds, difficultyScores);

        assertNotNull(ordered);
        assertEquals(5, ordered.size());
        // Les pièces plus faciles (score bas) devraient venir en premier
        assertTrue(ordered.indexOf(1) < ordered.indexOf(2),
            "Pièce facile devrait venir avant pièce difficile");
    }

    @Test
    @DisplayName("selectCornerPieceForThread - Thread 0")
    void testSelectCornerPieceForThread_Thread0() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5);

        Integer cornerPiece = optimizer.selectCornerPieceForThread(0, unusedIds, pieces);

        // Devrait retourner une pièce de coin ou null
        if (cornerPiece != null) {
            assertTrue(cornerPiece == 1 || cornerPiece == 2,
                "Devrait sélectionner une pièce de coin");
        }
    }

    @Test
    @DisplayName("selectCornerPieceForThread - Thread > 3 retourne null")
    void testSelectCornerPieceForThread_HighThreadId() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5);

        Integer cornerPiece = optimizer.selectCornerPieceForThread(5, unusedIds, pieces);

        assertNull(cornerPiece, "Thread ID >= 4 devrait retourner null");
    }

    @Test
    @DisplayName("selectCornerPieceForThread - Pas assez de pièces")
    void testSelectCornerPieceForThread_NotEnoughPieces() {
        List<Integer> unusedIds = Arrays.asList(1, 2); // Seulement 2 pièces

        Integer cornerPiece = optimizer.selectCornerPieceForThread(0, unusedIds, pieces);

        assertNull(cornerPiece, "Devrait retourner null si moins de 10 pièces disponibles");
    }

    @Test
    @DisplayName("getCornerPositionForThread - Thread 0 retourne coin haut-gauche")
    void testGetCornerPositionForThread_Thread0() {
        int[] position = optimizer.getCornerPositionForThread(0, 5, 5);

        assertNotNull(position);
        assertArrayEquals(new int[]{0, 0}, position, "Thread 0 devrait retourner (0,0)");
    }

    @Test
    @DisplayName("getCornerPositionForThread - Thread 1 retourne coin haut-droit")
    void testGetCornerPositionForThread_Thread1() {
        int[] position = optimizer.getCornerPositionForThread(1, 5, 5);

        assertNotNull(position);
        assertArrayEquals(new int[]{0, 4}, position, "Thread 1 devrait retourner (0,4)");
    }

    @Test
    @DisplayName("getCornerPositionForThread - Thread 2 retourne coin bas-gauche")
    void testGetCornerPositionForThread_Thread2() {
        int[] position = optimizer.getCornerPositionForThread(2, 5, 5);

        assertNotNull(position);
        assertArrayEquals(new int[]{4, 0}, position, "Thread 2 devrait retourner (4,0)");
    }

    @Test
    @DisplayName("getCornerPositionForThread - Thread 3 retourne coin bas-droit")
    void testGetCornerPositionForThread_Thread3() {
        int[] position = optimizer.getCornerPositionForThread(3, 5, 5);

        assertNotNull(position);
        assertArrayEquals(new int[]{4, 4}, position, "Thread 3 devrait retourner (4,4)");
    }

    @Test
    @DisplayName("getCornerPositionForThread - Thread invalide retourne null")
    void testGetCornerPositionForThread_InvalidThread() {
        int[] position = optimizer.getCornerPositionForThread(5, 5, 5);

        assertNull(position, "Thread ID invalide devrait retourner null");
    }

    @Test
    @DisplayName("findCornerRotation - Trouve rotation valide pour coin haut-gauche")
    void testFindCornerRotation_TopLeft() {
        Piece cornerPiece = pieces.get(1); // Pièce avec bords (0,1,1,0)

        int rotation = optimizer.findCornerRotation(cornerPiece, 0, 0, 5, 5);

        assertTrue(rotation >= 0 && rotation < 4 || rotation == -1,
            "Rotation devrait être entre 0-3 ou -1 si aucune valide");

        if (rotation != -1) {
            int[] edges = cornerPiece.edgesRotated(rotation);
            assertEquals(0, edges[0], "Nord devrait être 0 pour coin haut-gauche");
            assertEquals(0, edges[3], "Ouest devrait être 0 pour coin haut-gauche");
        }
    }

    @Test
    @DisplayName("findCornerRotation - Pièce non-coin retourne -1")
    void testFindCornerRotation_NonCornerPiece() {
        Piece regularPiece = pieces.get(3); // Pièce régulière sans zéros

        int rotation = optimizer.findCornerRotation(regularPiece, 0, 0, 5, 5);

        assertEquals(-1, rotation,
            "Pièce sans bords 0 appropriés devrait retourner -1");
    }

    @Test
    @DisplayName("Intégration - Workflow complet d'ordonnancement")
    void testIntegration_CompleteOrderingWorkflow() {
        // 1. Obtenir les placements valides
        List<DomainManager.ValidPlacement> placements =
            optimizer.getValidPlacements(board, 0, 0, pieces, pieceUsed, 5);

        assertNotNull(placements);

        // 2. Extraire les IDs de pièces uniques
        List<Integer> uniquePieceIds = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (DomainManager.ValidPlacement vp : placements) {
            if (!seen.contains(vp.pieceId)) {
                uniquePieceIds.add(vp.pieceId);
                seen.add(vp.pieceId);
            }
        }

        // 3. Ordonner par LCV
        List<Integer> orderedByLCV = optimizer.orderPiecesByLeastConstraining(
            board, 0, 0, uniquePieceIds, pieces, pieceUsed, 5);

        assertNotNull(orderedByLCV);
        assertTrue(orderedByLCV.size() <= uniquePieceIds.size());
    }
}
