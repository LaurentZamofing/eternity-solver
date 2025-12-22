#!/usr/bin/env python3
"""
Enhanced script to expand project-specific wildcard imports.
"""

import re
import sys
from pathlib import Path
from typing import Set, Dict, List

def get_project_package_classes(package: str, root: Path) -> Set[str]:
    """Get all classes in a project package by scanning files."""
    package_path = root / 'src/main/java' / package.replace('.', '/')

    if not package_path.exists():
        return set()

    classes = set()
    for java_file in package_path.glob('*.java'):
        # Extract class name from file
        class_name = java_file.stem
        classes.add(class_name)

    return classes

def find_used_classes(content: str, package: str, available_classes: Set[str]) -> Set[str]:
    """Find which classes from a package are actually used in the file."""
    # Remove strings, comments, and imports to avoid false positives
    code = re.sub(r'"(?:[^"\\]|\\.)*"', '', content)
    code = re.sub(r'//.*?$', '', code, flags=re.MULTILINE)
    code = re.sub(r'/\*.*?\*/', '', code, flags=re.DOTALL)
    code = re.sub(r'^import\s+.*?;', '', code, flags=re.MULTILINE)

    used = set()
    for class_name in available_classes:
        # Check if class is used as a word boundary
        pattern = r'\b' + re.escape(class_name) + r'\b'
        if re.search(pattern, code):
            used.add(class_name)

    return used

def expand_project_wildcards(file_path: Path, root: Path) -> bool:
    """Expand project wildcard imports. Returns True if modified."""
    try:
        content = file_path.read_text(encoding='utf-8')
        original_content = content

        # Find all wildcard imports (both static and regular)
        wildcard_pattern = r'^import\s+(static\s+)?([a-z][a-z0-9_.]*)\.\*;'
        wildcards = []

        for match in re.finditer(wildcard_pattern, content, re.MULTILINE):
            is_static = match.group(1) is not None
            package = match.group(2)
            wildcards.append((match.group(0), package, is_static))

        if not wildcards:
            return False

        modified = False
        for full_import, package, is_static in wildcards:
            # Skip java.* and javax.* packages (already handled)
            if package.startswith('java.') or package.startswith('javax.'):
                continue

            # Get available classes in this package
            available_classes = get_project_package_classes(package, root)

            if not available_classes:
                # Try with Spring/external libraries - just skip them
                if any(x in package for x in ['springframework', 'jakarta', 'org.']):
                    continue
                print(f"Warning: No classes found for package {package} in {file_path}")
                continue

            # Find which classes are actually used
            used_classes = find_used_classes(content, package, available_classes)

            if used_classes:
                # Build replacement imports
                specific_imports = []
                for cls in sorted(used_classes):
                    if is_static:
                        specific_imports.append(f"import static {package}.{cls};")
                    else:
                        specific_imports.append(f"import {package}.{cls};")

                # Replace the wildcard import
                replacement = '\n'.join(specific_imports)
                content = content.replace(full_import, replacement, 1)
                modified = True

        if modified and content != original_content:
            file_path.write_text(content, encoding='utf-8')
            print(f"✓ Fixed: {file_path}")
            return True

        return False

    except Exception as e:
        print(f"✗ Error processing {file_path}: {e}", file=sys.stderr)
        return False

def main():
    """Main entry point."""
    root = Path('/Users/laurentzamofing/dev/eternity')

    # Find files that still have wildcard imports
    files_to_check = []
    for pattern in ['src/main/java/**/*.java', 'src/test/java/**/*.java']:
        files_to_check.extend(root.glob(pattern))

    modified_count = 0
    checked_count = 0

    for file_path in files_to_check:
        content = file_path.read_text(encoding='utf-8')
        # Check if it has any non-java wildcard imports
        if re.search(r'^import\s+[a-z][a-z0-9_.]+ *\.\*;', content, re.MULTILINE):
            checked_count += 1
            if expand_project_wildcards(file_path, root):
                modified_count += 1

    print()
    print(f"Checked {checked_count} files, modified {modified_count} files")

if __name__ == '__main__':
    main()
