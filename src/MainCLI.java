import cli.CommandLineInterface;
import model.Board;
import model.Piece;
import runner.PuzzleRunner;
import runner.PuzzleRunner.PuzzleRunnerConfig;
import runner.PuzzleRunner.PuzzleResult;
import util.PuzzleFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Point d'entrée principal avec interface CLI professionnelle.
 * Remplace Main.java avec support complet des arguments.
 */
public class MainCLI {

    private static final Logger logger = LoggerFactory.getLogger(MainCLI.class);

    public static void main(String[] args) {
        CommandLineInterface cli = new CommandLineInterface();

        // Parser les arguments
        if (!cli.parse(args)) {
            System.err.println("Erreur: " + cli.getErrorMessage());
            System.err.println();
            cli.printHelp();
            System.exit(1);
        }

        // Afficher l'aide si demandé
        if (cli.isHelpRequested()) {
            cli.printHelp();
            System.exit(0);
        }

        // Afficher la version si demandé
        if (cli.isVersionRequested()) {
            cli.printVersion();
            System.exit(0);
        }

        // Vérifier qu'un puzzle a été spécifié
        String puzzleName = cli.getPuzzleName();
        if (puzzleName == null) {
            System.err.println("Erreur: Aucun puzzle spécifié");
            System.err.println();
            cli.printHelp();
            System.exit(1);
        }

        // Logger la configuration
        if (!cli.isQuiet()) {
            cli.printConfiguration();
        }

        // Créer le puzzle selon le nom
        PuzzleInfo puzzleInfo = createPuzzle(puzzleName);
        if (puzzleInfo == null) {
            System.err.println("Erreur: Puzzle inconnu: " + puzzleName);
            System.err.println();
            System.err.println("Puzzles disponibles:");
            System.err.println("  - puzzle_6x12, puzzle_16x16, validation_6x6");
            System.err.println("  - example_3x3, example_4x4, example_4x4_easy");
            System.err.println("  - eternity2_p* (voir data/eternity2/)");
            System.exit(1);
        }

        // Afficher l'en-tête
        if (!cli.isQuiet()) {
            printHeader(puzzleInfo.name, puzzleInfo.board.getRows(), puzzleInfo.board.getCols(),
                       puzzleInfo.pieces.size());
        }

        // Configurer le runner
        PuzzleRunnerConfig config = new PuzzleRunnerConfig()
            .setVerbose(cli.isVerbose() && !cli.isQuiet())
            .setParallel(cli.isParallel())
            .setUseSingletons(cli.useSingletons())
            .setMinDepth(cli.getMinDepth() != null ? cli.getMinDepth() : 0);

        if (cli.getThreads() != null) {
            config.setThreads(cli.getThreads());
        }
        if (cli.getTimeout() != null) {
            config.setTimeoutSeconds(cli.getTimeout());
        }

        // Créer et lancer le runner
        PuzzleRunner runner = new PuzzleRunner(puzzleInfo.board, puzzleInfo.pieces, config);
        PuzzleResult result = runner.run();

        // Afficher le résultat
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (result.isSolved()) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", result.getDurationSeconds()) + " secondes");
            System.out.println();

            if (!cli.isQuiet()) {
                result.getBoard().prettyPrint(puzzleInfo.pieces);
                result.getBoard().printScore();
            }
            System.exit(0);
        } else {
            System.out.println("✗ Aucune solution trouvée");
            System.out.println("Temps: " + String.format("%.2f", result.getDurationSeconds()) + " secondes");
            System.exit(1);
        }
    }

    /**
     * Affiche l'en-tête du programme
     */
    private static void printHeader(String puzzleName, int rows, int cols, int numPieces) {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║          ETERNITY PUZZLE SOLVER                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Puzzle: " + puzzleName);
        System.out.println("Taille: " + rows + "×" + cols + " (" + numPieces + " pièces)");
        System.out.println();
    }

    /**
     * Crée le puzzle selon le nom
     */
    private static PuzzleInfo createPuzzle(String name) {
        switch (name) {
            case "puzzle_6x12":
            case "6x12":
                return new PuzzleInfo("Puzzle 6×12", new Board(6, 12),
                                     PuzzleFactory.createPuzzle6x12());

            case "puzzle_16x16":
            case "16x16":
                return new PuzzleInfo("Puzzle 16×16", new Board(16, 16),
                                     PuzzleFactory.createPuzzle16x16());

            case "validation_6x6":
            case "6x6":
                return new PuzzleInfo("Validation 6×6", new Board(6, 6),
                                     PuzzleFactory.createValidation6x6());

            case "example_3x3":
            case "3x3":
                return new PuzzleInfo("Exemple 3×3", new Board(3, 3),
                                     PuzzleFactory.createExample3x3());

            case "example_4x4":
            case "4x4":
                return new PuzzleInfo("Exemple 4×4", new Board(4, 4),
                                     PuzzleFactory.createExample4x4());

            case "example_4x4_easy":
            case "4x4_easy":
                return new PuzzleInfo("Exemple 4×4 (facile)", new Board(4, 4),
                                     PuzzleFactory.createExample4x4Easy());

            case "example_4x4_ordered":
            case "4x4_ordered":
                return new PuzzleInfo("Exemple 4×4 (ordonné)", new Board(4, 4),
                                     PuzzleFactory.createExample4x4Ordered());

            case "example_5x5":
            case "5x5":
                return new PuzzleInfo("Exemple 5×5", new Board(5, 5),
                                     PuzzleFactory.createExample5x5());

            case "eternity2":
            case "eternity_ii":
                return new PuzzleInfo("Eternity II 16×16", new Board(16, 16),
                                     PuzzleFactory.createEternityII());

            default:
                // Vérifier si c'est une config eternity2_p*
                if (name.startsWith("eternity2_p") || name.startsWith("indice_")) {
                    logger.warn("Configuration {} nécessite PuzzleConfig.loadFromFile()", name);
                }
                return null;
        }
    }

    /**
     * Classe interne pour stocker les informations d'un puzzle
     */
    private static class PuzzleInfo {
        final String name;
        final Board board;
        final Map<Integer, Piece> pieces;

        PuzzleInfo(String name, Board board, Map<Integer, Piece> pieces) {
            this.name = name;
            this.board = board;
            this.pieces = pieces;
        }
    }
}
