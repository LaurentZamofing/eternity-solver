#!/bin/bash
# Script pour convertir best_state.txt -> eternity2_save.txt
# Utiliser après interruption avant 10 minutes

python3 << 'EOF'
import re
from datetime import datetime

try:
    with open('saves/best_state.txt', 'r') as f:
        lines = f.readlines()

    depth = 0
    placements = []
    board_size = None
    reading_placements = False

    for line in lines:
        line = line.strip()
        if line.startswith('# Depth:'):
            depth = int(line.split(':')[1].strip())
        elif line == 'BOARD_SIZE':
            continue
        elif re.match(r'^\d+ \d+$', line) and board_size is None:
            board_size = line
        elif line == 'PLACEMENTS':
            reading_placements = True
        elif line == 'END_PLACEMENTS':
            break
        elif reading_placements and line and not line.startswith('#'):
            parts = line.split()
            if len(parts) == 4:
                r, c, pid, rot = parts
                placements.append(f"{r},{c} {pid} {rot}")  # Avec virgule!

    if not placements:
        print("❌ Aucun placement trouvé dans best_state.txt")
        exit(1)

    rows, cols = map(int, board_size.split())
    timestamp = int(datetime.now().timestamp() * 1000)
    date_str = datetime.now().strftime('%Y-%m-%d_%H-%M-%S')

    used_pieces = set()
    for p in placements:
        pid = int(p.split()[1])
        used_pieces.add(pid)

    all_pieces = set(range(1, 257))
    unused_pieces = sorted(all_pieces - used_pieces)

    with open('saves/eternity2_save.txt', 'w') as f:
        f.write("# Sauvegarde Eternity II\n")
        f.write(f"# Timestamp: {timestamp}\n")
        f.write(f"# Date: {date_str}\n")
        f.write("# Puzzle: eternity2\n")
        f.write(f"# Dimensions: {rows}x{cols}\n")
        f.write(f"# Depth: {depth}\n")
        f.write("\n")
        f.write("# Placements (row,col pieceId rotation)\n")
        for p in placements:
            f.write(f"{p}\n")
        f.write("\n")
        f.write("# Unused pieces\n")
        f.write(" ".join(map(str, unused_pieces)) + "\n")

    print(f"✅ Conversion réussie: {len(placements)} pièces sauvegardées")
    print(f"   Vous pouvez relancer: java -cp bin MainSequential")

except FileNotFoundError:
    print("❌ Fichier best_state.txt introuvable")
    exit(1)
except Exception as e:
    print(f"❌ Erreur: {e}")
    exit(1)
EOF
