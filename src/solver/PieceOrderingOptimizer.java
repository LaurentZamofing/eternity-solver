package solver;

import model.Board;
import model.Piece;

import java.util.*;

/**
 * Optimise l'ordonnancement et la sélection de pièces pour une résolution efficace du puzzle.
 *
 * Cette classe fournit des stratégies pour :
 * - Trouver des placements de pièces valides en utilisant la compatibilité des bords
 * - Ordonner les pièces selon l'heuristique LCV (Least Constraining Value)
 * - Ordonner les pièces par difficulté (les plus difficiles d'abord pour fail-fast)
 * - Sélectionner des pièces de coin pour la diversification des threads
 *
 * Ces optimisations réduisent significativement l'espace de recherche en :
 * - Essayant d'abord les pièces les plus difficiles (stratégie fail-fast)
 * - Préférant les pièces qui laissent le plus de flexibilité
 * - Utilisant la compatibilité des bords pré-calculée pour filtrage rapide
 *
 * Extrait de EternitySolver dans le Sprint 5 pour améliorer :
 * - La modularité et la réutilisabilité
 * - La testabilité des stratégies d'ordonnancement
 * - La séparation de la logique heuristique
 */
public class PieceOrderingOptimizer {

    private final EdgeCompatibilityIndex edgeIndex;
    private final PlacementValidator validator;
    private final NeighborAnalyzer neighborAnalyzer;

    /**
     * Classe auxiliaire pour stocker l'ID de pièce et le score pour le tri.
     */
    public static class PieceScore {
        public final int pieceId;
        public final int score;

        public PieceScore(int pieceId, int score) {
            this.pieceId = pieceId;
            this.score = score;
        }
    }

    /**
     * Constructeur
     * @param edgeIndex index de compatibilité des bords
     * @param validator validateur de placement
     * @param neighborAnalyzer analyseur de voisinage pour calculs de contraintes
     */
    public PieceOrderingOptimizer(EdgeCompatibilityIndex edgeIndex,
                                  PlacementValidator validator,
                                  NeighborAnalyzer neighborAnalyzer) {
        this.edgeIndex = edgeIndex;
        this.validator = validator;
        this.neighborAnalyzer = neighborAnalyzer;
    }

