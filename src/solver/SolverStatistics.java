package solver;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe pour stocker les statistiques de résolution du puzzle.
 * Suit les métriques de performance comme les appels récursifs, les placements, les retours arrière,
 * et fournit une estimation de progression basée sur la profondeur de l'arbre de recherche.
 */
public class SolverStatistics {
    public long startTime;
    public long endTime;
    public long previousTimeOffset = 0; // Temps déjà cumulé dans les runs précédents
    public long recursiveCalls = 0;
    public long placements = 0;
    public long backtracks = 0;
    public long singletonsFound = 0;
    public long singletonsPlaced = 0;
    public long deadEndsDetected = 0;
    public long fitChecks = 0;
    public long forwardCheckRejects = 0;

    // Suivi de progression (pour estimation du %)
    private Map<Integer, ProgressTracker> depthTrackers = new HashMap<>();

    /**
     * Classe interne pour suivre la progression à une profondeur donnée
     */
    private static class ProgressTracker {
        int totalOptions;      // Nombre total d'options à cette profondeur
        int currentOption;     // Option actuellement explorée (indexé à partir de 0)

        ProgressTracker(int totalOptions) {
            this.totalOptions = totalOptions;
            this.currentOption = 0;
        }
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void start(long previousComputeTimeMs) {
        this.previousTimeOffset = previousComputeTimeMs;
        startTime = System.currentTimeMillis();
    }

    public void end() {
        endTime = System.currentTimeMillis();
    }

    public long getElapsedTimeMs() {
        long end = (endTime > 0) ? endTime : System.currentTimeMillis();
        return previousTimeOffset + (end - startTime);
    }

    public double getElapsedTimeSec() {
        return getElapsedTimeMs() / 1000.0;
    }

    /**
     * Enregistre le nombre d'options à une profondeur donnée
     */
    public void registerDepthOptions(int depth, int numOptions) {
        if (!depthTrackers.containsKey(depth)) {
            depthTrackers.put(depth, new ProgressTracker(numOptions));
        }
    }

    /**
     * Incrémente l'option actuelle à une profondeur donnée
     */
    public void incrementDepthProgress(int depth) {
        ProgressTracker tracker = depthTrackers.get(depth);
        if (tracker != null) {
            tracker.currentOption++;
        }
    }

    /**
     * Calcule un pourcentage d'avancement estimé basé sur les premières profondeurs.
     * Utilise les 5 premières profondeurs pour l'estimation.
     *
     * IMPORTANT : Ce pourcentage représente uniquement la progression dans l'arbre de recherche
     * des 5 premières profondeurs. Il n'indique PAS que 100% de la recherche totale est terminée,
     * car l'exploration continue au-delà de ces profondeurs.
     */
    public double getProgressPercentage() {
        // Limiter le suivi aux 5 premières profondeurs pour la performance
        final int MAX_DEPTH_TRACKED = 5;

        // Compter combien de profondeurs ont été complètement explorées
        int completedDepths = 0;

        for (int d = 0; d < MAX_DEPTH_TRACKED; d++) {
            ProgressTracker tracker = depthTrackers.get(d);
            if (tracker == null || tracker.totalOptions == 0) {
                // Cette profondeur n'a pas encore été explorée
                break;
            }

            // Vérifier si cette profondeur est complète
            if (tracker.currentOption >= tracker.totalOptions - 1) {
                completedDepths++;
            } else {
                // On a trouvé la première profondeur non complète
                // Le pourcentage représente la progression dans CETTE profondeur uniquement
                double depthProgress = (double) tracker.currentOption / tracker.totalOptions;
                return depthProgress * 100.0;
            }
        }

        // Si on arrive ici, les 5 premières profondeurs sont complètes
        // Mais cela ne veut PAS dire que la recherche est à 100% !
        // On retourne un indicateur que cette phase est complète (mais il y a plus de travail)
        return (completedDepths >= MAX_DEPTH_TRACKED) ? 100.0 : 0.0;
    }

    public void print() {
        System.out.println("\n╔════════════════ STATISTIQUES ═══════════════════╗");
        System.out.println("║ Temps écoulé       : " + String.format("%.2f", getElapsedTimeSec()) + " secondes");
        System.out.println("║ Appels récursifs   : " + recursiveCalls);
        System.out.println("║ Placements testés  : " + placements);
        System.out.println("║ Backtracks         : " + backtracks);
        System.out.println("║ Vérifications fit  : " + fitChecks);
        System.out.println("║ Forward check rejets : " + forwardCheckRejects);
        System.out.println("║ Singletons trouvés : " + singletonsFound);
        System.out.println("║ Singletons posés   : " + singletonsPlaced);
        System.out.println("║ Dead-ends détectés : " + deadEndsDetected);
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    public void printCompact() {
        System.out.printf("Stats: Récursif=%d | Placements=%d | Backtracks=%d | Singletons=%d/%d | Dead-ends=%d | Temps=%.1fs\n",
                recursiveCalls, placements, backtracks, singletonsPlaced, singletonsFound, deadEndsDetected, getElapsedTimeSec());
    }
}
