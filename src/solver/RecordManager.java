package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gère les records de profondeur et le suivi des scores pendant la résolution.
 * Extrait de EternitySolver pour une meilleure organisation du code.
 */
public class RecordManager {

    private final String puzzleName;
    private final int threadId;
    private final int minDepthToShowRecords;
    private final Object lockObject;

    // Références atomiques pour la résolution parallèle
    private final AtomicInteger globalMaxDepth;
    private final AtomicInteger globalBestScore;
    private final AtomicInteger globalBestThreadId;
    private final AtomicReference<Board> globalBestBoard;
    private final AtomicReference<Map<Integer, Piece>> globalBestPieces;

    private int maxDepthReached = 0;
    private long lastProgressBacktracks = 0;

    /**
     * Constructeur pour RecordManager.
     *
     * @param puzzleName nom du puzzle
     * @param threadId ID du thread (-1 pour non-parallèle)
     * @param minDepthToShowRecords profondeur minimale pour afficher les records
     * @param lockObject objet de verrouillage pour les opérations synchronisées
     * @param globalMaxDepth référence atomique vers la profondeur maximale globale
     * @param globalBestScore référence atomique vers le meilleur score global
     * @param globalBestThreadId référence atomique vers l'ID du meilleur thread
     * @param globalBestBoard référence atomique vers le meilleur plateau
     * @param globalBestPieces référence atomique vers les meilleures pièces
     */
    public RecordManager(String puzzleName, int threadId, int minDepthToShowRecords,
                        Object lockObject, AtomicInteger globalMaxDepth,
                        AtomicInteger globalBestScore, AtomicInteger globalBestThreadId,
                        AtomicReference<Board> globalBestBoard,
                        AtomicReference<Map<Integer, Piece>> globalBestPieces) {
        this.puzzleName = puzzleName;
        this.threadId = threadId;
        this.minDepthToShowRecords = minDepthToShowRecords;
        this.lockObject = lockObject;
        this.globalMaxDepth = globalMaxDepth;
        this.globalBestScore = globalBestScore;
        this.globalBestThreadId = globalBestThreadId;
        this.globalBestBoard = globalBestBoard;
        this.globalBestPieces = globalBestPieces;
    }

    /**
     * Résultat de la vérification d'un nouveau record.
     */
    public static class RecordCheckResult {
        public final boolean isNewDepthRecord;
        public final boolean isNewScoreRecord;
        public final int currentScore;
        public final int maxScore;

        public RecordCheckResult(boolean isNewDepthRecord, boolean isNewScoreRecord,
                                int currentScore, int maxScore) {
            this.isNewDepthRecord = isNewDepthRecord;
            this.isNewScoreRecord = isNewScoreRecord;
            this.currentScore = currentScore;
            this.maxScore = maxScore;
        }
    }

    /**
     * Obtient la profondeur maximale atteinte.
     *
     * @return profondeur maximale atteinte
     */
    public int getMaxDepthReached() {
        return maxDepthReached;
    }

    /**
     * Obtient le nombre de retours en arrière lors du dernier progrès.
     *
     * @return derniers retours en arrière de progrès
     */
    public long getLastProgressBacktracks() {
        return lastProgressBacktracks;
    }

    /**
     * Vérifie si un nouveau record a été atteint et met à jour l'état.
     *
     * @param board état actuel du plateau
     * @param piecesById carte des pièces par ID
     * @param currentDepth profondeur actuelle (hors pièces fixes)
     * @param currentBacktracks nombre actuel de retours en arrière
     * @return résultat indiquant si de nouveaux records ont été atteints
     */
    public RecordCheckResult checkAndUpdateRecord(Board board, Map<Integer, Piece> piecesById,
                                                  int currentDepth, long currentBacktracks) {
        // Vérifie si nous avons atteint une nouvelle profondeur locale
        if (currentDepth <= maxDepthReached) {
            return null; // Pas de nouveau record
        }

        // Met à jour la profondeur maximale locale
        maxDepthReached = currentDepth;
        lastProgressBacktracks = currentBacktracks;

        // Calcule le score actuel
        int[] scoreData = board.calculateScore();
        int currentScore = scoreData[0];
        int maxScore = scoreData[1];

        // Met à jour les records globaux en utilisant CAS (Compare-And-Swap) pour des mises à jour sans verrou
        boolean isNewGlobalDepthRecord = updateGlobalDepthRecord(currentDepth);
        boolean isNewGlobalScoreRecord = updateGlobalScoreRecord(currentScore);

        // Si c'est un nouveau record global, sauvegarde le plateau
        if (isNewGlobalDepthRecord || isNewGlobalScoreRecord) {
            saveGlobalBestBoard(board, piecesById);
        }

        return new RecordCheckResult(isNewGlobalDepthRecord, isNewGlobalScoreRecord,
                                     currentScore, maxScore);
    }

