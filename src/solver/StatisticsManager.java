package solver;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages statistics and progress tracking for puzzle solving.
 * Extracted from EternitySolver for better code organization.
 */
public class StatisticsManager {

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
        int currentOption;     // Option actuellement explorée (0-indexed)

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
     * Calcule un pourcentage d'avancement estimé basé sur les premières profondeurs
     * Utilise les 5 premières profondeurs pour l'estimation
     */
    public double getProgressPercentage() {
        // Limiter le suivi aux 5 premières profondeurs pour la performance
        final int MAX_DEPTH_TRACKED = 5;

        double progress = 0.0;
        double weight = 1.0;

        for (int d = 0; d < MAX_DEPTH_TRACKED; d++) {
            ProgressTracker tracker = depthTrackers.get(d);
            if (tracker == null || tracker.totalOptions == 0) {
                break; // Plus de profondeur explorée
            }

            // Pourcentage à cette profondeur
            double depthProgress = (double) tracker.currentOption / tracker.totalOptions;
            progress += depthProgress * weight;
            weight /= tracker.totalOptions; // Pondération décroissante
        }

        return Math.min(100.0, progress * 100.0);
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
