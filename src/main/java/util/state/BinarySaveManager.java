package util.state;

import model.Board;
import model.Piece;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary save format manager for faster save/load operations
 * Provides 5-10x faster save/restore compared to text-based format
 */
public class BinarySaveManager {

    private static final int MAGIC_NUMBER = 0x45544552; // "ETER" in hex
    private static final int FORMAT_VERSION = 1;

    /**
     * Save board state in binary format
     */
    public static void saveBinary(String filename, Board board, Map<Integer, Piece> pieces,
                                   boolean[] pieceUsed, int totalPieces) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename)))) {

            // Header
            out.writeInt(MAGIC_NUMBER); // Magic number "ETER"
            out.writeInt(FORMAT_VERSION); // Version
            out.writeLong(System.currentTimeMillis()); // Timestamp

            // Board dimensions
            out.writeInt(board.getRows());
            out.writeInt(board.getCols());

            // Placements (only occupied cells)
            int count = 0;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) count++;
                }
            }
            out.writeInt(count);

            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        out.writeShort(r);
                        out.writeShort(c);
                        out.writeShort(board.getPlacement(r, c).getPieceId());
                        out.writeByte(board.getPlacement(r, c).getRotation());
                    }
                }
            }

            // Piece usage bitmap - write the array size (maxPieceId)
            int maxPieceId = pieceUsed.length - 1;
            out.writeInt(maxPieceId);
            for (int i = 1; i <= maxPieceId; i++) {
                out.writeBoolean(pieceUsed[i]);
            }
        }
    }

    /**
     * Load board state from binary format
     */
    public static SaveData loadBinary(String filename) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)))) {

            // Verify magic number
            int magic = in.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new IOException("Invalid binary save file: magic number mismatch");
            }

            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported binary format version: " + version);
            }

            long timestamp = in.readLong();

            int rows = in.readInt();
            int cols = in.readInt();

            // Read placements
            int count = in.readInt();
            List<PlacementData> placements = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int r = in.readShort();
                int c = in.readShort();
                int pieceId = in.readShort();
                int rotation = in.readByte();
                placements.add(new PlacementData(r, c, pieceId, rotation));
            }

            // Read piece usage - the file contains the MAX piece ID, not the count
            int maxPieceId = in.readInt();
            boolean[] pieceUsed = new boolean[maxPieceId + 1];
            for (int i = 1; i <= maxPieceId; i++) {
                pieceUsed[i] = in.readBoolean();
            }

            return new SaveData(rows, cols, placements, pieceUsed, timestamp);
        }
    }

    /**
     * Data class to hold save state information
     */
    public static class SaveData {
        public final int rows, cols;
        public final List<PlacementData> placements;
        public final boolean[] pieceUsed;
        public final long timestamp;

        public SaveData(int rows, int cols, List<PlacementData> placements,
                       boolean[] pieceUsed, long timestamp) {
            this.rows = rows;
            this.cols = cols;
            this.placements = placements;
            this.pieceUsed = pieceUsed;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class to hold placement information
     */
    public static class PlacementData {
        public final int row, col, pieceId, rotation;

        public PlacementData(int row, int col, int pieceId, int rotation) {
            this.row = row;
            this.col = col;
            this.pieceId = pieceId;
            this.rotation = rotation;
        }
    }

    /**
     * Check if a file exists and is a valid binary save file
     */
    public static boolean isValidBinarySave(String filename) {
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            return false;
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)))) {
            int magic = in.readInt();
            int version = in.readInt();
            return (magic == MAGIC_NUMBER && version == FORMAT_VERSION);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get the timestamp from a binary save file without loading the entire file
     */
    public static long getTimestamp(String filename) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)))) {
            in.readInt(); // Skip magic number
            in.readInt(); // Skip version
            return in.readLong(); // Read timestamp
        }
    }
}
