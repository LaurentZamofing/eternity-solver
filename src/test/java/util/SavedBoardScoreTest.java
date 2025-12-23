package util;

import model.Board;
import model.Piece;
import util.PuzzleFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

/**
 * Test pour calculer le score d'une sauvegarde existante.
 */
public class SavedBoardScoreTest {
    public static void main(String[] args) {
        try {
            // Charger les pièces Eternity II
            Map<Integer, Piece> allPieces = PuzzleFactory.createEternityII();

            // Créer un board et charger la sauvegarde thread_3
            Board board = new Board(16, 16);

            BufferedReader reader = new BufferedReader(new FileReader("saves/thread_3.txt"));
            String line;
            boolean inPlacements = false;
            int piecesCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.equals("PLACEMENTS")) {
                    inPlacements = true;
                    continue;
                }

                if (line.equals("END_PLACEMENTS")) {
                    break;
                }

                if (inPlacements && !line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("\\s+");
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    int pieceId = Integer.parseInt(parts[2]);
                    int rotation = Integer.parseInt(parts[3]);

                    Piece piece = allPieces.get(pieceId);
                    if (piece != null) {
                        board.place(r, c, piece, rotation);
                        piecesCount++;
                    }
                }
            }
            reader.close();

            // Afficher le score
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║        SCORE DE LA SAUVEGARDE THREAD_3                 ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println("Pièces placées: " + piecesCount + "/256\n");

            board.printScore();

        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de la sauvegarde:");
            e.printStackTrace();
        }
    }
}
