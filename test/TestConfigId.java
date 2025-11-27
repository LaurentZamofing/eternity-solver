/**
 * Test pour vérifier l'extraction du configId depuis les noms de fichiers
 */
public class TestConfigId {

    private static String extractConfigId(String filepath) {
        java.io.File file = new java.io.File(filepath);
        String filename = file.getName();

        if (filename.startsWith("puzzle_eternity2_p")) {
            String[] parts = filename.split("_");
            if (parts.length >= 8) {
                String perm = parts[2]; // pXX
                String order = parts[7].replace(".txt", ""); // ascending/descending
                return "eternity2_" + perm + "_" + order;
            }
        }

        return "eternity2";
    }

    public static void main(String[] args) {
        System.out.println("Test d'extraction de configId");
        System.out.println("═".repeat(70));

        String[] testCases = {
            "data/puzzle_eternity2_p01_1_2_3_4_ascending.txt",
            "data/puzzle_eternity2_p12_2_4_3_1_descending.txt",
            "data/puzzle_eternity2_p24_4_3_2_1_descending.txt",
            "/Users/test/puzzle_eternity2_p05_1_4_2_3_ascending.txt"
        };

        for (String filepath : testCases) {
            String configId = extractConfigId(filepath);
            System.out.println("Fichier : " + filepath);
            System.out.println("ConfigId: " + configId);
            System.out.println("  => Nom sauvegarde: " + configId + "_current_1234567890.txt");
            System.out.println();
        }

        System.out.println("═".repeat(70));
        System.out.println("✓ Test terminé");
    }
}
