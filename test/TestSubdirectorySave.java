import model.Board;
import model.Piece;
import util.SaveStateManager;
import util.SaveStateManager.PlacementInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Test de création des sous-répertoires et de l'affichage visuel
 */
public class TestSubdirectorySave {

    public static void main(String[] args) {
        System.out.println("Test de sauvegarde avec sous-répertoires et affichage visuel");
        System.out.println("=".repeat(70));

        try {
            // Créer un board 5x5 de test
            Board board = new Board(5, 5);

            // Créer quelques pièces de test
            Map<Integer, Piece> allPieces = new HashMap<>();
            for (int i = 1; i <= 10; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, 1, 2, 3}));
            }

            // Placer quelques pièces
            board.place(0, 0, allPieces.get(1), 0);
            board.place(0, 1, allPieces.get(2), 1);
            board.place(1, 0, allPieces.get(3), 2);
            board.place(2, 2, allPieces.get(4), 3);

            // Liste de placement pour le backtracking
            List<PlacementInfo> placementOrder = new ArrayList<>();
            placementOrder.add(new PlacementInfo(0, 0, 1, 0));
            placementOrder.add(new PlacementInfo(0, 1, 2, 1));
            placementOrder.add(new PlacementInfo(1, 0, 3, 2));
            placementOrder.add(new PlacementInfo(2, 2, 4, 3));

            // Liste des pièces non utilisées
            List<Integer> unusedIds = Arrays.asList(5, 6, 7, 8, 9, 10);

            // Test 1: Eternity2
            System.out.println("\n📁 Test 1: Sauvegarde eternity2_p01_ascending");
            SaveStateManager.saveState("eternity2_p01_ascending", board, allPieces, unusedIds, placementOrder);

            // Vérifier que le sous-répertoire existe
            File eternity2Dir = new File("saves/eternity2/");
            if (eternity2Dir.exists() && eternity2Dir.isDirectory()) {
                System.out.println("✓ Sous-répertoire saves/eternity2/ créé");

                // Lister les fichiers
                File[] files = eternity2Dir.listFiles();
                if (files != null && files.length > 0) {
                    System.out.println("✓ Fichiers créés:");
                    for (File f : files) {
                        System.out.println("  - " + f.getName());

                        // Lire et afficher les premières lignes du fichier
                        System.out.println("\n📄 Contenu du fichier:");
                        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                            String line;
                            int lineCount = 0;
                            while ((line = reader.readLine()) != null && lineCount < 30) {
                                System.out.println(line);
                                lineCount++;
                            }
                            if (lineCount >= 30) {
                                System.out.println("... (fichier tronqué)");
                            }
                        }
                    }
                } else {
                    System.out.println("✗ Aucun fichier trouvé dans saves/eternity2/");
                }
            } else {
                System.out.println("✗ Sous-répertoire saves/eternity2/ NON créé!");
            }

            // Test 2: Indice
            System.out.println("\n📁 Test 2: Sauvegarde indice1");
            SaveStateManager.saveState("indice1", board, allPieces, unusedIds, placementOrder);

            File indice1Dir = new File("saves/indice1/");
            if (indice1Dir.exists() && indice1Dir.isDirectory()) {
                System.out.println("✓ Sous-répertoire saves/indice1/ créé");

                File[] files = indice1Dir.listFiles();
                if (files != null && files.length > 0) {
                    System.out.println("✓ Fichiers créés:");
                    for (File f : files) {
                        System.out.println("  - " + f.getName());
                    }
                } else {
                    System.out.println("✗ Aucun fichier trouvé dans saves/indice1/");
                }
            } else {
                System.out.println("✗ Sous-répertoire saves/indice1/ NON créé!");
            }

            System.out.println("\n" + "=".repeat(70));
            System.out.println("✅ Test terminé");

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
