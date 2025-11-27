#!/usr/bin/env python3
"""
Extrait les valeurs des arêtes des pièces Eternity II en utilisant les formes de référence.
Compare chaque triangle avec les 22 motifs officiels + bordure (0).
"""

import os
import sys
from PIL import Image
import numpy as np
from collections import defaultdict

# Chemin vers les formes de référence
FORMES_DIR = "/Users/laurentzamofing/dev/eternity/pieces/Formes"

def load_image(filepath):
    """Charge une image et la convertit en array numpy RGB"""
    img = Image.open(filepath)
    if img.mode != 'RGB':
        img = img.convert('RGB')
    return np.array(img)

def extract_triangle_regions(img):
    """
    Extrait les 4 régions triangulaires (N, E, S, W) d'une pièce carrée.
    """
    height, width = img.shape[:2]
    center_y, center_x = height // 2, width // 2

    y_coords, x_coords = np.ogrid[:height, :width]

    dist_to_main_diag = y_coords - x_coords
    dist_to_anti_diag = y_coords - (height - 1 - x_coords)

    mask_north = (y_coords < center_y) & (dist_to_main_diag < 0) & (dist_to_anti_diag < 0)
    mask_east = (x_coords >= center_x) & (dist_to_main_diag < 0) & (dist_to_anti_diag > 0)
    mask_south = (y_coords >= center_y) & (dist_to_main_diag > 0) & (dist_to_anti_diag > 0)
    mask_west = (x_coords < center_x) & (dist_to_main_diag > 0) & (dist_to_anti_diag < 0)

    return {
        'N': mask_north,
        'E': mask_east,
        'S': mask_south,
        'W': mask_west
    }

def extract_pattern_signature(img, mask):
    """Extrait une signature du motif dans la région masquée, en excluant les pixels noirs"""
    masked_pixels = img[mask]

    if len(masked_pixels) == 0:
        return None

    # Exclure les pixels noirs/très sombres (fond et séparateur X)
    # Un pixel est considéré comme noir si la somme RGB < seuil
    brightness = np.sum(masked_pixels, axis=1)
    non_black_mask = brightness > 80  # Seuil: somme RGB > 80 (sur 765 max)

    # Filtrer les pixels non-noirs
    colored_pixels = masked_pixels[non_black_mask]

    if len(colored_pixels) < 10:  # Pas assez de pixels colorés
        return None

    # Histogramme de couleurs avec plus de bins pour plus de précision
    hist_r = np.histogram(colored_pixels[:, 0], bins=32, range=(0, 256))[0]
    hist_g = np.histogram(colored_pixels[:, 1], bins=32, range=(0, 256))[0]
    hist_b = np.histogram(colored_pixels[:, 2], bins=32, range=(0, 256))[0]

    # Normaliser
    hist = np.concatenate([hist_r, hist_g, hist_b])
    hist = hist / (np.sum(hist) + 1e-10)

    # Couleur moyenne (des pixels colorés seulement)
    mean_color = np.mean(colored_pixels, axis=0)

    # Déviation standard des couleurs
    std_color = np.std(colored_pixels, axis=0)

    # Signature complète
    signature = np.concatenate([hist, mean_color / 255.0, std_color / 255.0])

    return signature

def load_reference_forms():
    """Charge les 22 formes de référence + bordure grise"""

    print("Chargement des formes de référence...")

    reference_signatures = {}

    # Forme 0: Bordure grise (signature synthétique)
    # Signature pour un triangle gris uniforme
    gray_hist = np.zeros(96)  # 32*3 bins
    gray_hist[:] = 1.0 / 96  # Distribution uniforme
    gray_mean = np.array([0.5, 0.5, 0.5])  # Gris moyen
    gray_std = np.array([0.05, 0.05, 0.05])   # Très faible variance
    reference_signatures[0] = np.concatenate([gray_hist, gray_mean, gray_std])
    print(f"  Forme 0 (bordure): signature synthétique créée")

    # Charger les 22 formes de référence
    for form_id in range(1, 23):
        form_path = os.path.join(FORMES_DIR, f"{form_id:02d}.png")

        if not os.path.exists(form_path):
            print(f"  ⚠️  Forme {form_id} non trouvée: {form_path}")
            continue

        try:
            # Charger l'image de la forme
            form_img = load_image(form_path)

            # La forme de référence est juste un triangle, pas besoin de masque
            # On extrait sa signature directement
            # Créer un masque simple pour le triangle visible
            h, w = form_img.shape[:2]
            y_coords, x_coords = np.ogrid[:h, :w]

            # Masque pour le triangle (approximatif - toute la zone non-noire)
            # Détection du fond noir
            is_not_black = np.sum(form_img, axis=2) > 30

            if np.sum(is_not_black) > 0:
                pixels = form_img[is_not_black]

                # Calculer la signature avec plus de bins
                hist_r = np.histogram(pixels[:, 0], bins=32, range=(0, 256))[0]
                hist_g = np.histogram(pixels[:, 1], bins=32, range=(0, 256))[0]
                hist_b = np.histogram(pixels[:, 2], bins=32, range=(0, 256))[0]

                hist = np.concatenate([hist_r, hist_g, hist_b])
                hist = hist / (np.sum(hist) + 1e-10)

                mean_color = np.mean(pixels, axis=0) / 255.0
                std_color = np.std(pixels, axis=0) / 255.0

                signature = np.concatenate([hist, mean_color, std_color])
                reference_signatures[form_id] = signature
                print(f"  Forme {form_id}: signature extraite")
            else:
                print(f"  ⚠️  Forme {form_id}: aucun pixel valide")

        except Exception as e:
            print(f"  ❌ Erreur forme {form_id}: {e}")

    print(f"\nTotal: {len(reference_signatures)} formes de référence chargées (0-{max(reference_signatures.keys())})")
    return reference_signatures

