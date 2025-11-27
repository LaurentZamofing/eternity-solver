package solver;

/**
 * Constantes centralisées pour les paramètres du solveur Eternity.
 *
 * Cette classe regroupe toutes les valeurs de configuration et seuils
 * utilisés par les différents composants du solveur, permettant une
 * modification facile et une meilleure documentation des paramètres.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class SolverConstants {

    // Empêche l'instanciation
    private SolverConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ═══════════════════════════════════════════════════════════════
    // PARAMÈTRES ALGORITHMIQUES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Probabilité de randomisation de l'ordre des candidats MRV.
     * Valeur entre 0.0 (aucune randomisation) et 1.0 (toujours randomiser).
     *
     * Valeur par défaut: 0.3 (30% de chance de mélanger)
     * Permet d'explorer différents chemins de recherche.
     */
    public static final double DEFAULT_RANDOMIZATION_PROBABILITY = 0.3;

    /**
     * Seuil de stagnation avant redémarrage partiel.
     * Nombre de backtracks sans progrès avant de considérer
     * qu'on est bloqué et tenter une stratégie différente.
     *
     * Valeur par défaut: 50,000 backtracks
     * Ajuster selon la taille du puzzle et la patience désirée.
     */
    public static final int DEFAULT_STAGNATION_THRESHOLD = 50_000;

    /**
     * Profondeur de travail pour le work-stealing dans le solveur parallèle.
     * Nombre de niveaux à descendre avant qu'un thread puisse "voler"
     * du travail à un autre thread.
     *
     * Valeur par défaut: 5 niveaux
     * Voir: ParallelSearchManager.WORK_STEALING_DEPTH_THRESHOLD
     */
    public static final int WORK_STEALING_DEPTH_THRESHOLD = 5;

    // ═══════════════════════════════════════════════════════════════
    // TIMEOUTS ET INTERVALLES (en millisecondes)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Intervalle de sauvegarde automatique pour les threads.
     * Fréquence à laquelle un thread sauvegarde son état.
     *
     * Valeur: 60,000 ms = 1 minute
     */
    public static final long THREAD_SAVE_INTERVAL_MS = 60_000;

    /**
     * Timeout par défaut pour l'exécution d'un puzzle.
     * Temps maximum d'exécution avant arrêt automatique.
     *
     * Valeur: 600,000 ms = 10 minutes
     * Voir: MainSequential.PUZZLE_TIMEOUT
     */
    public static final long DEFAULT_PUZZLE_TIMEOUT_MS = 600_000;

    /**
     * Limite d'exécution maximale (pas de timeout).
     * Utilisé quand aucun timeout n'est spécifié.
     *
     * Valeur: Long.MAX_VALUE (essentiellement infini)
     */
    public static final long UNLIMITED_EXECUTION_TIME_MS = Long.MAX_VALUE;

    /**
     * Intervalle de vérification du timeout.
     * Fréquence à laquelle le solveur vérifie s'il doit s'arrêter.
     *
     * Valeur: 100 ms
     */
    public static final long TIMEOUT_CHECK_INTERVAL_MS = 100;

    // ═══════════════════════════════════════════════════════════════
    // GESTION DES SAUVEGARDES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Nombre maximum de sauvegardes backup à conserver.
     * Les plus anciennes sont supprimées automatiquement.
     *
     * Valeur: 10 sauvegardes
     * Voir: SaveStateManager.MAX_BACKUP_SAVES
     */
    public static final int MAX_BACKUP_SAVES = 10;

    /**
     * Intervalle de niveau pour les sauvegardes.
     * Sauvegarde chaque N pièces placées.
     *
     * Valeur: 1 (sauvegarde à chaque nouvelle pièce)
     * Voir: SaveStateManager.SAVE_LEVEL_INTERVAL
     */
    public static final int SAVE_LEVEL_INTERVAL = 1;

    /**
     * Répertoire racine pour les sauvegardes.
     *
     * Valeur: "saves/"
     */
    public static final String SAVE_DIRECTORY = "saves/";

    // ═══════════════════════════════════════════════════════════════
    // STATISTIQUES ET AFFICHAGE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Intervalle d'affichage des statistiques (en backtracks).
     * Affiche les stats tous les N backtracks.
     *
     * Valeur: 10,000 backtracks
     */
    public static final int STATS_DISPLAY_INTERVAL = 10_000;

    /**
     * Intervalle de mise à jour des records (en backtracks).
     * Vérifie les nouveaux records tous les N backtracks.
     *
     * Valeur: 1,000 backtracks
     */
    public static final int RECORD_CHECK_INTERVAL = 1_000;

    /**
     * Nombre de chiffres de précision pour le pourcentage de progression.
     *
     * Valeur: 8 décimales
     */
    public static final int PROGRESS_PRECISION_DECIMALS = 8;

    // ═══════════════════════════════════════════════════════════════
    // OPTIMISATIONS ET HEURISTIQUES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Nombre minimum de variables pour activer AC-3.
     * En dessous de ce seuil, AC-3 peut être trop coûteux.
     *
     * Valeur: 10 variables
     */
    public static final int MIN_VARIABLES_FOR_AC3 = 10;

    /**
     * Facteur de réduction pour le tri des candidats MRV.
     * Limite le nombre de candidats considérés pour accélérer.
     *
     * Valeur: 0.5 (considère 50% des meilleurs candidats)
     */
    public static final double MRV_CANDIDATE_REDUCTION_FACTOR = 0.5;

    /**
     * Seuil de domaine pour considérer une case comme contrainte.
     * Cases avec moins de N candidats sont prioritaires.
     *
     * Valeur: 5 candidats
     */
    public static final int DOMAIN_CONSTRAINT_THRESHOLD = 5;

    // ═══════════════════════════════════════════════════════════════
    // FORMAT DE FICHIERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Magic number pour les fichiers de sauvegarde binaire.
     * Identifie le format de fichier: "ETER" en hexadécimal.
     *
     * Valeur: 0x45544552
     */
    public static final int BINARY_SAVE_MAGIC_NUMBER = 0x45544552;

    /**
     * Version du format de sauvegarde binaire.
     * Incrémenté lors de changements incompatibles.
     *
     * Valeur: 1
     */
    public static final int BINARY_SAVE_FORMAT_VERSION = 1;

    // ═══════════════════════════════════════════════════════════════
    // PARALLÉLISATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Nombre de threads par défaut pour le solveur parallèle.
     * Utilisé si non spécifié par l'utilisateur.
     *
     * Valeur: -1 (auto-détection basée sur le nombre de CPUs)
     */
    public static final int DEFAULT_THREAD_COUNT = -1;

    /**
     * Nombre maximum de threads autorisés.
     * Limite supérieure pour éviter la sur-allocation.
     *
     * Valeur: 64 threads
     */
    public static final int MAX_THREAD_COUNT = 64;

    /**
     * Temps d'attente pour le shutdown des threads (en millisecondes).
     * Temps d'attente avant force-kill des threads.
     *
     * Valeur: 5,000 ms = 5 secondes
     */
    public static final long THREAD_SHUTDOWN_TIMEOUT_MS = 5_000;
}
