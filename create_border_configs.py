#!/usr/bin/env python3
"""
Crée des versions "border" de toutes les configurations Eternity2
avec l'option PrioritizeBorders activée
"""
import os
import glob

data_dir = "data"
pattern = os.path.join(data_dir, "puzzle_eternity2_p*_*.txt")

for filepath in glob.glob(pattern):
    # Skip si c'est déjà une version border
    if "_border.txt" in filepath:
        continue

    # Créer le nouveau nom
    newpath = filepath.replace(".txt", "_border.txt")

    # Lire le fichier original
    with open(filepath, 'r') as f:
        lines = f.readlines()

    # Insérer l'option PrioritizeBorders après SeuillAffichage
    new_lines = []
    for line in lines:
        new_lines.append(line)
        if line.startswith("# SeuillAffichage:"):
            new_lines.append("# PrioritizeBorders: true\n")

    # Modifier aussi la première ligne pour indiquer BORDER
    if new_lines[0].startswith("#"):
        new_lines[0] = new_lines[0].rstrip() + " - BORDER\n"

    # Écrire le nouveau fichier
    with open(newpath, 'w') as f:
        f.writelines(new_lines)

    print(f"Créé: {os.path.basename(newpath)}")

print("\nTerminé! Configurations 'border' créées.")