    /**
     * Trouve tous les placements valides (pièce, rotation) pour une cellule.
     * Utilise l'index de compatibilité des bords pour rejet précoce.
     *
     * @param board état actuel du plateau
     * @param r position ligne
     * @param c position colonne
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return liste des placements valides
     */
    public List<DomainManager.ValidPlacement> getValidPlacements(
            Board board, int r, int c,
            Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {

        List<DomainManager.ValidPlacement> validPlacements = new ArrayList<>();

        // Essayer toutes les pièces disponibles
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            int maxRotations = piece.getUniqueRotationCount();
            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);
                if (validator.fits(board, r, c, edges)) {
                    validPlacements.add(new DomainManager.ValidPlacement(pid, rot));
                }
            }
        }

        return validPlacements;
    }

    /**
     * Compte les placements valides pour une cellule.
     * Encapsuleur de getValidPlacements() qui retourne seulement le comptage.
     *
     * @param board état actuel du plateau
     * @param r position ligne
     * @param c position colonne
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return nombre de placements valides
     */
    public int countValidPlacements(Board board, int r, int c,
                                   Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        return getValidPlacements(board, r, c, pieces, pieceUsed, totalPieces).size();
    }

    /**
     * Compte les pièces uniques qui peuvent être placées à une position (en ignorant les rotations).
     *
     * @param board état actuel du plateau
     * @param r position ligne
     * @param c position colonne
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return nombre de pièces uniques qui s'adaptent
     */
    public int countUniquePieces(Board board, int r, int c,
                                Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        Set<Integer> uniquePieces = new HashSet<>();

        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            int maxRotations = piece.getUniqueRotationCount();
            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);
                if (validator.fits(board, r, c, edges)) {
                    uniquePieces.add(pid);
                    break; // Compter la pièce une fois
                }
            }
        }

        return uniquePieces.size();
    }

    /**
     * Ordonne les pièces selon l'heuristique LCV (Least Constraining Value).
     * Les pièces qui laissent plus d'options pour les voisins sont préférées (score de contrainte plus bas).
     *
     * @param board état actuel du plateau
     * @param r position ligne
     * @param c position colonne
     * @param availablePieces liste des IDs de pièces disponibles
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return liste des IDs de pièces ordonnées par LCV (moins contraignant d'abord)
     */
    public List<Integer> orderPiecesByLeastConstraining(
            Board board, int r, int c, List<Integer> availablePieces,
            Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {

        List<PieceScore> scored = new ArrayList<>();

        for (int pid : availablePieces) {
            if (pieceUsed.get(pid)) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            // Essayer toutes les rotations et trouver la moins contraignante
            int bestScore = Integer.MAX_VALUE;
            int maxRotations = piece.getUniqueRotationCount();

            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);
                if (validator.fits(board, r, c, edges)) {
                    int constraintScore = neighborAnalyzer.calculateConstraintScore(
                        board, r, c, edges, pieces, pieceUsed, totalPieces, pid);
                    bestScore = Math.min(bestScore, constraintScore);
                }
            }

            if (bestScore != Integer.MAX_VALUE) {
                scored.add(new PieceScore(pid, bestScore));
            }
        }

        // Trier par score (contrainte plus basse = meilleur)
        scored.sort(Comparator.comparingInt(ps -> ps.score));

        List<Integer> ordered = new ArrayList<>();
        for (PieceScore ps : scored) {
            ordered.add(ps.pieceId);
        }

        return ordered;
    }

    /**
     * Ordonne les pièces par difficulté (les plus difficiles d'abord).
     * Utilise les scores de difficulté pré-calculés de LeastConstrainingValueOrderer.
     *
     * Stratégie fail-fast : Essayer les pièces difficiles d'abord pour détecter les cul-de-sac tôt.
     *
     * @param pieceIds liste des IDs de pièces à ordonner
     * @param difficultyScores map de l'ID de pièce au score de difficulté (plus élevé = plus difficile)
     * @return liste ordonnée par difficulté (les plus difficiles d'abord)
     */
    public List<Integer> orderByDifficulty(List<Integer> pieceIds,
                                          Map<Integer, Integer> difficultyScores) {
        if (difficultyScores == null || difficultyScores.isEmpty()) {
            return new ArrayList<>(pieceIds); // Retourner une copie dans l'ordre original
        }

        List<Integer> ordered = new ArrayList<>(pieceIds);
        ordered.sort(Comparator.comparingInt(pid ->
            difficultyScores.getOrDefault(pid, Integer.MAX_VALUE)));

        return ordered;
    }

    /**
     * Sélectionne une pièce de coin pour la diversification des threads.
     * Différents threads essaient différentes pièces de coin pour explorer différentes parties
     * de l'espace de recherche en parallèle.
     *
     * @param threadId ID du thread (0-3 correspond aux coins)
     * @param unusedIds liste des IDs de pièces non utilisées
     * @param pieces map de toutes les pièces
     * @return ID de pièce de coin, ou null si aucun coin approprié trouvé
     */
    public Integer selectCornerPieceForThread(int threadId, List<Integer> unusedIds,
                                             Map<Integer, Piece> pieces) {
        if (threadId >= 4 || unusedIds.size() <= 10) {
            return null; // Seulement pour les 4 premiers threads, et seulement si assez de pièces
        }

        // Identifier les pièces de coin (exactement 2 bords = 0)
        List<Integer> cornerPieces = new ArrayList<>();
        for (int pid : unusedIds) {
            Piece p = pieces.get(pid);
            if (p == null) continue;

            int[] edges = p.getEdges();
            int zeroCount = 0;
            for (int e : edges) {
                if (e == 0) zeroCount++;
            }
            if (zeroCount == 2) {
                cornerPieces.add(pid);
            }
        }

        if (cornerPieces.isEmpty()) {
            return null;
        }

        // Sélectionner le coin basé sur l'ID du thread (distribuer équitablement)
        int index = threadId % cornerPieces.size();
        return cornerPieces.get(index);
    }

    /**
     * Obtient la position du coin pour un ID de thread.
     * Mappe les threads 0-3 aux quatre coins du plateau.
     *
     * @param threadId ID du thread (0-3)
     * @param rows nombre de lignes
     * @param cols nombre de colonnes
     * @return [ligne, colonne] du coin, ou null si threadId invalide
     */
    public int[] getCornerPositionForThread(int threadId, int rows, int cols) {
        switch (threadId) {
            case 0: return new int[]{0, 0};           // Haut-gauche
            case 1: return new int[]{0, cols - 1};    // Haut-droite
            case 2: return new int[]{rows - 1, 0};    // Bas-gauche
            case 3: return new int[]{rows - 1, cols - 1}; // Bas-droite
            default: return null;
        }
    }

    /**
     * Trouve la rotation correcte pour une pièce de coin à une position de coin spécifique.
     * Les pièces de coin ont exactement 2 zéros, qui doivent s'aligner avec les bords du plateau.
     *
     * @param piece pièce de coin
     * @param cornerRow ligne du coin (0 ou max)
     * @param cornerCol colonne du coin (0 ou max)
     * @param rows nombre total de lignes
     * @param cols nombre total de colonnes
     * @return rotation (0-3), ou -1 si aucune rotation valide trouvée
     */
    public int findCornerRotation(Piece piece, int cornerRow, int cornerCol, int rows, int cols) {
        boolean isTopEdge = (cornerRow == 0);
        boolean isBottomEdge = (cornerRow == rows - 1);
        boolean isLeftEdge = (cornerCol == 0);
        boolean isRightEdge = (cornerCol == cols - 1);

        for (int rot = 0; rot < 4; rot++) {
            int[] edges = piece.edgesRotated(rot);
            boolean valid = true;

            // Vérifier que chaque bord correspond à la contrainte du plateau
            if (isTopEdge && edges[0] != 0) valid = false;      // Nord doit être 0
            if (isBottomEdge && edges[2] != 0) valid = false;   // Sud doit être 0
            if (isLeftEdge && edges[3] != 0) valid = false;     // Ouest doit être 0
            if (isRightEdge && edges[1] != 0) valid = false;    // Est doit être 0

            if (valid) {
                return rot;
            }
        }

        return -1; // Aucune rotation valide trouvée
    }
}
