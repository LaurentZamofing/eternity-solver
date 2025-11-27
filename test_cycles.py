#!/usr/bin/env python3
"""Test 10 cycles de save/restore pour identifier les erreurs"""
import subprocess
import time
import os
import sys

def run_cycle(cycle_num):
    print(f"\n{'='*70}")
    print(f"  CYCLE {cycle_num}/10")
    print(f"{'='*70}")

    # Lancer MainSequential
    proc = subprocess.Popen(
        ['java', '-cp', 'bin', 'MainSequential'],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        cwd='/Users/laurentzamofing/dev/eternity'
    )

    # Capturer les 30 premières lignes
    lines = []
    try:
        for i in range(30):
            line = proc.stdout.readline()
            if not line:
                break
            lines.append(line.rstrip())
            print(line.rstrip())
    except:
        pass

    # Attendre 3 secondes
    time.sleep(3)

    # Tuer le processus
    proc.terminate()
    try:
        proc.wait(timeout=2)
    except subprocess.TimeoutExpired:
        proc.kill()
        proc.wait()

    # Petite pause avant le cycle suivant
    time.sleep(1)

    return lines

def main():
    os.chdir('/Users/laurentzamofing/dev/eternity')

    # Supprimer la sauvegarde existante
    try:
        os.remove('saves/eternity2_save.txt')
        print("Sauvegarde existante supprimée")
    except FileNotFoundError:
        print("Pas de sauvegarde existante")

    # Exécuter 10 cycles
    for i in range(1, 11):
        try:
            run_cycle(i)
        except KeyboardInterrupt:
            print("\n\nInterrompu par l'utilisateur")
            sys.exit(1)
        except Exception as e:
            print(f"\nERREUR au cycle {i}: {e}")

    print(f"\n{'='*70}")
    print("  TEST TERMINÉ - 10 cycles complétés")
    print(f"{'='*70}")

    # Vérifier l'état final
    if os.path.exists('saves/eternity2_save.txt'):
        with open('saves/eternity2_save.txt', 'r') as f:
            for line in f:
                if line.startswith('# Depth:'):
                    print(f"\nÉtat final: {line.strip()}")
                    break

if __name__ == '__main__':
    main()
