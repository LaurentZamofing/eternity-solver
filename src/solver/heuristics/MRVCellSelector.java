package solver.heuristics;

import model.Board;
import model.Piece;
import solver.DomainManager;
import solver.DomainManager.ValidPlacement;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * MRVCellSelector implémente l'heuristique MRV (Minimum Remaining Values).
 *
 * L'heuristique MRV sélectionne la case vide avec le moins de placements valides possibles.
 * Aussi connue comme l'heuristique "variable la plus contrainte" ou "fail-first".
 * En choisissant d'abord la case la plus contrainte, on détecte les échecs plus tôt dans
 * l'arbre de recherche, ce qui permet un élagage plus efficace.
 *
 * Fonctionnalités supplémentaires:
 * - Priorisation des bords: Remplit d'abord les cellules de bord avant l'intérieur
 * - Heuristique de degré pour départager: Choisit les cases avec plus de voisins remplis
 * - Détection de gaps: Évite de créer des gaps piégés sur les bords
 *
 * @author Équipe Eternity Solver
 */
public class MRVCellSelector implements HeuristicStrategy {

    private final DomainManager domainManager;
    private final FitChecker fitChecker;
    private final solver.NeighborAnalyzer neighborAnalyzer;
    private boolean prioritizeBorders = false;
    private boolean useAC3 = true;

    /**
     * Interface pour vérifier si une pièce s'adapte à une position.
     */
    public interface FitChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    /**
     * Constructeur pour MRVCellSelector.
     *
     * @param domainManager le gestionnaire de domaines pour les domaines AC-3
     * @param fitChecker le vérificateur pour valider les placements
     * @param neighborAnalyzer l'analyseur de voisinage pour l'analyse spatiale
     */
    public MRVCellSelector(DomainManager domainManager, FitChecker fitChecker, solver.NeighborAnalyzer neighborAnalyzer) {
        this.domainManager = domainManager;
        this.fitChecker = fitChecker;
        this.neighborAnalyzer = neighborAnalyzer;
    }

    /**
     * Active ou désactive la priorisation des bords.
     *
     * @param enabled true pour prioriser les cellules de bord
     */
    public void setPrioritizeBorders(boolean enabled) {
        this.prioritizeBorders = enabled;
    }

    /**
     * Active ou désactive l'utilisation des domaines AC-3.
     *
     * @param enabled true pour utiliser les domaines AC-3
     */
    public void setUseAC3(boolean enabled) {
        this.useAC3 = enabled;
    }

    @Override
    public CellPosition selectNextCell(Board board, Map<Integer, Piece> piecesById,
                                       BitSet pieceUsed, int totalPieces) {
        int[] result = findNextCellMRV(board, piecesById, pieceUsed, totalPieces);
        if (result == null) return null;
        return new CellPosition(result[0], result[1]);
    }