def find_best_match(signature, reference_signatures):
    """Trouve la meilleure correspondance avec les formes de référence"""

    if signature is None:
        return -1

    best_match = -1
    best_distance = float('inf')

    for form_id, ref_sig in reference_signatures.items():
        distance = np.linalg.norm(signature - ref_sig)

        if distance < best_distance:
            best_distance = distance
            best_match = form_id

    return best_match

def process_images(image_dir, reference_signatures):
    """Traite toutes les images et identifie les motifs"""

    image_files = sorted([f for f in os.listdir(image_dir) if f.endswith('.png')])

    print(f"\nTraitement de {len(image_files)} images...")

    pieces_data = {}
    match_distances = []  # Pour statistiques

    for img_file in image_files:
        try:
            piece_id = int(img_file.replace('.png', ''))
        except ValueError:
            print(f"⚠️  Fichier ignoré (nom invalide): {img_file}")
            continue

        img_path = os.path.join(image_dir, img_file)
        img = load_image(img_path)
        masks = extract_triangle_regions(img)

        piece_edges = {}

        for direction in ['N', 'E', 'S', 'W']:
            sig = extract_pattern_signature(img, masks[direction])
            match = find_best_match(sig, reference_signatures)
            piece_edges[direction] = match

        pieces_data[piece_id] = piece_edges

        edges_str = f"N={piece_edges['N']:2d} E={piece_edges['E']:2d} S={piece_edges['S']:2d} W={piece_edges['W']:2d}"
        print(f"Pièce {piece_id:02d}: {edges_str}")

    return pieces_data

def save_results(pieces_data, output_file):
    """Sauvegarde les résultats"""

    with open(output_file, 'w') as f:
        f.write("# Eternity II - Pièces extraites avec formes de référence\n")
        f.write("# Format: piece_id north east south west\n")
        f.write("# 0 = bordure (gris), 1-22 = motifs officiels\n\n")

        for piece_id in sorted(pieces_data.keys()):
            edges = pieces_data[piece_id]
            f.write(f"{piece_id} {edges['N']} {edges['E']} {edges['S']} {edges['W']}\n")

    print(f"\n✅ Résultats sauvegardés dans: {output_file}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 extract_pieces_with_templates.py <image_directory>")
        sys.exit(1)

    image_dir = sys.argv[1]

    if not os.path.exists(image_dir):
        print(f"Erreur: Le répertoire {image_dir} n'existe pas!")
        sys.exit(1)

    # Charger les formes de référence
    reference_signatures = load_reference_forms()

    if len(reference_signatures) < 10:
        print("⚠️  Attention: Moins de 10 formes de référence chargées!")

    # Traiter les images
    pieces_data = process_images(image_dir, reference_signatures)

    # Sauvegarder les résultats
    output_file = os.path.join(os.path.dirname(image_dir), "extracted_with_templates.txt")
    save_results(pieces_data, output_file)

    # Statistiques
    print(f"\n{'='*80}")
    print(f"STATISTIQUES")
    print(f"{'='*80}")
    print(f"Pièces traitées: {len(pieces_data)}")

    # Compter l'utilisation de chaque motif
    motif_counts = defaultdict(int)
    for edges in pieces_data.values():
        for direction in ['N', 'E', 'S', 'W']:
            motif_counts[edges[direction]] += 1

    print(f"\nUtilisation des motifs:")
    for motif in sorted(motif_counts.keys()):
        if motif == 0:
            print(f"  Motif 0 (bordure): {motif_counts[motif]} occurrences")
        else:
            print(f"  Motif {motif:2d}: {motif_counts[motif]} occurrences")

    print("\nTerminé!")

if __name__ == "__main__":
    main()
