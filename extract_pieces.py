#!/usr/bin/env python3
"""
Extrait les valeurs des arêtes des pièces Eternity II à partir des images.
Divise chaque pièce en 4 triangles et identifie les motifs uniques.
"""

import os
import sys
from PIL import Image
import numpy as np
from collections import defaultdict
import json

def load_image(filepath):
    """Charge une image et la convertit en array numpy"""
    img = Image.open(filepath)
    # Convertir en RGB pour s'assurer qu'il n'y a pas de canal alpha
    if img.mode != 'RGB':
        img = img.convert('RGB')
    return np.array(img)

def extract_triangle_regions(img):
    """
    Extrait les 4 régions triangulaires (N, E, S, W) d'une pièce carrée.
    Retourne un dictionnaire avec les masques pour chaque direction.
    """
    height, width = img.shape[:2]
    center_y, center_x = height // 2, width // 2

    # Créer des masques pour chaque triangle
    y_coords, x_coords = np.ogrid[:height, :width]

    # Calculer les distances aux diagonales
    # Diagonale principale: y = x
    # Diagonale anti: y = height - x

    dist_to_main_diag = y_coords - x_coords
    dist_to_anti_diag = y_coords - (height - 1 - x_coords)

    # Triangle Nord (en haut): y < center ET au-dessus des deux diagonales
    mask_north = (y_coords < center_y) & \
                 (dist_to_main_diag < 0) & \
                 (dist_to_anti_diag < 0)

    # Triangle Est (à droite): x >= center ET entre les deux diagonales
    mask_east = (x_coords >= center_x) & \
                (dist_to_main_diag < 0) & \
                (dist_to_anti_diag > 0)

    # Triangle Sud (en bas): y >= center ET en-dessous des deux diagonales
    mask_south = (y_coords >= center_y) & \
                 (dist_to_main_diag > 0) & \
                 (dist_to_anti_diag > 0)

    # Triangle Ouest (à gauche): x < center ET entre les deux diagonales
    mask_west = (x_coords < center_x) & \
                (dist_to_main_diag > 0) & \
                (dist_to_anti_diag < 0)

    return {
        'N': mask_north,
        'E': mask_east,
        'S': mask_south,
        'W': mask_west
    }

def extract_pattern_signature(img, mask):
    """
    Extrait une signature du motif dans la région masquée.
    Utilise l'histogramme de couleurs comme signature.
    """
    # Extraire les pixels de la région
    masked_pixels = img[mask]

    if len(masked_pixels) == 0:
        return None

    # Calculer l'histogramme de couleurs (bins réduits pour plus de robustesse)
    hist_r = np.histogram(masked_pixels[:, 0], bins=8, range=(0, 256))[0]
    hist_g = np.histogram(masked_pixels[:, 1], bins=8, range=(0, 256))[0]
    hist_b = np.histogram(masked_pixels[:, 2], bins=8, range=(0, 256))[0]

    # Normaliser
    hist = np.concatenate([hist_r, hist_g, hist_b])
    hist = hist / (np.sum(hist) + 1e-10)

    # Calculer aussi la couleur moyenne
    mean_color = np.mean(masked_pixels, axis=0)

    # Combiner histogramme et couleur moyenne
    signature = np.concatenate([hist, mean_color / 255.0])

    return signature

def compare_signatures(sig1, sig2):
    """Compare deux signatures et retourne une distance de similarité"""
    if sig1 is None or sig2 is None:
        return float('inf')
    return np.linalg.norm(sig1 - sig2)

