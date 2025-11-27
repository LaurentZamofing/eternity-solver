package util;

import model.Board;
import model.Piece;
import model.Placement;

import java.io.*;
import java.util.*;

/**
 * Gestionnaire de sauvegarde et chargement de l'état du puzzle.
 */
public class SaveManager {

    private static final String SAVE_DIR = "saves";
    private static final String SAVE_FILE = "best_state.txt";

    /**
     * Sauvegarde l'état actuel du board.
     */
    public static void saveBestState(Board board, Map<Integer, Piece> allPieces, int depth, int threadId) {
        try {
            File dir = new File(SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(SAVE_DIR + File.separator + SAVE_FILE);
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Header avec metadata
                writer.println("# Eternity II Best State");
                writer.println("# Depth: " + depth);
                writer.println("# Thread: " + threadId);
                writer.println("# Timestamp: " + System.currentTimeMillis());
                writer.println("# Date: " + new Date());
                writer.println();

                // Sauvegarder le board
                writer.println("BOARD_SIZE");
                writer.println(board.getRows() + " " + board.getCols());
                writer.println();

                writer.println("PLACEMENTS");
                for (int r = 0; r < board.getRows(); r++) {
                    for (int c = 0; c < board.getCols(); c++) {
                        if (!board.isEmpty(r, c)) {
                            Placement p = board.getPlacement(r, c);
                            // Format: row col pieceId rotation
                            writer.println(r + " " + c + " " + p.getPieceId() + " " + p.getRotation());
                        }
                    }
                }
                writer.println("END_PLACEMENTS");
            }

            System.out.println("💾 Sauvegarde effectuée: " + depth + " pièces (Thread " + threadId + ")");

        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Charge l'état sauvegardé.
     * @return [Board, Set<Integer> pièces utilisées, profondeur] ou null si pas de sauvegarde
     */
    public static Object[] loadBestState(Map<Integer, Piece> allPieces) {
        File file = new File(SAVE_DIR + File.separator + SAVE_FILE);
        if (!file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int rows = 0, cols = 0;
            int depth = 0;
            int threadId = -1;
            Board board = null;
            Set<Integer> usedPieceIds = new HashSet<>();

            // Lire les métadonnées
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Depth:")) {
                    depth = Integer.parseInt(line.substring(8).trim());
                } else if (line.startsWith("# Thread:")) {
                    threadId = Integer.parseInt(line.substring(9).trim());
                } else if (line.equals("BOARD_SIZE")) {
                    line = reader.readLine();
                    String[] parts = line.split(" ");
                    rows = Integer.parseInt(parts[0]);
                    cols = Integer.parseInt(parts[1]);
                    board = new Board(rows, cols);
                } else if (line.equals("PLACEMENTS")) {
                    break;
                }
            }

            if (board == null) {
                System.err.println("Erreur: taille du board non trouvée dans la sauvegarde");
                return null;
            }

            // Lire les placements
            while ((line = reader.readLine()) != null) {
                if (line.equals("END_PLACEMENTS")) {
                    break;
                }

                String[] parts = line.trim().split(" ");
                if (parts.length == 4) {
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    int pieceId = Integer.parseInt(parts[2]);
                    int rotation = Integer.parseInt(parts[3]);

                    Piece piece = allPieces.get(pieceId);
                    if (piece != null) {
                        board.place(r, c, piece, rotation);
                        usedPieceIds.add(pieceId);
                    }
                }
            }

            System.out.println("📂 Sauvegarde chargée: " + depth + " pièces (Thread " + threadId + ")");
            return new Object[]{board, usedPieceIds, depth, threadId};

        } catch (IOException | NumberFormatException e) {
            System.err.println("Erreur lors du chargement: " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si une sauvegarde existe.
     */
    public static boolean hasSavedState() {
        File file = new File(SAVE_DIR + File.separator + SAVE_FILE);
        return file.exists();
    }

    /**
     * Sauvegarde l'état d'un thread spécifique.
     */
    public static void saveThreadState(Board board, Map<Integer, Piece> allPieces, int depth, int threadId, long randomSeed) {
        try {
            File dir = new File(SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(SAVE_DIR + File.separator + "thread_" + threadId + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Header avec metadata
                writer.println("# Eternity II Thread State");
                writer.println("# Thread: " + threadId);
                writer.println("# Depth: " + depth);
                writer.println("# RandomSeed: " + randomSeed);
                writer.println("# Timestamp: " + System.currentTimeMillis());
                writer.println("# Date: " + new Date());
                writer.println();

                // Sauvegarder le board
                writer.println("BOARD_SIZE");
                writer.println(board.getRows() + " " + board.getCols());
                writer.println();

                writer.println("PLACEMENTS");
                for (int r = 0; r < board.getRows(); r++) {
                    for (int c = 0; c < board.getCols(); c++) {
                        if (!board.isEmpty(r, c)) {
                            Placement p = board.getPlacement(r, c);
                            writer.println(r + " " + c + " " + p.getPieceId() + " " + p.getRotation());
                        }
                    }
                }
                writer.println("END_PLACEMENTS");
            }

        } catch (IOException e) {
            System.err.println("Erreur sauvegarde thread " + threadId + ": " + e.getMessage());
        }
    }

    /**
     * Charge l'état d'un thread spécifique.
     * @return [Board, Set<Integer> pièces utilisées, profondeur, randomSeed] ou null
     */
    public static Object[] loadThreadState(int threadId, Map<Integer, Piece> allPieces) {
        File file = new File(SAVE_DIR + File.separator + "thread_" + threadId + ".txt");
        if (!file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int rows = 0, cols = 0;
            int depth = 0;
            long randomSeed = 0;
            Board board = null;
            Set<Integer> usedPieceIds = new HashSet<>();

            // Lire les métadonnées
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Depth:")) {
                    depth = Integer.parseInt(line.substring(8).trim());
                } else if (line.startsWith("# RandomSeed:")) {
                    randomSeed = Long.parseLong(line.substring(13).trim());
                } else if (line.equals("BOARD_SIZE")) {
                    line = reader.readLine();
                    String[] parts = line.split(" ");
                    rows = Integer.parseInt(parts[0]);
                    cols = Integer.parseInt(parts[1]);
                    board = new Board(rows, cols);
                } else if (line.equals("PLACEMENTS")) {
                    break;
                }
            }

            if (board == null) {
                System.err.println("Erreur: taille du board non trouvée pour thread " + threadId);
                return null;
            }

            // Lire les placements
            while ((line = reader.readLine()) != null) {
                if (line.equals("END_PLACEMENTS")) {
                    break;
                }

                String[] parts = line.trim().split(" ");
                if (parts.length == 4) {
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    int pieceId = Integer.parseInt(parts[2]);
                    int rotation = Integer.parseInt(parts[3]);

                    Piece piece = allPieces.get(pieceId);
                    if (piece != null) {
                        board.place(r, c, piece, rotation);
                        usedPieceIds.add(pieceId);
                    }
                }
            }

            return new Object[]{board, usedPieceIds, depth, randomSeed};

        } catch (IOException | NumberFormatException e) {
            System.err.println("Erreur chargement thread " + threadId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si une sauvegarde existe pour un thread spécifique.
     */
    public static boolean hasThreadState(int threadId) {
        File file = new File(SAVE_DIR + File.separator + "thread_" + threadId + ".txt");
        return file.exists();
    }

    /**
     * Compte le nombre de sauvegardes de threads disponibles.
     */
    public static int countThreadStates() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            return 0;
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith("thread_") && name.endsWith(".txt"));
        return files != null ? files.length : 0;
    }
}
