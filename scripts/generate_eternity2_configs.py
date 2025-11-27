#!/usr/bin/env python3
"""
Génère les fichiers de configuration pour Eternity II avec toutes les combinaisons de coins.
4 pièces de coin (1, 2, 3, 4) -> 4! = 24 permutations
Pour chaque permutation: 2 ordres de tri (croissant/décroissant) -> 48 fichiers au total
"""

import itertools
import os

# Les 4 pièces de coin d'Eternity II
CORNER_PIECES = [1, 2, 3, 4]

# Les positions des coins dans un puzzle 16x16 (indices 0-based)
CORNER_POSITIONS = [
    (0, 0),   # Haut-gauche
    (0, 15),  # Haut-droit
    (15, 0),  # Bas-gauche
    (15, 15)  # Bas-droit
]

# Rotations pour chaque position de coin
# Pour placer une pièce avec (N=0, E=0) aux différents coins:
CORNER_ROTATIONS = [
    0,  # Haut-gauche: N=0, W=0 -> rotation 0 (N=0, E=0, S=x, W=x) needs N=0, W=0 so rotation 0
    1,  # Haut-droit: N=0, E=0 -> rotation 1 (N=x, E=0, S=x, W=0)
    3,  # Bas-gauche: S=0, W=0 -> rotation 3 (N=x, E=x, S=0, W=0)
    2   # Bas-droit: S=0, E=0 -> rotation 2 (N=x, E=x, S=0, W=0)
]

# Lire le fichier de base
def read_base_file():
    with open('data/puzzle_eternity2.txt', 'r') as f:
        lines = f.readlines()

    # Séparer les métadonnées des pièces
    metadata_lines = []
    piece_lines = []

    for line in lines:
        if line.strip().startswith('#') or line.strip() == '':
            if 'PieceFixePosition' not in line:  # On va remplacer ces lignes
                metadata_lines.append(line)
        else:
            piece_lines.append(line)

    return metadata_lines, piece_lines

def create_config_file(perm_index, corner_perm, sort_order):
    """
    Crée un fichier de configuration avec une permutation donnée des coins
    perm_index: numéro de la permutation (1-24)
    corner_perm: tuple de 4 pièces (permutation des coins)
    sort_order: 'ascending' ou 'descending'
    """
    metadata_lines, piece_lines = read_base_file()

    # Nom du fichier
    corner_str = '_'.join(map(str, corner_perm))
    filename = f'data/puzzle_eternity2_p{perm_index:02d}_{corner_str}_{sort_order}.txt'

    with open(filename, 'w') as f:
        # Écrire le nom modifié
        f.write(f'# Eternity II (16×16) - Permutation {perm_index} - {sort_order.upper()}\n')

        # Écrire les autres métadonnées (sans le nom original)
        for line in metadata_lines:
            if not line.strip().startswith('# Eternity II'):
                f.write(line)

        # Ajouter l'ordre de tri
        f.write(f'# SortOrder: {sort_order}\n')

        # Ajouter les pièces fixes (les 4 coins)
        f.write('# Pièces fixes (coins dans cette permutation + pièces fixes originales):\n')
        for i, piece_id in enumerate(corner_perm):
            row, col = CORNER_POSITIONS[i]
            rotation = CORNER_ROTATIONS[i]
            f.write(f'# PieceFixePosition: {piece_id} {row} {col} {rotation}\n')

        # Ajouter les 5 pièces fixes originales d'Eternity II
        # (reprendre exactement les mêmes coordonnées que le fichier original)
        f.write('# PieceFixePosition: 139 8 7 0\n')
        f.write('# PieceFixePosition: 181 13 2 3\n')
        f.write('# PieceFixePosition: 255 2 13 3\n')
        f.write('# PieceFixePosition: 249 13 13 0\n')
        f.write('# PieceFixePosition: 208 2 2 3\n')
        f.write('#\n')

        # Écrire les pièces
        for line in piece_lines:
            f.write(line)

    print(f'Créé: {filename}')
    return filename

def main():
    print('Génération des fichiers de configuration Eternity II...')
    print(f'4 pièces de coin: {CORNER_PIECES}')
    print(f'Nombre de permutations: 4! = 24')
    print(f'Nombre de fichiers à créer: 24 × 2 (ascending/descending) = 48')
    print()

    # Générer toutes les permutations des 4 coins
    all_perms = list(itertools.permutations(CORNER_PIECES))

    created_files = []

    # Pour chaque permutation
    for perm_index, perm in enumerate(all_perms, 1):
        # Créer les deux versions (ascending et descending)
        for sort_order in ['ascending', 'descending']:
            filename = create_config_file(perm_index, perm, sort_order)
            created_files.append(filename)

    print()
    print(f'✓ {len(created_files)} fichiers créés avec succès!')
    print()
    print('Exemples:')
    for i in range(min(5, len(created_files))):
        print(f'  - {created_files[i]}')
    if len(created_files) > 5:
        print(f'  ... et {len(created_files) - 5} autres')

if __name__ == '__main__':
    main()
