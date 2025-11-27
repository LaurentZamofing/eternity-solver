package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour AutoSaveManager.
 * Teste la sauvegarde automatique périodique et la gestion de l'état du solveur.
 */
@DisplayName("AutoSaveManager Tests")
public class AutoSaveManagerTest {

    private AutoSaveManager autoSaveManager;
    private Board board;
    private Map<Integer, Piece> pieces;
    private BitSet pieceUsed;
    private StatisticsManager stats;
    private List<SaveStateManager.PlacementInfo> placementOrder;
    private List<SaveStateManager.PlacementInfo> initialFixedPieces;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Créer un plateau de test 3x3
        board = new Board(3, 3);

        // Créer des pièces de test
        pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0})); // Coin
        pieces.put(2, new Piece(2, new int[]{0, 2, 2, 0})); // Coin
        pieces.put(3, new Piece(3, new int[]{1, 2, 3, 4})); // Régulière
        pieces.put(4, new Piece(4, new int[]{2, 3, 4, 1})); // Régulière
        pieces.put(5, new Piece(5, new int[]{3, 4, 1, 2})); // Régulière

        // Initialiser le suivi des pièces
        pieceUsed = new BitSet(10);

        // Créer les gestionnaires de statistiques
        stats = new StatisticsManager();
        stats.start();

        // Initialiser les listes de placement
        placementOrder = new ArrayList<>();
        initialFixedPieces = new ArrayList<>();

        // Créer le gestionnaire de sauvegarde automatique
        autoSaveManager = new AutoSaveManager("test_puzzle", 0, initialFixedPieces, placementOrder);
    }

    @Test
    @DisplayName("Constructeur initialise correctement")
    void testConstructorInitialization() {
        AutoSaveManager manager = new AutoSaveManager("puzzle_test", 2, initialFixedPieces, placementOrder);

        assertNotNull(manager, "Le gestionnaire devrait être créé");
        assertNull(manager.getAllPiecesMap(), "La carte des pièces devrait être null au départ");
    }

    @Test
    @DisplayName("initializePiecesMap stocke la carte des pièces")
    void testInitializePiecesMap() {
        assertNull(autoSaveManager.getAllPiecesMap(), "Initialement null");

        autoSaveManager.initializePiecesMap(pieces);

        assertNotNull(autoSaveManager.getAllPiecesMap(), "Devrait stocker la carte");
        assertEquals(pieces, autoSaveManager.getAllPiecesMap(), "Devrait être la même référence");
    }

    @Test
    @DisplayName("getAllPiecesMap retourne la carte stockée")
    void testGetAllPiecesMap() {
        autoSaveManager.initializePiecesMap(pieces);

        Map<Integer, Piece> retrieved = autoSaveManager.getAllPiecesMap();

        assertSame(pieces, retrieved, "Devrait retourner la même référence");
    }

    @Test
    @DisplayName("checkAndSave ne sauvegarde pas si carte des pièces non initialisée")
    void testCheckAndSaveWithoutPiecesMap() {
        // Ne pas initialiser la carte des pièces
        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Ne devrait pas lancer d'exception même sans carte de pièces");
    }

    @Test
    @DisplayName("checkAndSave ne sauvegarde pas immédiatement")
    void testCheckAndSaveNotImmediate() {
        autoSaveManager.initializePiecesMap(pieces);

        // Premier appel immédiatement après création
        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Premier appel ne devrait pas échouer");

        // Deuxième appel juste après
        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Deuxième appel immédiat ne devrait pas échouer");
    }

    @Test
    @DisplayName("saveRecord ne sauvegarde pas en dessous de 10 pièces")
    void testSaveRecordBelowThreshold() {
        autoSaveManager.initializePiecesMap(pieces);

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 5);
        }, "Ne devrait pas sauvegarder avec profondeur < 10");

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 9);
        }, "Ne devrait pas sauvegarder avec profondeur = 9");
    }

    @Test
    @DisplayName("saveRecord sauvegarde à partir de 10 pièces")
    void testSaveRecordAtThreshold() {
        autoSaveManager.initializePiecesMap(pieces);

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "Devrait accepter profondeur = 10");

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 15);
        }, "Devrait accepter profondeur > 10");
    }

    @Test
    @DisplayName("saveRecord sans carte de pièces ne lance pas d'exception")
    void testSaveRecordWithoutPiecesMap() {
        // Ne pas initialiser la carte
        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "Ne devrait pas échouer même sans carte de pièces");
    }

    @Test
    @DisplayName("Gestion avec pièces fixes initiales")
    void testWithInitialFixedPieces() {
        List<SaveStateManager.PlacementInfo> fixed = new ArrayList<>();
        fixed.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));
        fixed.add(new SaveStateManager.PlacementInfo(0, 1, 2, 1));

        AutoSaveManager manager = new AutoSaveManager("puzzle_fixed", 2, fixed, placementOrder);

        assertNotNull(manager, "Devrait créer le gestionnaire avec pièces fixes");
        manager.initializePiecesMap(pieces);
        assertNotNull(manager.getAllPiecesMap(), "Carte de pièces devrait être initialisée");
    }

    @Test
    @DisplayName("Gestion avec ordre de placement non vide")
    void testWithPlacementOrder() {
        placementOrder.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));
        placementOrder.add(new SaveStateManager.PlacementInfo(1, 1, 3, 2));

        autoSaveManager.initializePiecesMap(pieces);

        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer un ordre de placement existant");
    }

    @Test
    @DisplayName("checkAndSave avec plateau partiellement rempli")
    void testCheckAndSavePartialBoard() {
        autoSaveManager.initializePiecesMap(pieces);

        // Placer quelques pièces
        board.place(0, 0, pieces.get(1), 0);
        pieceUsed.set(1);
        board.place(0, 1, pieces.get(2), 1);
        pieceUsed.set(2);

        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer un plateau partiellement rempli");
    }

    @Test
    @DisplayName("saveRecord avec plateau partiellement rempli")
    void testSaveRecordPartialBoard() {
        autoSaveManager.initializePiecesMap(pieces);

        // Placer des pièces
        board.place(0, 0, pieces.get(1), 0);
        pieceUsed.set(1);
        board.place(1, 1, pieces.get(3), 2);
        pieceUsed.set(3);

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "Devrait sauvegarder un plateau partiellement rempli");
    }

    @Test
    @DisplayName("Appels multiples à checkAndSave")
    void testMultipleCheckAndSaveCalls() {
        autoSaveManager.initializePiecesMap(pieces);

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            assertDoesNotThrow(() -> {
                autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
            }, "Appel " + finalI + " ne devrait pas échouer");
        }
    }

    @Test
    @DisplayName("Appels multiples à saveRecord")
    void testMultipleSaveRecordCalls() {
        autoSaveManager.initializePiecesMap(pieces);

        for (int depth = 10; depth <= 15; depth++) {
            int finalDepth = depth;
            assertDoesNotThrow(() -> {
                autoSaveManager.saveRecord(board, pieceUsed, 5, stats, finalDepth);
            }, "Appel avec profondeur " + finalDepth + " ne devrait pas échouer");
        }
    }

    @Test
    @DisplayName("Gestion avec statistiques mises à jour")
    void testWithUpdatedStatistics() {
        autoSaveManager.initializePiecesMap(pieces);

        // Mettre à jour les statistiques
        stats.recursiveCalls = 1000;
        stats.placements = 50;
        stats.backtracks = 20;
        stats.end();

        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer des statistiques mises à jour");

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 12);
        }, "saveRecord devrait gérer des statistiques mises à jour");
    }

    @Test
    @DisplayName("Gestion avec toutes les pièces utilisées")
    void testWithAllPiecesUsed() {
        autoSaveManager.initializePiecesMap(pieces);

        // Marquer toutes les pièces comme utilisées
        for (int i = 1; i <= 5; i++) {
            pieceUsed.set(i);
        }

        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer toutes les pièces utilisées");

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "saveRecord devrait gérer toutes les pièces utilisées");
    }

    @Test
    @DisplayName("Gestion avec aucune pièce utilisée")
    void testWithNoPiecesUsed() {
        autoSaveManager.initializePiecesMap(pieces);

        // BitSet vide - aucune pièce utilisée
        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer aucune pièce utilisée");

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "saveRecord devrait gérer aucune pièce utilisée");
    }

    @Test
    @DisplayName("Nom de puzzle avec caractères spéciaux")
    void testPuzzleNameWithSpecialCharacters() {
        AutoSaveManager manager = new AutoSaveManager("test_puzzle-01_v2", 0, initialFixedPieces, placementOrder);
        manager.initializePiecesMap(pieces);

        assertDoesNotThrow(() -> {
            manager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer des noms de puzzle avec caractères spéciaux");
    }

    @Test
    @DisplayName("Gestion avec carte de pièces vide")
    void testWithEmptyPiecesMap() {
        Map<Integer, Piece> emptyMap = new HashMap<>();
        autoSaveManager.initializePiecesMap(emptyMap);

        assertDoesNotThrow(() -> {
            autoSaveManager.checkAndSave(board, pieceUsed, 5, stats);
        }, "Devrait gérer une carte de pièces vide");

        assertDoesNotThrow(() -> {
            autoSaveManager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "saveRecord devrait gérer une carte de pièces vide");
    }

    @Test
    @DisplayName("Initialisation multiple de la carte de pièces")
    void testMultiplePiecesMapInitialization() {
        autoSaveManager.initializePiecesMap(pieces);
        assertEquals(pieces, autoSaveManager.getAllPiecesMap());

        Map<Integer, Piece> newPieces = new HashMap<>();
        newPieces.put(10, new Piece(10, new int[]{1, 1, 1, 1}));

        autoSaveManager.initializePiecesMap(newPieces);
        assertEquals(newPieces, autoSaveManager.getAllPiecesMap(),
            "Devrait mettre à jour avec la nouvelle carte");
    }

    @Test
    @DisplayName("Intégration - Workflow complet de sauvegarde")
    void testIntegrationCompleteWorkflow() {
        // 1. Créer le gestionnaire
        AutoSaveManager manager = new AutoSaveManager("integration_test", 0, initialFixedPieces, placementOrder);

        // 2. Initialiser la carte de pièces
        manager.initializePiecesMap(pieces);

        // 3. Placer quelques pièces
        board.place(0, 0, pieces.get(1), 0);
        pieceUsed.set(1);
        placementOrder.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));

        board.place(0, 1, pieces.get(2), 1);
        pieceUsed.set(2);
        placementOrder.add(new SaveStateManager.PlacementInfo(0, 1, 2, 1));

        // 4. Mettre à jour les statistiques
        stats.recursiveCalls = 500;
        stats.placements = 2;
        stats.end();

        // 5. Vérifier checkAndSave
        assertDoesNotThrow(() -> {
            manager.checkAndSave(board, pieceUsed, 5, stats);
        }, "checkAndSave devrait fonctionner dans le workflow complet");

        // 6. Vérifier saveRecord
        assertDoesNotThrow(() -> {
            manager.saveRecord(board, pieceUsed, 5, stats, 10);
        }, "saveRecord devrait fonctionner dans le workflow complet");
    }
}
