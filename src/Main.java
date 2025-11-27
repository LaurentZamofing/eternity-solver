import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;
import util.SaveManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Point d'entrée principal de l'application.
 * Exécute le solveur sur l'exemple 3x3 prédéfini.
 */
public class Main {

    public static void main(String[] args) {
        // CHOISIR LE PUZZLE À LANCER :

        // Puzzle 16x16 (lignes A-P, colonnes 1-16)
        // runPuzzle16x16();

        // Puzzle 6x12 (lignes A-F, colonnes 1-12)
        runPuzzle6x12();

        // Puzzle de validation 6x6 (lignes A-F, colonnes 1-6)
        // runValidation6x6();

        // Ou décommenter pour lancer le vrai puzzle Eternity II 16x16
        // runEternityII();

        // Autres exemples disponibles :
        // compareWithAndWithoutSingletons();  // 5x5
        // runExample4x4();
        // runExample4x4Easy();
        // runExample4x4Ordered();
    }

    /**
     * Compare les performances avec et sans l'optimisation singleton.
     */
    private static void compareWithAndWithoutSingletons() {
        int rows = 5, cols = 5;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample5x5();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   COMPARAISON 5x5 : AVEC vs SANS optimisation SINGLETON ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // ===== AVEC SINGLETONS =====
        System.out.println("\n█████████████████████████████████████████████████████████");
        System.out.println("█  TEST 1 : AVEC optimisation SINGLETON                 █");
        System.out.println("█████████████████████████████████████████████████████████\n");

        Board board1 = new Board(rows, cols);
        EternitySolver solver1 = new EternitySolver();
        solver1.setUseSingletons(true);

        boolean solved1 = solver1.solve(board1, pieces);
        EternitySolver.Statistics stats1 = solver1.getStatistics();

        if (!solved1) {
            System.out.println("⚠ Aucune solution trouvée (avec singletons)");
        }

        // ===== SANS SINGLETONS =====
        System.out.println("\n\n█████████████████████████████████████████████████████████");
        System.out.println("█  TEST 2 : SANS optimisation SINGLETON (MRV seul)      █");
        System.out.println("█████████████████████████████████████████████████████████\n");

        Board board2 = new Board(rows, cols);
        EternitySolver solver2 = new EternitySolver();
        solver2.setUseSingletons(false);

        boolean solved2 = solver2.solve(board2, pieces);
        EternitySolver.Statistics stats2 = solver2.getStatistics();

        if (!solved2) {
            System.out.println("⚠ Aucune solution trouvée (sans singletons)");
        }

        // ===== COMPARAISON =====
        System.out.println("\n\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   RÉSUMÉ COMPARATIF                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        System.out.println("┌──────────────────────────┬─────────────────┬─────────────────┬─────────────┐");
        System.out.println("│ Métrique                 │ AVEC Singleton  │ SANS Singleton  │ Gain        │");
        System.out.println("├──────────────────────────┼─────────────────┼─────────────────┼─────────────┤");

        printComparisonRow("Temps (secondes)",
            stats1.getElapsedTimeSec(), stats2.getElapsedTimeSec());
        printComparisonRow("Appels récursifs",
            stats1.recursiveCalls, stats2.recursiveCalls);
        printComparisonRow("Placements testés",
            stats1.placements, stats2.placements);
        printComparisonRow("Backtracks",
            stats1.backtracks, stats2.backtracks);
        printComparisonRow("Vérifications fit()",
            stats1.fitChecks, stats2.fitChecks);

        System.out.println("└──────────────────────────┴─────────────────┴─────────────────┴─────────────┘\n");

        System.out.println("Singletons détectés (test 1) : " + stats1.singletonsFound);
        System.out.println("Singletons posés (test 1)    : " + stats1.singletonsPlaced);
        System.out.println("\nDead-ends (test 1) : " + stats1.deadEndsDetected);
        System.out.println("Dead-ends (test 2) : " + stats2.deadEndsDetected);
    }

    /**
     * Affiche une ligne de comparaison dans le tableau.
     */
    private static void printComparisonRow(String label, double val1, double val2) {
        double gain = val2 > 0 ? ((val2 - val1) / val2) * 100 : 0;
        String gainStr = String.format("%.1f%%", gain);
        if (gain > 0) {
            gainStr = "↓ " + gainStr;
        } else if (gain < 0) {
            gainStr = "↑ " + String.format("%.1f%%", Math.abs(gain));
        } else {
            gainStr = "=";
        }

        System.out.printf("│ %-24s │ %15.2f │ %15.2f │ %11s │%n",
            label, val1, val2, gainStr);
    }

    /**
     * Affiche une ligne de comparaison dans le tableau (version int).
     */
    private static void printComparisonRow(String label, int val1, int val2) {
        printComparisonRow(label, (double)val1, (double)val2);
    }

    /**
     * Exécute le solveur sur l'exemple 4x4 prédéfini (version difficile).
     */
    private static void runExample4x4() {
        int rows = 4, cols = 4;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Lancement du solveur pour exemple 4x4 (DIFFICILE)...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("Aucune solution trouvée pour l'exemple 4x4.");
        } else {
            System.out.println("Solution trouvée :\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Exécute le solveur sur l'exemple 4x4 (version facile).
     */
    private static void runExample4x4Easy() {
        int rows = 4, cols = 4;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Lancement du solveur pour exemple 4x4 (FACILE)...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("Aucune solution trouvée pour l'exemple 4x4.");
        } else {
            System.out.println("Solution trouvée :\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Exécute le solveur sur l'exemple 4x4 (version ordonnée/triviale).
     */
    private static void runExample4x4Ordered() {
        int rows = 4, cols = 4;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Ordered();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Lancement du solveur pour exemple 4x4 (TRIVIALE - ordonnée)...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("Aucune solution trouvée pour l'exemple 4x4.");
        } else {
            System.out.println("Solution trouvée :\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Exécute le solveur sur l'exemple 3x3 prédéfini.
     */
    private static void runExample3x3() {
        int rows = 3, cols = 3;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Lancement du solveur pour exemple 3x3...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("Aucune solution trouvée pour l'exemple 3x3.");
        } else {
            System.out.println("Solution trouvée :\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Exécute le solveur sur le vrai puzzle Eternity II (16x16, 256 pièces).
     */
    private static void runEternityII() {
        int rows = 16, cols = 16;

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              ETERNITY II - PUZZLE 16x16                  ║");
        System.out.println("║                  256 pièces - 22 motifs                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        System.out.println("Chargement des 256 pièces officielles...");
        Map<Integer, Piece> allPieces = PuzzleFactory.createEternityII();
        System.out.println("✓ " + allPieces.size() + " pièces chargées\n");

        Board board;
        Map<Integer, Piece> pieces;

        // Vérifier s'il existe une sauvegarde
        if (SaveManager.hasSavedState()) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              SAUVEGARDE DÉTECTÉE                         ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            Object[] savedState = SaveManager.loadBestState(allPieces);
            if (savedState != null) {
                board = (Board) savedState[0];
                @SuppressWarnings("unchecked")
                Set<Integer> usedPieceIds = (Set<Integer>) savedState[1];
                int savedDepth = (int) savedState[2];
                int savedThread = (int) savedState[3];

                // Créer la map des pièces restantes
                pieces = new java.util.HashMap<>(allPieces);
                for (int usedId : usedPieceIds) {
                    pieces.remove(usedId);
                }

                System.out.println("✓ État restauré: " + savedDepth + " pièces placées");
                System.out.println("  Pièces restantes: " + pieces.size() + "\n");
            } else {
                System.out.println("✗ Erreur lors du chargement - démarrage nouveau\n");
                board = new Board(rows, cols);
                pieces = new java.util.HashMap<>(allPieces);

                // Pré-placer la pièce d'indice obligatoire (piece 139)
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║           PRÉ-PLACEMENT DE LA PIÈCE D'INDICE            ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝\n");

                int hintPieceId = 139;
                Piece hintPiece = pieces.get(hintPieceId);
                if (hintPiece != null) {
                    board.place(8, 7, hintPiece, 3);
                    pieces.remove(hintPieceId);
                    System.out.println("✓ Pièce " + hintPieceId + " placée à (8, 7) avec rotation 3");
                    System.out.println("  Pièces restantes: " + pieces.size() + "\n");
                }
            }
        } else {
            board = new Board(rows, cols);
            pieces = new java.util.HashMap<>(allPieces);

            // Pré-placer la pièce d'indice obligatoire (piece 139)
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           PRÉ-PLACEMENT DE LA PIÈCE D'INDICE            ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            int hintPieceId = 139;
            int hintRow = 8;
            int hintCol = 7;
            int hintRotation = 3;

            Piece hintPiece = pieces.get(hintPieceId);
            if (hintPiece == null) {
                System.out.println("✗ ERREUR: Pièce d'indice " + hintPieceId + " introuvable!");
                return;
            }

            board.place(hintRow, hintCol, hintPiece, hintRotation);
            pieces.remove(hintPieceId);

            System.out.println("✓ Pièce " + hintPieceId + " placée à (" + hintRow + ", " + hintCol + ") avec rotation " + hintRotation);
            System.out.println("  Edges: " + java.util.Arrays.toString(hintPiece.getEdges()));
            System.out.println("  Pièces restantes: " + pieces.size() + "\n");
        }

        EternitySolver solver = new EternitySolver();
        solver.setUseSingletons(true);
        solver.setVerbose(false); // Désactiver l'affichage détaillé

        // Déterminer le nombre de threads (utiliser 75% des cœurs disponibles)
        int numCores = Runtime.getRuntime().availableProcessors();
        int numThreads = Math.max(4, (int)(numCores * 0.75));

        System.out.println("Lancement du solveur avec optimisations...");
        System.out.println("- Heuristique MRV (Minimum Remaining Values)");
        System.out.println("- Détection de singletons (forced moves)");
        System.out.println("- Détection de dead-ends");
        System.out.println("- Randomisation anti-thrashing");
        System.out.println("- Recherche parallèle (" + numThreads + " threads sur " + numCores + " cœurs)");
        System.out.println("- Pièce d'indice obligatoire pré-placée\n");

        System.out.println("⚠ ATTENTION: Ce puzzle n'a jamais été résolu!");
        System.out.println("Le solveur va explorer l'espace de recherche...\n");

        // Utiliser la recherche parallèle
        // IMPORTANT: passer allPieces (toutes les 256 pièces) pour pouvoir reconstruire le board
        // et pieces (pièces restantes) pour la recherche
        boolean solved = solver.solveParallel(board, allPieces, pieces, numThreads);

        if (!solved) {
            System.out.println("\n⚠ Aucune solution trouvée (ou timeout)");
        } else {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🎉 SOLUTION TROUVÉE! 🎉");
            System.out.println("=".repeat(60) + "\n");
            board.prettyPrint(pieces);
        }

        // Afficher les statistiques finales
        solver.getStatistics().print();
    }

    /**
     * Lance le puzzle 16x16.
     * Lignes: A-P (0-15)
     * Colonnes: 1-16 (0-15)
     */
    private static void runPuzzle16x16() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║             PUZZLE 16×16 (256 pièces)                  ║");
        System.out.println("║        Lignes: A-P / Colonnes: 1-16                    ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Charger les pièces
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle16x16();
        System.out.println("✓ " + pieces.size() + " pièces chargées\n");

        // Afficher les pièces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PIÈCES DU PUZZLE");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%3d | %2d %2d %2d %2d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Créer le board
        Board board = new Board(16, 16);

        // TEST: Désactiver les indices pour tester si le solver fonctionne avec les nouvelles données
        // Format: ligne (0-indexed), colonne (0-indexed), pièce, rotation
        // Source: https://github.com/TheSil/edge_puzzle/blob/main/data/eternity2/eternity2_256_all_hints.csv
        int[][] clues = {
            // {8, 7, 139, 3},   // I8
            // {13, 2, 181, 0},  // N3
            // {2, 13, 255, 0},  // C14
            // {2, 2, 208, 0},   // C3
            // {13, 13, 249, 1}  // N14
        };

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PLACEMENT DES INDICES");
        System.out.println("═══════════════════════════════════════════════════════");

        Set<Integer> usedPieceIds = new HashSet<>();
        for (int[] clue : clues) {
            int row = clue[0];    // CSV est déjà 0-indexed
            int col = clue[1];    // CSV est déjà 0-indexed
            int pieceId = clue[2];
            int rotation = clue[3];

            Piece piece = pieces.get(pieceId);
            if (piece != null) {
                board.place(row, col, piece, rotation);
                usedPieceIds.add(pieceId);
                char rowLabel = (char) ('A' + row);
                System.out.printf("  ✓ Pièce %d (rotation %d) placée en %c%d%n",
                    pieceId, rotation, rowLabel, col + 1);
            }
        }
        System.out.println();
        System.out.printf("  → %d pièces pré-placées, %d pièces restantes à placer%n%n",
            usedPieceIds.size(), pieces.size() - usedPieceIds.size());

        // Afficher le board avec les indices placés
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("BOARD AVEC INDICES (16×16)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Afficher le score maximum
        int[] score = board.calculateScore();
        System.out.println("Score maximum théorique:");
        System.out.println("  - Arêtes internes horizontales: " + ((16-1) * 16) + " (15 × 16)");
        System.out.println("  - Arêtes internes verticales: " + (16 * (16-1)) + " (16 × 15)");
        System.out.println("  - Total: " + score[1] + " arêtes internes");
        System.out.println();

        // Créer une copie de la map des pièces sans les pièces pré-placées
        Map<Integer, Piece> remainingPieces = new HashMap<>(pieces);
        for (int usedId : usedPieceIds) {
            remainingPieces.remove(usedId);
        }

        // Lancer le solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("LANCEMENT DU SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");
        System.out.println("Note: Le solver peut prendre plusieurs heures/jours");
        System.out.println("pour un puzzle 16×16 avec 251 pièces à placer.\n");

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false); // Désactiver verbose, afficher seulement les records
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, remainingPieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ Aucune solution trouvée");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");
        }
    }

    /**
     * Lance le puzzle 6x12.
     * Lignes: A-F (0-5)
     * Colonnes: 1-12 (0-11)
     */
    private static void runPuzzle6x12() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              PUZZLE 6×12 (72 pièces)                   ║");
        System.out.println("║        Lignes: A-F / Colonnes: 1-12                    ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Charger les pièces
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle6x12();
        System.out.println("✓ " + pieces.size() + " pièces chargées\n");

        // Afficher les pièces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PIÈCES DU PUZZLE");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%2d  | %2d %2d %2d %2d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Créer le board
        Board board = new Board(6, 12);

        // Afficher le board vide avec coordonnées A-F et 1-12
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("BOARD VIDE (6×12)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Afficher le score maximum
        int[] score = board.calculateScore();
        System.out.println("Score maximum théorique:");
        System.out.println("  - Arêtes internes horizontales: " + ((6-1) * 12) + " (5 × 12)");
        System.out.println("  - Arêtes internes verticales: " + (6 * (12-1)) + " (6 × 11)");
        System.out.println("  - Total: " + score[1] + " arêtes internes");
        System.out.println();

        // Lancer le solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("LANCEMENT DU SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false); // N'afficher que les records, pas chaque placement
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, pieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ Aucune solution trouvée");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");
        }
    }

    /**
     * Lance le puzzle de validation 6x6.
     * Lignes: A-F (0-5)
     * Colonnes: 1-6 (0-5)
     */
    private static void runValidation6x6() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        TEST DE VALIDATION - PUZZLE 6×6                 ║");
        System.out.println("║        Lignes: A-F / Colonnes: 1-6                     ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Charger les pièces
        Map<Integer, Piece> pieces = PuzzleFactory.createValidation6x6();
        System.out.println("✓ " + pieces.size() + " pièces chargées\n");

        // Afficher les pièces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PIÈCES DU PUZZLE");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%2d  | %d  %d  %d  %d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Créer le board
        Board board = new Board(6, 6);

        // Afficher le board vide avec coordonnées A-F et 1-6
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("BOARD VIDE (6×6)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Afficher le score maximum
        int[] score = board.calculateScore();
        System.out.println("Score maximum théorique:");
        System.out.println("  - Arêtes internes horizontales: " + ((6-1) * 6) + " (5 × 6)");
        System.out.println("  - Arêtes internes verticales: " + (6 * (6-1)) + " (6 × 5)");
        System.out.println("  - Total: " + score[1] + " arêtes internes");
        System.out.println();

        // Lancer le solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("LANCEMENT DU SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false); // N'afficher que les records, pas chaque placement
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, pieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ Aucune solution trouvée");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");
        }
    }

    /**
     * Affiche le board avec les coordonnées A-F (lignes) et 1-12 (colonnes).
     * Chaque case affiche le numéro de pièce au centre avec les valeurs d'arêtes autour.
     */
    private static void printBoardWithCoordinates(Board board, Map<Integer, Piece> pieces) {
        int rows = board.getRows();
        int cols = board.getCols();

        // En-tête avec numéros de colonnes (alignés à droite sur 2 caractères)
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d    ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();

        // Ligne supérieure
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);

            // Ligne 1: Arête Nord
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print("        ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    System.out.printf("   %2d   ", edges[0]); // Nord
                }
                System.out.print("│");
            }
            System.out.println();

            // Ligne 2: Ouest + ID pièce + Est
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print("   --   ");
                } else {
                    int pieceId = board.getPlacement(r, c).getPieceId();
                    int[] edges = board.getPlacement(r, c).edges;
                    System.out.printf("%2d %2d %2d", edges[3], pieceId, edges[1]); // Ouest, ID, Est
                }
                System.out.print("│");
            }
            System.out.println();

            // Ligne 3: Arête Sud
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print("        ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    System.out.printf("   %2d   ", edges[2]); // Sud
                }
                System.out.print("│");
            }
            System.out.println();

            // Séparateur entre lignes
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                System.out.println();
            }
        }

        // Ligne inférieure
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println("\n");
    }
}
