import model.Board;
import model.Piece;
import model.Placement;
import solver.EternitySolver;
import util.ConfigurationUtils;
import util.FormattingUtils;
import util.SaveStateManager;
import java.io.*;
import java.util.*;

/**
 * Résolveur séquentiel de tous les puzzles Eternity II
 * Résout dans l'ordre: Online → Indices 1-4 → Eternity 2
 */
public class MainSequential {

    private static final String DATA_DIR = "data/";

    // Removed: extractConfigId() - now using ConfigurationUtils.extractConfigId()

    // Liste des puzzles dans l'ordre de résolution
    private static final String[] PUZZLE_FILES = {
        "online/online.txt",
        "indice1/indice1.txt",
        "indice2/indice2.txt",
        "indice3/indice3.txt",
        "indice4/indice4.txt",
        "eternity2/eternity2.txt"  // 256 pièces
    };

    // Timeout pour chaque puzzle (10 minutes)
    private static final long PUZZLE_TIMEOUT = 600000; // 10 minutes en millisecondes

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          ETERNITY II - RÉSOLVEUR SÉQUENTIEL DE PUZZLES           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        List<PuzzleResult> results = new ArrayList<>();
        long totalStartTime = System.currentTimeMillis();

        // Résoudre chaque puzzle dans l'ordre (avec rotation toutes les 10 minutes)
        while (true) {
            for (String puzzleFile : PUZZLE_FILES) {
                String filepath = DATA_DIR + puzzleFile;

                try {
                    // Charger le puzzle
                    PuzzleConfig config = PuzzleConfig.loadFromFile(filepath);

                    if (config == null) {
                        System.out.println("✗ Impossible de charger: " + puzzleFile);
                        System.out.println();
                        continue;
                    }

                    // Afficher les informations
                    config.printInfo();
                    System.out.println();

                    // Résoudre le puzzle avec timeout de 10 minutes
                    long startTime = System.currentTimeMillis();
                    boolean solved = solvePuzzleWithTimeout(config, filepath, PUZZLE_TIMEOUT);
                    long duration = System.currentTimeMillis() - startTime;

                    // Enregistrer le résultat
                    results.add(new PuzzleResult(config.getName(), config.getType(),
                                                 config.getPieces().size(), solved, duration));

                    // Afficher le résumé
                    config.printSummary(duration, solved);
                    System.out.println();

                    // Si résolu, on peut passer au suivant
                    if (solved) {
                        System.out.println("  → ✓ Puzzle résolu, passage au suivant...");
                        System.out.println();
                    } else {
                        System.out.println("  → ⏱ Timeout de 10 minutes atteint, passage au puzzle suivant...");
                        System.out.println();
                    }

                } catch (IOException e) {
                    System.out.println("✗ Erreur lors du chargement de " + puzzleFile + ": " + e.getMessage());
                    System.out.println();
                }
            }

            // Une fois tous les puzzles parcourus, recommencer depuis le début
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("  Cycle complet terminé, recommencement depuis le premier puzzle...");
            System.out.println("═══════════════════════════════════════════════════════════════════\n");
        }

