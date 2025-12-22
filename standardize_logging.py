#!/usr/bin/env python3
"""
Script to standardize logging by replacing System.out/err with SolverLogger.
"""

import re
from pathlib import Path
from typing import Tuple

def should_skip_file(file_path: Path) -> bool:
    """Determine if a file should be skipped."""
    if file_path.name == 'SolverLogger.java':
        return True
    if file_path.name == 'package-info.java':
        return True
    formatters = ['BoardFormatter', 'BoardRenderer', 'AnsiColorHelper', 'GridStructureRenderer']
    if any(f in file_path.name for f in formatters):
        return True
    return False

def has_solver_logger_import(content: str) -> bool:
    """Check if file already imports SolverLogger."""
    return bool(re.search(r'^import\s+util\.SolverLogger;', content, re.MULTILINE))

def add_solver_logger_import(content: str) -> str:
    """Add SolverLogger import after package declaration."""
    package_match = re.search(r'^package\s+[\w.]+;', content, re.MULTILINE)
    if not package_match:
        return content

    import_match = re.search(r'^import\s+', content, re.MULTILINE)
    if import_match:
        pos = import_match.start()
        return content[:pos] + "import util.SolverLogger;\n" + content[pos:]
    else:
        pos = package_match.end()
        return content[:pos] + "\n\nimport util.SolverLogger;\n" + content[pos:]

def replace_system_out_calls(content: str) -> Tuple[str, int]:
    """Replace System.out/err calls with SolverLogger. Returns (new_content, num_changes)."""
    changes = 0

    # Pattern: System.out.println(...); -> SolverLogger.info(...);
    def replace_out_println(match):
        nonlocal changes
        changes += 1
        return f"SolverLogger.info({match.group(1)});"

    content = re.sub(
        r'System\.out\.println\((.*?)\);',
        replace_out_println,
        content
    )

    # Pattern: System.err.println(...); -> SolverLogger.error(...);
    def replace_err_println(match):
        nonlocal changes
        changes += 1
        return f"SolverLogger.error({match.group(1)});"

    content = re.sub(
        r'System\.err\.println\((.*?)\);',
        replace_err_println,
        content
    )

    # Pattern: System.out.print(...); (no ln) -> SolverLogger.info(...);
    def replace_out_print(match):
        nonlocal changes
        changes += 1
        return f"SolverLogger.info({match.group(1)});"

    content = re.sub(
        r'System\.out\.print\((.*?)\);',
        replace_out_print,
        content
    )

    # Pattern: System.err.print(...); (no ln) -> SolverLogger.error(...);
    def replace_err_print(match):
        nonlocal changes
        changes += 1
        return f"SolverLogger.error({match.group(1)});"

    content = re.sub(
        r'System\.err\.print\((.*?)\);',
        replace_err_print,
        content
    )

    return content, changes

def standardize_logging(file_path: Path) -> bool:
    """Standardize logging in a file. Returns True if modified."""
    if should_skip_file(file_path):
        return False

    try:
        content = file_path.read_text(encoding='utf-8')
        original_content = content

        if not re.search(r'System\.(out|err)\.print', content):
            return False

        content, changes = replace_system_out_calls(content)

        if changes == 0:
            return False

        if not has_solver_logger_import(content):
            content = add_solver_logger_import(content)

        if content != original_content:
            file_path.write_text(content, encoding='utf-8')
            print(f"✓ Fixed: {file_path} ({changes} replacements)")
            return True

        return False

    except Exception as e:
        print(f"✗ Error processing {file_path}: {e}")
        return False

def main():
    """Main entry point."""
    root = Path('/Users/laurentzamofing/dev/eternity')
    files = list((root / 'src/main/java').rglob('*.java'))

    modified_count = 0
    for file_path in files:
        if standardize_logging(file_path):
            modified_count += 1

    print()
    print(f"Modified: {modified_count} files")

if __name__ == '__main__':
    main()