    /**
     * Trouve la prochaine case vide en utilisant l'heuristique MRV (Minimum Remaining Values).
     * Choisit la case avec le moins de placements valides possibles.
     *
     * @param board grille actuelle
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return coordonnées [r, c] de la case la plus contrainte, ou null si aucune
     */
    public int[] findNextCellMRV(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        int[] bestCell = null;
        int minUniquePieces = Integer.MAX_VALUE;
        boolean bestIsBorder = false;
        int bestBorderNeighbors = 0; // Tracker le nombre de voisins de bord de la meilleure case

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    // Détecter si c'est une case de bord
                    boolean isBorder = (r == 0 || r == board.getRows() - 1 || c == 0 || c == board.getCols() - 1);

                    // Use AC-3 domains if available for more efficient MRV
                    int uniquePiecesCount;
                    if (useAC3 && domainManager.isAC3Initialized()) {
                        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(r, c);
                        // Count unique pieces from AC-3 domains
                        uniquePiecesCount = (domain != null) ? domain.size() : 0;
                    } else {
                        // Fall back to computing from scratch
                        List<ValidPlacement> validPlacements = getValidPlacements(board, r, c, piecesById, pieceUsed, totalPieces);

                        // Extraire les IDs uniques des pièces
                        List<Integer> uniquePieceIds = new ArrayList<>();
                        for (ValidPlacement vp : validPlacements) {
                            if (!uniquePieceIds.contains(vp.pieceId)) {
                                uniquePieceIds.add(vp.pieceId);
                            }
                        }
                        uniquePiecesCount = uniquePieceIds.size();
                    }

                    // Si aucune possibilité, c'est un dead-end immédiat
                    if (uniquePiecesCount == 0) {
                        // Dead-end détecté - backtrack silencieux
                        return new int[]{r, c}; // Retourner immédiatement pour backtrack
                    }

                    // PRIORISATION DES BORDS: si activé, toujours choisir un bord avant l'intérieur
                    // NOUVELLE LOGIQUE: Pénaliser les gaps (cases sans voisins) et privilégier les séquences continues
                    boolean shouldUpdate = false;
                    if (prioritizeBorders) {
                        if (isBorder && !bestIsBorder) {
                            // Cette case est un bord et la meilleure actuelle ne l'est pas -> update
                            shouldUpdate = true;
                        } else if (isBorder == bestIsBorder && isBorder) {
                            // Deux cases de bord: appliquer la stratégie de remplissage continu

                            // VÉRIFICATION CRITIQUE : Cette case créerait-elle un gap piégé ?
                            boolean wouldTrap = neighborAnalyzer.wouldCreateTrappedGap(board, r, c, null, null, null, 0, -1);
                            boolean bestWouldTrap = (bestCell != null) ? neighborAnalyzer.wouldCreateTrappedGap(board, bestCell[0], bestCell[1], null, null, null, 0, -1) : false;

                            // RÈGLE 0 (PRIORITAIRE) : Ne JAMAIS choisir une case qui piège un gap
                            if (!wouldTrap && bestWouldTrap) {
                                // Case actuelle ne piège pas, mais meilleure piège -> privilégier case actuelle
                                shouldUpdate = true;
                            } else if (wouldTrap && !bestWouldTrap) {
                                // Case actuelle piège, mais meilleure ne piège pas -> garder meilleure
                                shouldUpdate = false;
                            } else if (!wouldTrap && !bestWouldTrap) {
                                // Aucune des deux ne piège : appliquer les règles normales de continuité

                                // Compter les voisins de bord remplis pour la case actuelle
                                int currentBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);

                                // RÈGLE 1: Toujours privilégier les cases avec des voisins de bord (éviter les gaps)
                                if (currentBorderNeighbors > 0 && bestBorderNeighbors == 0) {
                                    // Case actuelle a des voisins, la meilleure n'en a pas -> privilégier case actuelle
                                    shouldUpdate = true;
                                } else if (currentBorderNeighbors == 0 && bestBorderNeighbors > 0) {
                                    // Meilleure case a des voisins, case actuelle n'en a pas -> garder la meilleure
                                    shouldUpdate = false;
                                } else if (currentBorderNeighbors == 0 && bestBorderNeighbors == 0) {
                                    // RÈGLE 2: Si les deux cases n'ont pas de voisins (début ou gap potentiel)
                                    // N'accepter la case actuelle QUE si elle a significativement moins d'options (≤50%)
                                    if (uniquePiecesCount * 2 <= minUniquePieces) {
                                        shouldUpdate = true; // Case actuelle a ≤50% des options -> accepter malgré le gap
                                    }
                                } else {
                                    // Les deux ont des voisins de bord: comparer d'abord le nombre de voisins
                                    if (currentBorderNeighbors > bestBorderNeighbors) {
                                        // Plus de voisins = meilleure continuité -> TOUJOURS privilégier
                                        shouldUpdate = true;
                                    } else if (currentBorderNeighbors == bestBorderNeighbors) {
                                        // Même nombre de voisins: utiliser MRV pour départager
                                        shouldUpdate = (uniquePiecesCount < minUniquePieces);
                                    }
                                    // Si currentBorderNeighbors < bestBorderNeighbors -> ne pas update, garder la meilleure
                                }
                            } else {
                                // Les DEUX cases piègent un gap : choisir le moindre mal (MRV)
                                shouldUpdate = (uniquePiecesCount < minUniquePieces);
                            }
                        } else if (!isBorder && !bestIsBorder) {
                            // Deux cases intérieures: MRV normal
                            shouldUpdate = (uniquePiecesCount < minUniquePieces);
                        }
                        // Sinon (!isBorder && bestIsBorder) -> ne pas update, garder le bord
                    } else {
                        // Mode normal sans priorisation des bords
                        shouldUpdate = (uniquePiecesCount < minUniquePieces);
                    }

                    // Choisir la case avec le minimum de pièces uniques (avec priorisation bords si activée)
                    if (shouldUpdate) {
                        minUniquePieces = uniquePiecesCount;
                        bestCell = new int[]{r, c};
                        bestIsBorder = isBorder;
                        // IMPORTANT: Mettre à jour le nombre de voisins de bord pour les cases de bord
                        if (isBorder) {
                            bestBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);
                        }
                    } else if (uniquePiecesCount == minUniquePieces && bestCell != null && (isBorder == bestIsBorder || !prioritizeBorders)) {
                        // Tie-breaking: utiliser l'heuristique de degré
                        // Compter le nombre de voisins OCCUPÉS pour cette case (plus de contraintes = mieux)
                        int currentConstraints = neighborAnalyzer.countOccupiedNeighbors(board, r, c);
                        int bestConstraints = neighborAnalyzer.countOccupiedNeighbors(board, bestCell[0], bestCell[1]);

                        // Choisir la case avec le plus de voisins occupés (plus contraignante)
                        if (currentConstraints > bestConstraints) {
                            bestCell = new int[]{r, c};
                        } else if (currentConstraints == bestConstraints) {
                            // Second tie-breaking: privilégier les positions centrales
                            int centerR = board.getRows() / 2;
                            int centerC = board.getCols() / 2;
                            int currentDistToCenter = Math.abs(r - centerR) + Math.abs(c - centerC);
                            int bestDistToCenter = Math.abs(bestCell[0] - centerR) + Math.abs(bestCell[1] - centerC);

                            if (currentDistToCenter < bestDistToCenter) {
                                bestCell = new int[]{r, c};
                            }
                        }
                    }
                }
            }
        }

        return bestCell;
    }

    /**
     * Obtient les placements valides pour une cellule.
     */
    private List<ValidPlacement> getValidPlacements(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        List<ValidPlacement> validPlacements = new ArrayList<>();

        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée

            Piece piece = piecesById.get(pid);
            for (int rot = 0; rot < 4; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fitChecker.fits(board, r, c, candidate)) {
                    validPlacements.add(new ValidPlacement(pid, rot));
                }
            }
        }
        return validPlacements;
    }

    @Override
    public String getName() {
        return "MRV (Minimum Remaining Values)";
    }
}
