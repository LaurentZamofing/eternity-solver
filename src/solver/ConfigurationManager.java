package solver;

import model.Board;
import model.Piece;
import model.Placement;
import util.SaveStateManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gère tous les paramètres de configuration du solveur Eternity.
 *
 * Cette classe centralise:
 * - Tous les drapeaux de configuration du solveur (verbose, useSingletons, useAC3, etc.)
 * - Métadonnées du puzzle (nom, label du thread, ordre de tri)
 * - Détection et gestion des pièces fixes
 * - Initialisation des gestionnaires (AutoSaveManager, RecordManager, etc.)
 *
 * Extrait de EternitySolver dans le Sprint 5 pour:
 * - Éliminer le code d'initialisation dupliqué
 * - Consolider la logique de détection des pièces fixes
 * - Fournir une source unique de vérité pour la configuration
 * - Simplifier les tests et la maintenance
 */
public class ConfigurationManager {

    // Drapeaux de configuration principaux
    private boolean useSingletons = true;
    private boolean verbose = true;
    private boolean useAC3 = true;
    private boolean useDomainCache = true;
    private boolean prioritizeBorders = false;

    // Configuration d'affichage et de log
    private int minDepthToShowRecords = 0;

    // Configuration du timeout
    private long maxExecutionTimeMs = Long.MAX_VALUE;

    // Métadonnées du puzzle
    private String puzzleName = "eternity2";
    private String threadLabel = "";
    private String sortOrder = "ascending";
    private int threadId = -1;

    // État des pièces fixes
    private int numFixedPieces = 0;
    private Set<String> fixedPositions = new HashSet<>();
    private List<SaveStateManager.PlacementInfo> initialFixedPieces = new ArrayList<>();

    // Gestion des threads et sauvegarde
    private long randomSeed = 0;
    private static final long THREAD_SAVE_INTERVAL = 60000; // 1 minute

    /**
     * Constructeur par défaut avec paramètres par défaut
     */
    public ConfigurationManager() {
    }

    // ============ Setters de Configuration ============

    public void setDisplayConfig(boolean verbose, int minDepth) {
        this.verbose = verbose;
        this.minDepthToShowRecords = minDepth;
    }

    public void setMinDepthToShowRecords(int minDepth) {
        this.minDepthToShowRecords = minDepth;
    }

    public void setPuzzleName(String name) {
        this.puzzleName = name;
    }

    public void setSortOrder(String order) {
        this.sortOrder = order;
    }

    public void setNumFixedPieces(int num) {
        this.numFixedPieces = num;
    }

    public void setMaxExecutionTime(long timeMs) {
        this.maxExecutionTimeMs = timeMs;
    }

    public void setThreadLabel(String label) {
        this.threadLabel = label;
    }

    public void setThreadId(int id) {
        this.threadId = id;
    }

    public void setUseSingletons(boolean enabled) {
        this.useSingletons = enabled;
    }

    public void setPrioritizeBorders(boolean enabled) {
        this.prioritizeBorders = enabled;
        if (verbose && enabled) {
            System.out.println("  🔲 Priorisation des bords activée - les bords seront remplis en premier");
        }
    }

    public void setVerbose(boolean enabled) {
        this.verbose = enabled;
    }

    public void setUseAC3(boolean enabled) {
        this.useAC3 = enabled;
    }

    public void setUseDomainCache(boolean enabled) {
        this.useDomainCache = enabled;
    }

    public void setRandomSeed(long seed) {
        this.randomSeed = seed;
    }

    // ============ Getters de Configuration ============

    public boolean isUseSingletons() {
        return useSingletons;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isUseAC3() {
        return useAC3;
    }

    public boolean isUseDomainCache() {
        return useDomainCache;
    }

    public boolean isPrioritizeBorders() {
        return prioritizeBorders;
    }

    public int getMinDepthToShowRecords() {
        return minDepthToShowRecords;
    }

    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public String getThreadLabel() {
        return threadLabel;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getNumFixedPieces() {
        return numFixedPieces;
    }

    public Set<String> getFixedPositions() {
        return new HashSet<>(fixedPositions);
    }

    public List<SaveStateManager.PlacementInfo> getInitialFixedPieces() {
        return new ArrayList<>(initialFixedPieces);
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public static long getThreadSaveInterval() {
        return THREAD_SAVE_INTERVAL;
    }

    // ============ Détection des Pièces Fixes ============

    /**
     * Détecte et initialise les pièces fixes depuis l'état du plateau.
     * Utilisé lors du démarrage d'une résolution avec des pièces pré-placées.
     *
     * @param board le plateau avec potentiellement des pièces pré-placées
     * @param pieceUsed bitset pour marquer les pièces utilisées
     * @param placementOrder liste à remplir avec les placements fixes
     */
    public void detectFixedPiecesFromBoard(Board board, BitSet pieceUsed,
                                           List<SaveStateManager.PlacementInfo> placementOrder) {
        fixedPositions.clear();
        numFixedPieces = 0;
        initialFixedPieces.clear();

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) {
                    fixedPositions.add(r + "," + c);
                    numFixedPieces++;

                    Placement placement = board.getPlacement(r, c);
                    int placedPieceId = placement.getPieceId();
                    int placedRotation = placement.getRotation();
                    pieceUsed.set(placedPieceId);

                    SaveStateManager.PlacementInfo fixedPiece =
                        new SaveStateManager.PlacementInfo(r, c, placedPieceId, placedRotation);
                    initialFixedPieces.add(fixedPiece);

                    if (placementOrder != null) {
                        placementOrder.add(fixedPiece);
                    }
                }
            }
        }
    }