        // Note: Cette section n'est jamais atteinte car la boucle est infinie
        // Le programme tourne en continu et change de puzzle toutes les 10 minutes
    }

    /**
     * Résout un puzzle donné
     * Charge la sauvegarde current si disponible, sinon démarre depuis le début
     * Le backtracking se fait en mémoire par le solver
     */
    private static boolean solvePuzzle(PuzzleConfig config, String filepath) {
        try {
            // Vérifier s'il existe une sauvegarde "current"
            File currentSave = SaveStateManager.findCurrentSave(config.getType());

            if (currentSave != null) {
                System.out.println("  → 📂 Sauvegarde current trouvée");
                System.out.println("  → Reprise de la résolution depuis l'état sauvegardé...");

                // Charger la sauvegarde
                SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(currentSave, config.getType());
                if (saveState == null) {
                    System.out.println("  → ⚠️  Erreur de chargement, démarrage depuis le début...");
                    return solvePuzzleFromScratch(config, filepath);
                }

                System.out.println("  → État sauvegardé: " + saveState.depth + " pièces placées");

                // Vérifier si le puzzle est déjà complètement résolu
                int totalPieces = config.getRows() * config.getCols();
                if (saveState.depth == totalPieces) {
                    System.out.println("  → ✅ Puzzle déjà résolu! (" + totalPieces + "/" + totalPieces + " pièces)");

                    // Créer un board pour afficher la solution
                    Board board = new Board(config.getRows(), config.getCols());
                    Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                    boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
                    if (restored) {
                        System.out.println();
                        if (totalPieces <= 72) {
                            // Affichage détaillé pour les petits puzzles
                            displayDetailedSolution(board, allPieces);
                        } else {
                            // Affichage simple pour les grands puzzles
                            displaySolution(board);
                        }
                    }

                    return true;
                }

                // NOUVEAU: Backtracking complet avec historique
                // On ne retire AUCUNE pièce, on passe l'historique complet au solver
                // Le solver pourra backtracker à travers TOUTES les pièces pré-chargées
                System.out.println("  → Reprise depuis: " + saveState.depth + " pièces (TOUTES les pièces peuvent être backtractées)");

                // Créer un nouveau board et restaurer l'état complet
                Board board = new Board(config.getRows(), config.getCols());
                Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
                if (!restored) {
                    System.out.println("  → ⚠️  Erreur de restauration, démarrage depuis le début...");
                    return solvePuzzleFromScratch(config, filepath);
                }

                // Préparer les pièces non utilisées
                List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);

                // Trier selon l'ordre configuré (ascending/descending)
                ConfigurationUtils.sortPiecesByOrder(unusedIds, config.getSortOrder());

                System.out.println("  → " + unusedIds.size() + " pièces restantes à placer");
                System.out.println("  → Ordre de tri: " + config.getSortOrder());

                // Vérifier les meilleurs scores disponibles
                List<File> bestSaves = SaveStateManager.findAllSaves(config.getType());
                if (!bestSaves.isEmpty()) {
                    System.out.println("  → 📊 " + bestSaves.size() + " meilleur(s) score(s) sauvegardé(s)");

                    // Afficher la meilleure solution trouvée jusqu'à présent
                    File bestSave = bestSaves.get(0); // Le premier est le meilleur (tri par profondeur)
                    SaveStateManager.SaveState bestState = SaveStateManager.loadStateFromFile(bestSave, config.getType());

                    if (bestState != null) {
                        System.out.println("  → 🏆 Meilleure solution atteinte: " + bestState.depth + " pièces");
                        System.out.println();

                        // Créer un board pour afficher la meilleure solution
                        Board bestBoard = new Board(config.getRows(), config.getCols());
                        Map<Integer, Piece> bestPieces = new HashMap<>(config.getPieces());

                        boolean bestRestored = SaveStateManager.restoreState(bestState, bestBoard, bestPieces);
                        if (bestRestored) {
                            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
                            System.out.println("║              MEILLEURE SOLUTION ATTEINTE (RECORD)                ║");
                            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
                            System.out.println();
                            System.out.println("État avec le plus de pièces placées jusqu'à présent:");
                            System.out.println();
                            System.out.println("Légende des couleurs (comparaison RECORD vs CURRENT actuel):");
                            System.out.println("  - \033[1;35mMagenta\033[0m: Case occupée dans RECORD mais vide dans CURRENT (régression)");
                            System.out.println("  - \033[1;38;5;208mOrange\033[0m: Pièce différente entre RECORD et CURRENT (changement)");
                            System.out.println("  - \033[1;33mJaune\033[0m: Case vide dans RECORD mais occupée dans CURRENT (progression)");
                            System.out.println("  - \033[1;36mCyan\033[0m: Case identique dans RECORD et CURRENT (stabilité)");
                            System.out.println();

                            // Créer un solver temporaire pour l'affichage avec comparaison
                            EternitySolver tempSolver = new EternitySolver();
                            List<Integer> bestUnusedIds = new ArrayList<>(bestState.unusedPieceIds);

                            // Utiliser la comparaison pour montrer les différences avec le current
                            tempSolver.printBoardWithComparison(bestBoard, board, bestPieces, bestUnusedIds);
                            System.out.println();

                            bestBoard.printScore();
                            System.out.println();
                            System.out.println("═".repeat(70));
                            System.out.println();
                        }
                    }
                }

                // Créer et configurer le solveur
                EternitySolver.resetGlobalState();
                EternitySolver solver = new EternitySolver();
                solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());
                String configId = ConfigurationUtils.extractConfigId(filepath);
                solver.setPuzzleName(configId);
                solver.setSortOrder(config.getSortOrder());

                System.out.println("  → Le backtracking pourra remonter à travers TOUTES les " + saveState.depth + " pièces pré-chargées");
                System.out.println();

                // Afficher l'état complet du puzzle chargé pour validation
                System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
                System.out.println("║              ÉTAT DU PUZZLE CHARGÉ (VALIDATION)                  ║");
                System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
                System.out.println();
                System.out.println("Légende:");
                System.out.println("  - Pièces placées: ID de la pièce avec valeurs d'arêtes (N/E/S/W)");
                System.out.println("  - Cases vides: (XXX) = nombre de pièces valides possibles");
                System.out.println("  - \033[93mJaune\033[0m: cases critiques (≤20 possibilités)");
                System.out.println("  - \033[1;91mRouge\033[0m: dead-end (0 possibilités)");
                System.out.println();
                solver.printBoardWithLabels(board, allPieces, unusedIds);
                System.out.println();

                // Afficher le score actuel
                board.printScore();
                System.out.println();
                System.out.println("═".repeat(70));
                System.out.println();

                // Résoudre avec backtracking complet (nouvelle méthode avec historique)
                boolean solved = solver.solveWithHistory(board, allPieces, unusedIds,
                                                         new ArrayList<>(saveState.placementOrder));

                if (solved) {
                    System.out.println("\n  → ✅ Solution trouvée!");
                    System.out.println();
                    if (totalPieces <= 72) {
                        // Affichage détaillé pour les petits puzzles
                        displayDetailedSolution(board, allPieces);
                    } else {
                        // Affichage simple pour les grands puzzles
                        displaySolution(board);
                    }
                } else {
                    System.out.println("  → ✗ Pas de solution trouvée");
                }

                return solved;
            }

            // Pas de sauvegarde current - démarrage classique
            System.out.println("  → Aucune sauvegarde current trouvée");
            return solvePuzzleFromScratch(config, filepath);

        } catch (Exception e) {
            System.out.println("  → ✗ Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Résout un puzzle avec un timeout (pour rotation automatique des puzzles)
     */
    private static boolean solvePuzzleWithTimeout(PuzzleConfig config, String filepath, long timeout) {
        final boolean[] solved = {false};
        final Exception[] exception = {null};

        Thread solverThread = new Thread(() -> {
            try {
                solved[0] = solvePuzzle(config, filepath);
            } catch (Exception e) {
                exception[0] = e;
            }
        });

        solverThread.start();

        try {
            solverThread.join(timeout);

            if (solverThread.isAlive()) {
                // Timeout atteint - interrompre le thread
                System.out.println("\n  → ⏱ Timeout de 10 minutes atteint");
                System.out.println("  → Interruption de la résolution et sauvegarde de l'état...");
                solverThread.interrupt();
                solverThread.join(5000); // Attendre 5 secondes pour que le thread se termine proprement

                if (solverThread.isAlive()) {
                    // Force kill si le thread ne se termine pas
                    System.out.println("  → Arrêt forcé du thread de résolution");
                }

                return false; // Pas résolu dans le temps imparti
            }

            if (exception[0] != null) {
                System.out.println("  → ✗ Erreur: " + exception[0].getMessage());
                exception[0].printStackTrace();
                return false;
            }

            return solved[0];

        } catch (InterruptedException e) {
            System.out.println("  → Thread principal interrompu");
            solverThread.interrupt();
            return false;
        }
    }

    /**
     * Résout un puzzle depuis le début (sans sauvegarde)
     */
    private static boolean solvePuzzleFromScratch(PuzzleConfig config, String filepath) {
        try {
            System.out.println("  → Démarrage depuis le début...");

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            // Placer les pièces fixes
            for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                Piece piece = config.getPieces().get(fp.pieceId);
                if (piece != null) {
                    board.place(fp.row, fp.col, piece, fp.rotation);
                    config.getPieces().remove(fp.pieceId);
                    System.out.println("  → Pièce fixe " + fp.pieceId + " placée à [" + fp.row + "," + fp.col + "] rotation " + fp.rotation);
                }
            }

            // Résoudre
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());
            String configId = ConfigurationUtils.extractConfigId(filepath);
            solver.setPuzzleName(configId);
            solver.setSortOrder(config.getSortOrder());

            System.out.println("  → Résolution en cours...");
            System.out.println("  → Ordre de tri: " + config.getSortOrder());
            System.out.println("  → Sauvegarde automatique toutes les 1 minute");
            System.out.println("  → Changement de puzzle toutes les 10 minutes");

            boolean solved = solver.solve(board, allPieces);

            if (solved) {
                System.out.println("  → ✓ Solution trouvée!");
                System.out.println();
                int totalPieces = config.getRows() * config.getCols();
                if (totalPieces <= 72) {
                    // Affichage détaillé pour les petits puzzles
                    displayDetailedSolution(board, allPieces);
                } else {
                    // Affichage simple pour les grands puzzles
                    displaySolution(board);
                }
            } else {
                System.out.println("  → ✗ Pas de solution trouvée");
            }

            return solved;

        } catch (Exception e) {
            System.out.println("  → ✗ Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtient le timeout en fonction de la difficulté
     */
    private static long getTimeoutForDifficulty(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "facile":
                return 30000;  // 30 secondes
            case "moyen":
                return 120000; // 2 minutes
            case "difficile":
                return 300000; // 5 minutes
            case "extreme":
                return 1800000; // 30 minutes
            default:
                return 60000;   // 1 minute par défaut
        }
    }

    // Removed: sortPiecesByOrder() - now using ConfigurationUtils.sortPiecesByOrder()

    /**
     * Affiche la solution d'un plateau (version simple, rétro-compatible)
     */
    private static void displaySolution(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        System.out.println("  Solution:");
        System.out.println("  ┌" + "─".repeat(cols * 4 + 1) + "┐");

        for (int r = 0; r < rows; r++) {
            System.out.print("  │");
            for (int c = 0; c < cols; c++) {
                Placement p = board.getPlacement(r, c);
                if (p != null) {
                    System.out.print(String.format(" %3d", p.getPieceId()));
                } else {
                    System.out.print("  · ");
                }
            }
            System.out.println(" │");
        }

        System.out.println("  └" + "─".repeat(cols * 4 + 1) + "┘");
    }

    /**
     * Affiche la solution d'un plateau (version détaillée avec arêtes)
     */
    private static void displayDetailedSolution(Board board, Map<Integer, Piece> allPieces) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        SOLUTION TROUVÉE                          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Légende:");
        System.out.println("  - Chaque pièce affiche: ID de la pièce avec valeurs d'arêtes (N/E/S/W)");
        System.out.println("  - \033[32mVert\033[0m: arêtes qui correspondent avec les voisins");
        System.out.println("  - \033[91mRouge\033[0m: arêtes qui ne correspondent PAS (erreur!)");
        System.out.println();

        // Créer un solver temporaire pour utiliser sa méthode d'affichage
        solver.EternitySolver tempSolver = new solver.EternitySolver();
        List<Integer> emptyList = new ArrayList<>();
        tempSolver.printBoardWithLabels(board, allPieces, emptyList);

        System.out.println();
        board.printScore();
        System.out.println();
        System.out.println("═".repeat(70));
    }

    /**
     * Affiche le rapport final
     */
    private static void printFinalReport(List<PuzzleResult> results, long totalDuration) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        RAPPORT FINAL                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        int solved = 0;
        int total = results.size();

        for (PuzzleResult result : results) {
            String status = result.solved ? "✓" : "✗";
            String name = String.format("%-35s", result.name);
            String pieces = String.format("%3d pièces", result.pieceCount);
            String time = String.format("%12s", FormattingUtils.formatDuration(result.duration));

            System.out.println("║ " + status + " " + name + " " + pieces + " " + time + " ║");

            if (result.solved) solved++;
        }

        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Résolus: " + String.format("%-58s", solved + " / " + total) + " ║");
        System.out.println("║ Temps total: " + String.format("%-52s", FormattingUtils.formatDuration(totalDuration)) + " ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // Removed: formatDuration() - now using FormattingUtils.formatDuration()

    /**
     * Classe interne pour stocker les résultats
     */
    private static class PuzzleResult {
        String name;
        String type;
        int pieceCount;
        boolean solved;
        long duration;

        PuzzleResult(String name, String type, int pieceCount, boolean solved, long duration) {
            this.name = name;
            this.type = type;
            this.pieceCount = pieceCount;
            this.solved = solved;
            this.duration = duration;
        }
    }
}