    /**
     * Met à jour la profondeur maximale globale en utilisant CAS atomique.
     *
     * @param currentDepth profondeur actuelle
     * @return true si c'est un nouveau record global
     */
    private boolean updateGlobalDepthRecord(int currentDepth) {
        int oldMaxDepth, newMaxDepth;
        do {
            oldMaxDepth = globalMaxDepth.get();
            newMaxDepth = Math.max(oldMaxDepth, currentDepth);
        } while (oldMaxDepth < newMaxDepth && !globalMaxDepth.compareAndSet(oldMaxDepth, newMaxDepth));

        if (newMaxDepth > oldMaxDepth) {
            globalBestThreadId.set(threadId);
            return true;
        }
        return false;
    }

    /**
     * Met à jour le meilleur score global en utilisant CAS atomique.
     *
     * @param currentScore score actuel
     * @return true si c'est un nouveau record de score global
     */
    private boolean updateGlobalScoreRecord(int currentScore) {
        int oldMaxScore, newMaxScore;
        do {
            oldMaxScore = globalBestScore.get();
            newMaxScore = Math.max(oldMaxScore, currentScore);
        } while (oldMaxScore < newMaxScore && !globalBestScore.compareAndSet(oldMaxScore, newMaxScore));

        return newMaxScore > oldMaxScore;
    }

    /**
     * Sauvegarde le plateau actuel comme le meilleur global (synchronisé).
     *
     * @param board plateau actuel
     * @param piecesById carte des pièces par ID
     */
    private void saveGlobalBestBoard(Board board, Map<Integer, Piece> piecesById) {
        synchronized (lockObject) {
            // Crée une copie du plateau actuel
            Board newBestBoard = new Board(board.getRows(), board.getCols());
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        Placement p = board.getPlacement(r, c);
                        Piece piece = piecesById.get(p.getPieceId());
                        if (piece != null) {
                            newBestBoard.place(r, c, piece, p.getRotation());
                        }
                    }
                }
            }
            globalBestBoard.set(newBestBoard);
            globalBestPieces.set(new HashMap<>(piecesById));
        }
    }

    /**
     * Détermine si un record doit être affiché.
     *
     * @param result résultat de la vérification du record
     * @param currentDepth profondeur actuelle
     * @return true si le record doit être affiché
     */
    public boolean shouldShowRecord(RecordCheckResult result, int currentDepth) {
        if (result == null) return false;

        return currentDepth >= minDepthToShowRecords &&
               ((result.isNewDepthRecord && currentDepth > 60) || result.isNewScoreRecord);
    }

    /**
     * Affiche les informations du record.
     *
     * @param result résultat de la vérification du record
     * @param usedCount nombre total de pièces utilisées
     * @param stats gestionnaire de statistiques pour le progrès
     */
    public void displayRecord(RecordCheckResult result, int usedCount, StatisticsManager stats) {
        synchronized (System.out) {
            System.out.println("\n" + "=".repeat(80));

            if (result.isNewDepthRecord) {
                System.out.println("🔥 RECORD EXCEPTIONNEL ! " + usedCount + " pièces placées (Thread " + threadId + ") 🔥");
                System.out.println("📋 Puzzle: " + puzzleName);
            }

            if (result.isNewScoreRecord) {
                double percentage = result.maxScore > 0 ? (result.currentScore * 100.0 / result.maxScore) : 0.0;
                System.out.println("⭐ MEILLEUR SCORE ! " + result.currentScore + "/" + result.maxScore +
                                  " arêtes internes (" + String.format("%.1f%%", percentage) + ")");
                if (!result.isNewDepthRecord) {
                    System.out.println("📋 Puzzle: " + puzzleName);
                }
            }

            // Affiche le pourcentage de progression
            double progress = stats.getProgressPercentage();
            if (progress > 0.0 && progress < 99.9) {
                System.out.println("📊 Avancement estimé : " + String.format("%.8f%%", progress) +
                                  " (basé sur les 5 premières profondeurs)");
            } else if (progress >= 99.9) {
                System.out.println("📊 Avancement : exploration au-delà des profondeurs suivies (>5)");
            } else {
                System.out.println("📊 Avancement : calcul en cours... (en attente de données des premières profondeurs)");
            }

            System.out.println("=".repeat(80) + "\n");
        }
    }
}