    /**
     * Calcule le nombre de pièces fixes selon le nom du puzzle.
     * Utilisé lors de la reprise depuis un état sauvegardé (solveWithHistory).
     *
     * @param puzzleName nom du puzzle
     * @return nombre de pièces fixes (coins, indices)
     */
    public int calculateNumFixedPieces(String puzzleName) {
        if (puzzleName.startsWith("eternity2")) {
            return 9; // 4 coins + 5 indices pour Eternity II
        } else if (puzzleName.startsWith("indice")) {
            return 0; // Pas de pièces fixes pour les puzzles d'indices
        } else {
            return 0; // Par défaut: pas de pièces fixes
        }
    }

    /**
     * Construit la liste des pièces fixes initiales depuis l'ordre de placement préchargé.
     * Utilisé lors de la reprise depuis un état sauvegardé.
     *
     * @param preloadedOrder historique complet des placements
     * @param numFixedPieces nombre de pièces à traiter comme fixes
     */
    public void buildInitialFixedPieces(List<SaveStateManager.PlacementInfo> preloadedOrder,
                                       int numFixedPieces) {
        this.fixedPositions = new HashSet<>();
        this.numFixedPieces = numFixedPieces;
        this.initialFixedPieces.clear();

        for (int i = 0; i < Math.min(numFixedPieces, preloadedOrder.size()); i++) {
            initialFixedPieces.add(preloadedOrder.get(i));
        }
    }

    // ============ Initialisation des Managers ============

    /**
     * Crée et initialise AutoSaveManager avec la configuration actuelle.
     *
     * @param placementOrder historique des placements
     * @param allPieces map de toutes les pièces
     * @return AutoSaveManager initialisé
     */
    public AutoSaveManager createAutoSaveManager(
            List<SaveStateManager.PlacementInfo> placementOrder,
            Map<Integer, Piece> allPieces) {

        AutoSaveManager manager = new AutoSaveManager(
            puzzleName,
            numFixedPieces,
            initialFixedPieces,
            placementOrder
        );
        manager.initializePiecesMap(allPieces);
        return manager;
    }

    /**
     * Crée et initialise RecordManager avec la configuration actuelle.
     *
     * @param lockObject verrou de synchronisation
     * @param globalMaxDepth profondeur maximale globale atomique
     * @param globalBestScore meilleur score global atomique
     * @param globalBestThreadId ID du meilleur thread global atomique
     * @param globalBestBoard meilleur plateau global atomique
     * @param globalBestPieces meilleures pièces globales atomiques
     * @return RecordManager initialisé
     */
    public RecordManager createRecordManager(
            Object lockObject,
            AtomicInteger globalMaxDepth,
            AtomicInteger globalBestScore,
            AtomicInteger globalBestThreadId,
            AtomicReference<Board> globalBestBoard,
            AtomicReference<Map<Integer, Piece>> globalBestPieces) {

        return new RecordManager(
            puzzleName,
            threadId,
            minDepthToShowRecords,
            lockObject,
            globalMaxDepth,
            globalBestScore,
            globalBestThreadId,
            globalBestBoard,
            globalBestPieces
        );
    }

    /**
     * Affiche les paramètres de configuration actuels.
     */
    public void logConfiguration() {
        if (verbose) {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("  Configuration:");
            System.out.println("  - Puzzle: " + puzzleName);
            System.out.println("  - Thread: " + threadLabel);
            System.out.println("  - Singletons: " + (useSingletons ? "✓" : "✗"));
            System.out.println("  - AC-3: " + (useAC3 ? "✓" : "✗"));
            System.out.println("  - Domain Cache: " + (useDomainCache ? "✓" : "✗"));
            System.out.println("  - Prioritize Borders: " + (prioritizeBorders ? "✓" : "✗"));
            System.out.println("  - Fixed Pieces: " + numFixedPieces);
            System.out.println("═══════════════════════════════════════\n");
        }
    }
}
