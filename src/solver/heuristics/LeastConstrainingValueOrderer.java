package solver.heuristics;

import model.Piece;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * LeastConstrainingValueOrderer calcule les scores de difficulté des pièces pour l'ordonnancement.
 *
 * Cette classe implémente l'heuristique "Least Constraining Value" pour ordonner les pièces.
 * L'idée est d'essayer d'abord les pièces les moins contraignantes, donnant plus de flexibilité
 * pour les placements futurs. Alternativement, elle peut être utilisée en sens inverse pour essayer
 * les pièces les plus difficiles en premier (stratégie fail-fast).
 *
 * Les scores de difficulté sont calculés en fonction de la compatibilité des bords :
 * - Pour chaque pièce, compter combien d'autres pièces pourraient potentiellement correspondre à chacun de ses bords
 * - Score plus bas = plus contraint = plus difficile à placer
 * - Score plus élevé = moins contraint = plus facile à placer
 *
 * La classe construit également des tables de compatibilité des bords qui mappent les valeurs de bord
 * à l'ensemble des pièces qui peuvent fournir cette valeur de bord (dans n'importe quelle rotation).
 *
 * @author Eternity Solver Team
 */
public class LeastConstrainingValueOrderer {

    // Tables de compatibilité des bords pour un ordonnancement rapide des pièces
    private Map<Integer, Set<Integer>> northEdgeCompatible;  // valeurBord -> IDs des pièces
    private Map<Integer, Set<Integer>> eastEdgeCompatible;
    private Map<Integer, Set<Integer>> southEdgeCompatible;
    private Map<Integer, Set<Integer>> westEdgeCompatible;

    // Scores de difficulté des pièces : idPièce -> difficulté
    // Score plus bas = plus contraint = plus difficile à placer
    private Map<Integer, Integer> pieceDifficultyScores;

    private final boolean verbose;

    /**
     * Constructeur pour LeastConstrainingValueOrderer.
     *
     * @param verbose si l'on doit afficher une sortie détaillée
     */
    public LeastConstrainingValueOrderer(boolean verbose) {
        this.verbose = verbose;
        this.pieceDifficultyScores = new HashMap<>();
    }

    /**
     * Construit les tables de compatibilité des bords pour un ordonnancement rapide.
     * Pour chaque valeur de bord, précalcule quelles pièces (dans n'importe quelle rotation) peuvent correspondre.
     *
     * @param pieces map de toutes les pièces par ID
     */
    public void buildEdgeCompatibilityTables(Map<Integer, Piece> pieces) {
        northEdgeCompatible = new HashMap<>();
        eastEdgeCompatible = new HashMap<>();
        southEdgeCompatible = new HashMap<>();
        westEdgeCompatible = new HashMap<>();

        // Pour chaque pièce, pour chaque rotation, enregistrer quels bords elle peut fournir
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int pieceId = entry.getKey();
            Piece piece = entry.getValue();

            // Essayer les 4 rotations
            for (int rot = 0; rot < 4; rot++) {
                int[] edges = piece.edgesRotated(rot);

                // Cette pièce (à cette rotation) fournit ces valeurs de bord :
                northEdgeCompatible.computeIfAbsent(edges[0], k -> new HashSet<>()).add(pieceId);
                eastEdgeCompatible.computeIfAbsent(edges[1], k -> new HashSet<>()).add(pieceId);
                southEdgeCompatible.computeIfAbsent(edges[2], k -> new HashSet<>()).add(pieceId);
                westEdgeCompatible.computeIfAbsent(edges[3], k -> new HashSet<>()).add(pieceId);
            }
        }

