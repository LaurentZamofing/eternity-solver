#!/usr/bin/env python3
"""
Script pour corriger les rotations des coins dans tous les fichiers de configuration.

Les pièces 1,2,3,4 ont toutes la forme [0, 0, X, Y] (coin top-right naturel).
Pour les placer correctement:
- Top-Left (0,0): rotation 3 (besoin N=0, W=0)
- Top-Right (0,15): rotation 0 (besoin N=0, E=0) ← déjà correct
- Bottom-Left (15,0): rotation 2 (besoin S=0, W=0)
- Bottom-Right (15,15): rotation 1 (besoin S=0, E=0)
"""

import os
import re

# Mapping: position -> rotation correcte
CORRECT_ROTATIONS = {
    "(0, 0)": 3,    # Top-Left
    "(0, 15)": 0,   # Top-Right
    "(15, 0)": 2,   # Bottom-Left
    "(15, 15)": 1,  # Bottom-Right
}

def fix_file(filepath):
    """Corrige les rotations dans un fichier de configuration."""
    with open(filepath, 'r') as f:
        lines = f.readlines()

    new_lines = []
    fixed_count = 0

    for line in lines:
        # Chercher les lignes PieceFixePosition pour les coins
        match = re.match(r'# PieceFixePosition: ([1-4]) (\d+) (\d+) (\d+)', line)
        if match:
            piece_id, row, col, old_rotation = match.groups()
            position = f"({row}, {col})"

            # Si c'est une position de coin, utiliser la rotation correcte
            if position in CORRECT_ROTATIONS:
                new_rotation = CORRECT_ROTATIONS[position]
                new_line = f"# PieceFixePosition: {piece_id} {row} {col} {new_rotation}\n"
                if new_line != line:
                    fixed_count += 1
                    print(f"  {filepath}: Coin {piece_id} at {position}: rotation {old_rotation} → {new_rotation}")
                new_lines.append(new_line)
            else:
                new_lines.append(line)
        else:
            new_lines.append(line)

    if fixed_count > 0:
        with open(filepath, 'w') as f:
            f.writelines(new_lines)
        return fixed_count
    return 0

def main():
    data_dir = "data"
    total_fixed = 0
    file_count = 0

    print("Correction des rotations des coins dans les fichiers de configuration...\n")

    for filename in sorted(os.listdir(data_dir)):
        if filename.startswith("puzzle_eternity2_p") and filename.endswith(".txt"):
            filepath = os.path.join(data_dir, filename)
            fixed = fix_file(filepath)
            if fixed > 0:
                file_count += 1
                total_fixed += fixed

    print(f"\n✓ {file_count} fichiers corrigés ({total_fixed} rotations modifiées)")

if __name__ == "__main__":
    main()