def process_images(image_dir):
    """
    Traite toutes les images dans le répertoire et extrait les motifs.
    """
    # Charger toutes les images
    image_files = sorted([f for f in os.listdir(image_dir) if f.endswith('.png')])

    print(f"Traitement de {len(image_files)} images...")

    # Extraire les signatures de tous les triangles
    all_signatures = []  # [(piece_id, direction, signature), ...]

    for img_file in image_files:
        try:
            # Essayer de convertir le nom en int
            piece_id = int(img_file.replace('.png', ''))
        except ValueError:
            # Ignorer les fichiers qui ne sont pas des nombres (ex: "1x4.png")
            print(f"⚠️  Fichier ignoré (nom invalide): {img_file}")
            continue

        img_path = os.path.join(image_dir, img_file)

        img = load_image(img_path)
        masks = extract_triangle_regions(img)

        piece_sigs = {}
        for direction, mask in masks.items():
            sig = extract_pattern_signature(img, mask)
            if sig is not None:
                all_signatures.append((piece_id, direction, sig))
                piece_sigs[direction] = sig

        print(f"Pièce {piece_id:02d}: {len(piece_sigs)} triangles extraits")

    print(f"\nTotal: {len(all_signatures)} signatures extraites")

    # Clustering des signatures pour identifier les motifs uniques
    print("\nIdentification des motifs uniques...")

    pattern_clusters = []  # Liste de clusters: [[sig1, sig2, ...], ...]
    pattern_mapping = {}    # {(piece_id, direction): pattern_id}

    threshold = 0.08  # Seuil de similarité (à ajuster)

    for piece_id, direction, sig in all_signatures:
        # Chercher un cluster existant
        found_cluster = False
        for cluster_id, cluster in enumerate(pattern_clusters):
            # Comparer avec le premier élément du cluster
            ref_sig = cluster[0][2]  # signature de référence
            distance = compare_signatures(sig, ref_sig)

            if distance < threshold:
                cluster.append((piece_id, direction, sig))
                pattern_mapping[(piece_id, direction)] = cluster_id
                found_cluster = True
                break

        if not found_cluster:
            # Créer un nouveau cluster
            cluster_id = len(pattern_clusters)
            pattern_clusters.append([(piece_id, direction, sig)])
            pattern_mapping[(piece_id, direction)] = cluster_id

    print(f"Motifs uniques trouvés: {len(pattern_clusters)}")

    # Afficher les clusters
    print("\nClusters de motifs:")
    for cluster_id, cluster in enumerate(pattern_clusters):
        pieces_in_cluster = [(pid, d) for pid, d, _ in cluster]
        print(f"  Motif {cluster_id}: {len(cluster)} occurrences - {pieces_in_cluster[:5]}{'...' if len(cluster) > 5 else ''}")

    # Générer le résultat final
    print("\n" + "="*80)
    print("PIÈCES EXTRAITES (format: piece_id N E S W)")
    print("="*80)

    pieces_data = {}
    for img_file in image_files:
        try:
            piece_id = int(img_file.replace('.png', ''))
        except ValueError:
            # Ignorer les fichiers avec des noms invalides
            continue

        n = pattern_mapping.get((piece_id, 'N'), -1)
        e = pattern_mapping.get((piece_id, 'E'), -1)
        s = pattern_mapping.get((piece_id, 'S'), -1)
        w = pattern_mapping.get((piece_id, 'W'), -1)

        pieces_data[piece_id] = [n, e, s, w]
        print(f"{piece_id:3d} {n:3d} {e:3d} {s:3d} {w:3d}")

    return pieces_data, pattern_clusters

def save_results(pieces_data, output_file):
    """Sauvegarde les résultats dans un fichier"""
    with open(output_file, 'w') as f:
        f.write("# Eternity II - Pièces extraites depuis images\n")
        f.write("# Format: piece_id north east south west\n\n")
        for piece_id in sorted(pieces_data.keys()):
            edges = pieces_data[piece_id]
            f.write(f"{piece_id} {edges[0]} {edges[1]} {edges[2]} {edges[3]}\n")

    print(f"\nRésultats sauvegardés dans: {output_file}")

if __name__ == "__main__":
    image_dir = "/Users/laurentzamofing/dev/eternity/pieces/indice1"

    if len(sys.argv) > 1:
        image_dir = sys.argv[1]

    if not os.path.exists(image_dir):
        print(f"Erreur: Le répertoire {image_dir} n'existe pas!")
        sys.exit(1)

    pieces_data, clusters = process_images(image_dir)

    # Sauvegarder les résultats
    output_file = os.path.join(os.path.dirname(image_dir), "extracted_pieces.txt")
    save_results(pieces_data, output_file)

    print("\nTerminé!")
