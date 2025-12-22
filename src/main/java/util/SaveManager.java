package util;

import model.Board;
import model.Piece;
import model.Placement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manager for saving and loading puzzle state.
 */
public class SaveManager {

    private static final String SAVE_DIR = "saves";
    private static final String SAVE_FILE = "best_state.txt";

    /**
     * Saves the current board state.
     */
    public static void saveBestState(Board board, Map<Integer, Piece> allPieces, int depth, int threadId) {
        try {
            File dir = new File(SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(SAVE_DIR + File.separator + SAVE_FILE);
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Header with metadata
                writer.println("# Eternity II Best State");
                writer.println("# Depth: " + depth);
                writer.println("# Thread: " + threadId);
                writer.println("# Timestamp: " + System.currentTimeMillis());
                writer.println("# Date: " + new Date());
                writer.println();

                // Save the board
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

            SolverLogger.save("Save completed: {} pieces (Thread {})", depth, threadId);

        } catch (IOException e) {
            SolverLogger.error("Error during save: {}", e.getMessage());
        }
    }

    /**
     * Loads the saved state.
     * @return [Board, Set<Integer> used pieces, depth] or null if no save
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

            // Read metadata
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
                SolverLogger.error("Error: board size not found in save");
                return null;
            }

            // Read placements
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

            SolverLogger.save("Save loaded: {} pieces (Thread {})", depth, threadId);
            return new Object[]{board, usedPieceIds, depth, threadId};

        } catch (IOException | NumberFormatException e) {
            SolverLogger.error("Error during load: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a save exists.
     */
    public static boolean hasSavedState() {
        File file = new File(SAVE_DIR + File.separator + SAVE_FILE);
        return file.exists();
    }

    /**
     * Saves the state of a specific thread.
     */
    public static void saveThreadState(Board board, Map<Integer, Piece> allPieces, int depth, int threadId, long randomSeed) {
        try {
            File dir = new File(SAVE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(SAVE_DIR + File.separator + "thread_" + threadId + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Header with metadata
                writer.println("# Eternity II Thread State");
                writer.println("# Thread: " + threadId);
                writer.println("# Depth: " + depth);
                writer.println("# RandomSeed: " + randomSeed);
                writer.println("# Timestamp: " + System.currentTimeMillis());
                writer.println("# Date: " + new Date());
                writer.println();

                // Save the board
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
            SolverLogger.error("Error saving thread {}: {}", threadId, e.getMessage());
        }
    }

    /**
     * Loads the state of a specific thread.
     * @return [Board, Set<Integer> used pieces, depth, randomSeed] or null
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

            // Read metadata
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
                SolverLogger.error("Error: board size not found for thread {}", threadId);
                return null;
            }

            // Read placements
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
            SolverLogger.error("Error loading thread {}: {}", threadId, e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a save exists for a specific thread.
     */
    public static boolean hasThreadState(int threadId) {
        File file = new File(SAVE_DIR + File.separator + "thread_" + threadId + ".txt");
        return file.exists();
    }

    /**
     * Counts the number of available thread saves.
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
