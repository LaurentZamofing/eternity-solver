package cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface en ligne de commande pour le solveur Eternity.
 * Parse et valide les arguments passés au programme.
 */
public class CommandLineInterface {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineInterface.class);

    private final Map<String, String> options = new HashMap<>();
    private final List<String> arguments = new ArrayList<>();
    private boolean helpRequested = false;
    private String errorMessage = null;

    /**
     * Parse les arguments de la ligne de commande.
     *
     * Formats supportés:
     * - Options courtes: -v, -h
     * - Options longues: --verbose, --help
     * - Options avec valeur: --puzzle <name>, --threads <n>, --timeout <seconds>
     * - Arguments positionnels: puzzle_name
     *
     * @param args arguments de main()
     * @return true si le parsing a réussi, false sinon
     */
    public boolean parse(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if (arg.equals("--help") || arg.equals("-h")) {
                    helpRequested = true;
                    return true;
                }
                else if (arg.equals("--version")) {
                    options.put("version", "true");
                    return true;
                }
                else if (arg.equals("--verbose") || arg.equals("-v")) {
                    options.put("verbose", "true");
                }
                else if (arg.equals("--quiet") || arg.equals("-q")) {
                    options.put("quiet", "true");
                }
                else if (arg.equals("--parallel") || arg.equals("-p")) {
                    options.put("parallel", "true");
                }
                else if (arg.equals("--no-singletons")) {
                    options.put("singletons", "false");
                }
                else if (arg.startsWith("--puzzle=")) {
                    options.put("puzzle", arg.substring("--puzzle=".length()));
                }
                else if (arg.equals("--puzzle")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --puzzle nécessite un argument";
                        return false;
                    }
                    options.put("puzzle", args[++i]);
                }
                else if (arg.startsWith("--threads=")) {
                    String value = arg.substring("--threads=".length());
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --threads nécessite un entier positif: " + value;
                        return false;
                    }
                    options.put("threads", value);
                }
                else if (arg.equals("--threads") || arg.equals("-t")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --threads nécessite un argument";
                        return false;
                    }
                    String value = args[++i];
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --threads nécessite un entier positif: " + value;
                        return false;
                    }
                    options.put("threads", value);
                }
                else if (arg.startsWith("--timeout=")) {
                    String value = arg.substring("--timeout=".length());
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --timeout nécessite un entier positif: " + value;
                        return false;
                    }
                    options.put("timeout", value);
                }
                else if (arg.equals("--timeout")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --timeout nécessite un argument";
                        return false;
                    }
                    String value = args[++i];
                    if (!validatePositiveInteger(value)) {
                        errorMessage = "Option --timeout nécessite un entier positif: " + value;
                        return false;
                    }
                    options.put("timeout", value);
                }
                else if (arg.startsWith("--min-depth=")) {
                    String value = arg.substring("--min-depth=".length());
                    if (!validateNonNegativeInteger(value)) {
                        errorMessage = "Option --min-depth nécessite un entier non-négatif: " + value;
                        return false;
                    }
                    options.put("min-depth", value);
                }
                else if (arg.equals("--min-depth")) {
                    if (i + 1 >= args.length) {
                        errorMessage = "Option --min-depth nécessite un argument";
                        return false;
                    }
                    String value = args[++i];
                    if (!validateNonNegativeInteger(value)) {
                        errorMessage = "Option --min-depth nécessite un entier non-négatif: " + value;
                        return false;
                    }
                    options.put("min-depth", value);
                }
                else if (arg.startsWith("--")) {
                    errorMessage = "Option inconnue: " + arg;
                    return false;
                }
                else if (arg.startsWith("-") && arg.length() > 1 && !arg.equals("-")) {
                    errorMessage = "Option courte inconnue: " + arg;
                    return false;
                }
                else {
                    // Argument positionnel (nom du puzzle)
                    arguments.add(arg);
                }
            }

            return true;

        } catch (Exception e) {
            errorMessage = "Erreur lors du parsing: " + e.getMessage();
            logger.error("Erreur de parsing CLI", e);
            return false;
        }
    }

    /**
     * Valide qu'une chaîne est un entier positif (> 0)
     */
    private boolean validatePositiveInteger(String value) {
        try {
            int n = Integer.parseInt(value);
            return n > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valide qu'une chaîne est un entier non-négatif (>= 0)
     */
    private boolean validateNonNegativeInteger(String value) {
        try {
            int n = Integer.parseInt(value);
            return n >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Affiche le message d'aide
     */
    public void printHelp() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║           ETERNITY PUZZLE SOLVER - AIDE                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -jar eternity-solver.jar [OPTIONS] [PUZZLE_NAME]");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  -h, --help              Affiche ce message d'aide");
        System.out.println("  --version               Affiche la version du programme");
        System.out.println("  -v, --verbose           Active le mode verbeux (détails)");
        System.out.println("  -q, --quiet             Mode silencieux (erreurs uniquement)");
        System.out.println("  -p, --parallel          Active la recherche parallèle");
        System.out.println("  --no-singletons         Désactive l'optimisation singletons");
        System.out.println();
        System.out.println("  --puzzle <name>         Nom du puzzle à résoudre");
        System.out.println("  -t, --threads <n>       Nombre de threads (défaut: auto)");
        System.out.println("  --timeout <seconds>     Timeout en secondes (défaut: illimité)");
        System.out.println("  --min-depth <n>         Profondeur min pour afficher records");
        System.out.println();
        System.out.println("EXEMPLES:");
        System.out.println("  # Résoudre puzzle 6x12 en mode verbeux");
        System.out.println("  java -jar eternity-solver.jar --puzzle puzzle_6x12 --verbose");
        System.out.println();
        System.out.println("  # Recherche parallèle avec 8 threads");
        System.out.println("  java -jar eternity-solver.jar -p --threads 8 eternity2_p1");
        System.out.println();
        System.out.println("  # Mode silencieux avec timeout de 1 heure");
        System.out.println("  java -jar eternity-solver.jar -q --timeout 3600 puzzle_16x16");
        System.out.println();
        System.out.println("PUZZLES DISPONIBLES:");
        System.out.println("  - puzzle_6x12       Puzzle 6×12 (72 pièces)");
        System.out.println("  - puzzle_16x16      Puzzle 16×16 (256 pièces)");
        System.out.println("  - validation_6x6    Puzzle de test 6×6 (36 pièces)");
        System.out.println("  - eternity2_p*      Configurations Eternity II");
        System.out.println("  - example_3x3       Exemple simple 3×3 (9 pièces)");
        System.out.println("  - example_4x4       Exemple 4×4 (16 pièces)");
        System.out.println();
    }

    /**
     * Affiche la version du programme
     */
    public void printVersion() {
        System.out.println("Eternity Puzzle Solver v1.0.0");
        System.out.println("Solveur de puzzle d'edge-matching avec backtracking optimisé");
        System.out.println();
        System.out.println("Fonctionnalités:");
        System.out.println("  - Heuristique MRV (Minimum Remaining Values)");
        System.out.println("  - Détection de singletons (forced moves)");
        System.out.println("  - Propagation de contraintes AC-3");
        System.out.println("  - Recherche parallèle multi-thread");
        System.out.println("  - Sauvegarde/reprise automatique");
        System.out.println();
    }

    // ===== Getters =====

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPuzzleName() {
        // Priorité à l'option --puzzle
        if (options.containsKey("puzzle")) {
            return options.get("puzzle");
        }
        // Sinon premier argument positionnel
        if (!arguments.isEmpty()) {
            return arguments.get(0);
        }
        return null;
    }

    public boolean isVerbose() {
        return options.containsKey("verbose") && "true".equals(options.get("verbose"));
    }

    public boolean isQuiet() {
        return options.containsKey("quiet") && "true".equals(options.get("quiet"));
    }

    public boolean isParallel() {
        return options.containsKey("parallel") && "true".equals(options.get("parallel"));
    }

    public boolean useSingletons() {
        // Par défaut true, false si --no-singletons
        return !"false".equals(options.get("singletons"));
    }

    public Integer getThreads() {
        if (options.containsKey("threads")) {
            try {
                return Integer.parseInt(options.get("threads"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getTimeout() {
        if (options.containsKey("timeout")) {
            try {
                return Integer.parseInt(options.get("timeout"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getMinDepth() {
        if (options.containsKey("min-depth")) {
            try {
                return Integer.parseInt(options.get("min-depth"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public boolean isVersionRequested() {
        return options.containsKey("version") && "true".equals(options.get("version"));
    }

    /**
     * Affiche un résumé de la configuration parsée (pour debug)
     */
    public void printConfiguration() {
        logger.info("Configuration CLI:");
        logger.info("  Puzzle: {}", getPuzzleName());
        logger.info("  Verbose: {}", isVerbose());
        logger.info("  Quiet: {}", isQuiet());
        logger.info("  Parallel: {}", isParallel());
        logger.info("  Singletons: {}", useSingletons());
        logger.info("  Threads: {}", getThreads());
        logger.info("  Timeout: {}s", getTimeout());
        logger.info("  Min depth: {}", getMinDepth());
    }
}
