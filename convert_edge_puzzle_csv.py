#!/usr/bin/env python3
"""
Convert edge_puzzle CSV format (E,S,W,N) to our format (N,E,S,W).

Usage:
    python3 convert_edge_puzzle_csv.py edge_puzzle.csv output.txt
"""

import sys
import csv

def convert_csv_to_nesw(input_csv, output_txt):
    """Convert edge_puzzle CSV (E,S,W,N) to our TXT format (N E S W)."""

    with open(input_csv, 'r') as f:
        reader = csv.reader(f)

        # Skip header line (16,16,5,17,)
        header = next(reader)
        print(f"Header: {header}")
        print(f"Board: {header[0]}×{header[1]}, Patterns: edge={header[2]}, inner={header[3]}")

        pieces = []
        for row in reader:
            if len(row) >= 4:
                piece_id = row[0]
                east = row[1] if row[1] else '0'
                south = row[2] if row[2] else '0'
                west = row[3] if row[3] else '0'
                north = row[4] if len(row) > 4 and row[4] else '0'

                # Convert to our format: N E S W
                pieces.append((piece_id, north, east, south, west))

    # Write to output file
    with open(output_txt, 'w') as f:
        f.write(f"# Eternity II - {header[0]}×{header[1]} - Converted from edge_puzzle CSV\n")
        f.write(f"# Format: pieceId N E S W\n")
        f.write(f"# Total pieces: {len(pieces)}\n")
        f.write(f"#\n")

        for piece_id, n, e, s, w in pieces:
            f.write(f"{piece_id} {n} {e} {s} {w}\n")

    print(f"✓ Converted {len(pieces)} pieces")
    print(f"✓ Output: {output_txt}")

    # Print first few pieces for verification
    print(f"\nFirst 3 pieces:")
    for i in range(min(3, len(pieces))):
        pid, n, e, s, w = pieces[i]
        print(f"  Piece {pid}: N={n} E={e} S={s} W={w}")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python3 convert_edge_puzzle_csv.py input.csv output.txt")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2]

    convert_csv_to_nesw(input_file, output_file)