        if (verbose) {
            System.out.println("✓ Edge compatibility tables built:");
            System.out.println("  - Unique edge values: ~" + northEdgeCompatible.size());
        }
    }

    /**
     * Calcule le score de difficulté pour chaque pièce en fonction des contraintes de motif des bords.
     * Difficulté = somme des pièces correspondantes pour chaque bord (plus bas = plus contraint = plus difficile)
     * Il s'agit d'une approximation légère qui ne nécessite pas de balayage complet du plateau.
     *
     * @param pieces map de toutes les pièces par ID
     */
    public void computePieceDifficulty(Map<Integer, Piece> pieces) {
        pieceDifficultyScores = new HashMap<>();

        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int pieceId = entry.getKey();
            Piece piece = entry.getValue();
            int[] edges = piece.getEdges();

            // Pour chaque bord, compter combien de pièces pourraient potentiellement correspondre
            int totalCompatibility = 0;
            for (int i = 0; i < 4; i++) {
                int edgeValue = edges[i];
                // Compter les pièces qui ont cette valeur de bord dans n'importe quelle rotation
                Set<Integer> compatiblePieces = new HashSet<>();
                for (int rot = 0; rot < 4; rot++) {
                    int[] rotatedEdges = piece.edgesRotated(rot);
                    // Vérifier toutes les tables de compatibilité
                    if (northEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(northEdgeCompatible.get(edgeValue));
                    }
                    if (eastEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(eastEdgeCompatible.get(edgeValue));
                    }
                    if (southEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(southEdgeCompatible.get(edgeValue));
                    }
                    if (westEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(westEdgeCompatible.get(edgeValue));
                    }
                }
                totalCompatibility += compatiblePieces.size();
            }

            // Score plus bas = moins de pièces compatibles = plus difficile à placer = devrait essayer en premier
            pieceDifficultyScores.put(pieceId, totalCompatibility);
        }

        if (verbose) {
            System.out.println("✓ Piece difficulty scores computed");
        }
    }

    /**
     * Obtient le score de difficulté pour une pièce.
     * Score plus bas = plus contraint = plus difficile à placer.
     *
     * @param pieceId l'ID de la pièce
     * @return score de difficulté, ou Integer.MAX_VALUE si non trouvé
     */
    public int getDifficultyScore(int pieceId) {
        return pieceDifficultyScores.getOrDefault(pieceId, Integer.MAX_VALUE);
    }

    /**
     * Obtient tous les scores de difficulté.
     *
     * @return map des IDs de pièces vers les scores de difficulté
     */
    public Map<Integer, Integer> getAllDifficultyScores() {
        return new HashMap<>(pieceDifficultyScores);
    }

    /**
     * Obtient les pièces qui peuvent fournir une valeur de bord spécifique sur le côté nord.
     *
     * @param edgeValue la valeur de bord à faire correspondre
     * @return ensemble des IDs de pièces qui peuvent fournir cette valeur de bord
     */
    public Set<Integer> getNorthCompatiblePieces(int edgeValue) {
        return northEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /**
     * Obtient les pièces qui peuvent fournir une valeur de bord spécifique sur le côté est.
     *
     * @param edgeValue la valeur de bord à faire correspondre
     * @return ensemble des IDs de pièces qui peuvent fournir cette valeur de bord
     */
    public Set<Integer> getEastCompatiblePieces(int edgeValue) {
        return eastEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /**
     * Obtient les pièces qui peuvent fournir une valeur de bord spécifique sur le côté sud.
     *
     * @param edgeValue la valeur de bord à faire correspondre
     * @return ensemble des IDs de pièces qui peuvent fournir cette valeur de bord
     */
    public Set<Integer> getSouthCompatiblePieces(int edgeValue) {
        return southEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /**
     * Obtient les pièces qui peuvent fournir une valeur de bord spécifique sur le côté ouest.
     *
     * @param edgeValue la valeur de bord à faire correspondre
     * @return ensemble des IDs de pièces qui peuvent fournir cette valeur de bord
     */
    public Set<Integer> getWestCompatiblePieces(int edgeValue) {
        return westEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /**
     * Obtient les pièces qui pourraient potentiellement correspondre à une cellule en fonction des contraintes des voisins.
     * Ceci utilise les tables de compatibilité des bords pour un filtrage rapide.
     *
     * @param northEdge valeur de bord nord requise (-1 si aucune contrainte)
     * @param eastEdge valeur de bord est requise (-1 si aucune contrainte)
     * @param southEdge valeur de bord sud requise (-1 si aucune contrainte)
     * @param westEdge valeur de bord ouest requise (-1 si aucune contrainte)
     * @return ensemble des IDs de pièces qui pourraient potentiellement correspondre à toutes les contraintes
     */
    public Set<Integer> getCandidatePieces(int northEdge, int eastEdge, int southEdge, int westEdge) {
        Set<Integer> candidates = null;

        // Construire l'ensemble de contraintes en intersectant les pièces compatibles pour chaque bord
        if (northEdge >= 0) {
            candidates = new HashSet<>(getNorthCompatiblePieces(northEdge));
        }
        if (eastEdge >= 0) {
            Set<Integer> eastCandidates = getEastCompatiblePieces(eastEdge);
            if (candidates == null) {
                candidates = new HashSet<>(eastCandidates);
            } else {
                candidates.retainAll(eastCandidates);
            }
        }
        if (southEdge >= 0) {
            Set<Integer> southCandidates = getSouthCompatiblePieces(southEdge);
            if (candidates == null) {
                candidates = new HashSet<>(southCandidates);
            } else {
                candidates.retainAll(southCandidates);
            }
        }
        if (westEdge >= 0) {
            Set<Integer> westCandidates = getWestCompatiblePieces(westEdge);
            if (candidates == null) {
                candidates = new HashSet<>(westCandidates);
            } else {
                candidates.retainAll(westCandidates);
            }
        }

        return (candidates != null) ? candidates : new HashSet<>();
    }

    /**
     * Vérifie si les tables de compatibilité ont été construites.
     *
     * @return true si les tables sont initialisées
     */
    public boolean isInitialized() {
        return northEdgeCompatible != null && pieceDifficultyScores != null;
    }

    /**
     * Efface toutes les structures de données.
     */
    public void clear() {
        if (northEdgeCompatible != null) northEdgeCompatible.clear();
        if (eastEdgeCompatible != null) eastEdgeCompatible.clear();
        if (southEdgeCompatible != null) southEdgeCompatible.clear();
        if (westEdgeCompatible != null) westEdgeCompatible.clear();
        if (pieceDifficultyScores != null) pieceDifficultyScores.clear();
    }

    /**
     * Obtient les statistiques sur la compatibilité des bords.
     *
     * @return une chaîne décrivant les statistiques des tables de compatibilité
     */
    public String getStatistics() {
        if (!isInitialized()) {
            return "Tables de compatibilité des bords non initialisées";
        }

        int uniqueEdgeValues = northEdgeCompatible.size();
        int avgPiecesPerEdge = 0;
        if (uniqueEdgeValues > 0) {
            int totalPieces = 0;
            for (Set<Integer> pieces : northEdgeCompatible.values()) {
                totalPieces += pieces.size();
            }
            avgPiecesPerEdge = totalPieces / uniqueEdgeValues;
        }

        return String.format("Compatibilité des bords : %d valeurs de bord uniques, ~%d pièces/bord en moyenne",
                           uniqueEdgeValues, avgPiecesPerEdge);
    }
}
