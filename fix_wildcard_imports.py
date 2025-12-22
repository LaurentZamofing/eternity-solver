#!/usr/bin/env python3
"""
Script to expand wildcard imports in Java files.
Analyzes each file to find which classes are actually used and replaces
wildcard imports with specific imports.
"""

import re
import subprocess
import sys
from pathlib import Path
from typing import Set, Dict, List, Tuple

def find_wildcard_imports(content: str) -> List[Tuple[str, str]]:
    """Find all wildcard imports and their packages."""
    pattern = r'^import\s+(static\s+)?(\w+(?:\.\w+)*)\.\*;'
    wildcards = []
    for match in re.finditer(pattern, content, re.MULTILINE):
        is_static = match.group(1) is not None
        package = match.group(2)
        wildcards.append((package, is_static))
    return wildcards

def find_used_symbols(content: str) -> Set[str]:
    """Extract all potential class names used in the file."""
    # Remove strings and comments to avoid false positives
    content = re.sub(r'"(?:[^"\\]|\\.)*"', '', content)
    content = re.sub(r'//.*?$', '', content, flags=re.MULTILINE)
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)

    # Find all identifiers that could be class names (start with uppercase)
    pattern = r'\b([A-Z][a-zA-Z0-9_]*)\b'
    symbols = set(re.findall(pattern, content))
    return symbols

def get_package_classes(package: str) -> Set[str]:
    """Get all classes in a package from the classpath."""
    # For java.* and javax.* packages, we can use a known list
    # For others, we'd need to inspect the classpath

    common_packages = {
        'java.util': [
            'List', 'ArrayList', 'LinkedList', 'Set', 'HashSet', 'TreeSet',
            'Map', 'HashMap', 'TreeMap', 'LinkedHashMap', 'Collection',
            'Collections', 'Arrays', 'Iterator', 'Optional', 'Objects',
            'Properties', 'Random', 'UUID', 'Date', 'Calendar', 'TimeZone',
            'Comparator', 'Comparisons', 'Queue', 'Deque', 'ArrayDeque',
            'PriorityQueue', 'Stack', 'Vector', 'Hashtable', 'Enumeration',
            'BitSet', 'Timer', 'TimerTask', 'Scanner', 'Formatter',
            'StringTokenizer', 'Dictionary', 'AbstractList', 'AbstractSet',
            'AbstractMap', 'AbstractCollection', 'AbstractQueue', 'Spliterator',
            'ListIterator', 'NavigableMap', 'NavigableSet', 'SortedMap', 'SortedSet',
            'RandomAccess', 'EventListener', 'EventObject', 'Observable', 'Observer',
            'Currency', 'Locale', 'ResourceBundle', 'PropertyResourceBundle',
            'ListResourceBundle', 'MissingResourceException', 'IllegalFormatException',
            'InputMismatchException', 'NoSuchElementException', 'ConcurrentModificationException',
            'UnsupportedOperationException', 'DuplicateFormatFlagsException',
            'UnknownFormatConversionException', 'IllegalFormatFlagsException',
            'IllegalFormatPrecisionException', 'IllegalFormatCodePointException',
            'IllegalFormatWidthException', 'FormatFlagsConversionMismatchException',
            'MissingFormatWidthException', 'MissingFormatArgumentException',
            'TooManyListenersException', 'EmptyStackException', 'ServiceLoader',
            'ServiceConfigurationError', 'Base64', 'DoubleSummaryStatistics',
            'IntSummaryStatistics', 'LongSummaryStatistics', 'OptionalDouble',
            'OptionalInt', 'OptionalLong', 'PrimitiveIterator', 'StringJoiner',
            'WeakHashMap', 'IdentityHashMap', 'EnumMap', 'EnumSet'
        ],
        'java.util.concurrent': [
            'ExecutorService', 'Executors', 'Future', 'Callable', 'TimeUnit',
            'CountDownLatch', 'CyclicBarrier', 'Semaphore', 'ConcurrentHashMap',
            'ConcurrentLinkedQueue', 'BlockingQueue', 'LinkedBlockingQueue',
            'ArrayBlockingQueue', 'PriorityBlockingQueue', 'DelayQueue',
            'SynchronousQueue', 'LinkedTransferQueue', 'ConcurrentSkipListMap',
            'ConcurrentSkipListSet', 'CopyOnWriteArrayList', 'CopyOnWriteArraySet',
            'ThreadPoolExecutor', 'ScheduledExecutorService', 'ScheduledThreadPoolExecutor',
            'ForkJoinPool', 'ForkJoinTask', 'RecursiveTask', 'RecursiveAction',
            'CompletableFuture', 'CompletionStage', 'Executor', 'Callable',
            'RunnableFuture', 'RunnableScheduledFuture', 'ScheduledFuture',
            'ExecutorCompletionService', 'Exchanger', 'Phaser', 'ThreadLocalRandom',
            'ThreadFactory', 'RejectedExecutionHandler', 'RejectedExecutionException',
            'CancellationException', 'ExecutionException', 'TimeoutException',
            'BrokenBarrierException', 'locks.Lock', 'locks.ReentrantLock',
            'locks.ReadWriteLock', 'locks.ReentrantReadWriteLock', 'locks.Condition',
            'locks.StampedLock', 'atomic.AtomicInteger', 'atomic.AtomicLong',
            'atomic.AtomicBoolean', 'atomic.AtomicReference', 'atomic.AtomicIntegerArray',
            'atomic.AtomicLongArray', 'atomic.AtomicReferenceArray', 'atomic.AtomicIntegerFieldUpdater',
            'atomic.AtomicLongFieldUpdater', 'atomic.AtomicReferenceFieldUpdater',
            'atomic.DoubleAccumulator', 'atomic.DoubleAdder', 'atomic.LongAccumulator',
            'atomic.LongAdder'
        ],
        'java.util.stream': [
            'Stream', 'Collectors', 'Collector', 'IntStream', 'LongStream',
            'DoubleStream', 'StreamSupport'
        ],
        'java.io': [
            'File', 'FileInputStream', 'FileOutputStream', 'FileReader', 'FileWriter',
            'BufferedReader', 'BufferedWriter', 'PrintWriter', 'IOException',
            'FileNotFoundException', 'InputStream', 'OutputStream', 'Reader', 'Writer',
            'ByteArrayInputStream', 'ByteArrayOutputStream', 'StringReader', 'StringWriter',
            'InputStreamReader', 'OutputStreamWriter', 'ObjectInputStream', 'ObjectOutputStream',
            'Serializable', 'Externalizable', 'DataInputStream', 'DataOutputStream',
            'RandomAccessFile', 'FileDescriptor', 'FilenameFilter', 'FileFilter',
            'EOFException', 'InterruptedIOException', 'UnsupportedEncodingException',
            'UTFDataFormatException', 'ObjectStreamException', 'InvalidObjectException',
            'InvalidClassException', 'NotSerializableException', 'OptionalDataException',
            'StreamCorruptedException', 'WriteAbortedException', 'Closeable', 'Flushable',
            'BufferedInputStream', 'BufferedOutputStream', 'PipedInputStream', 'PipedOutputStream',
            'PipedReader', 'PipedWriter', 'PushbackInputStream', 'PushbackReader',
            'SequenceInputStream', 'LineNumberReader', 'PrintStream', 'StreamTokenizer',
            'Console', 'IOError', 'UncheckedIOException'
        ],
        'java.nio.file': [
            'Path', 'Paths', 'Files', 'FileSystem', 'FileSystems', 'FileStore',
            'WatchService', 'WatchKey', 'WatchEvent', 'StandardWatchEventKinds',
            'FileVisitor', 'FileVisitResult', 'SimpleFileVisitor', 'DirectoryStream',
            'PathMatcher', 'StandardOpenOption', 'StandardCopyOption', 'LinkOption',
            'FileTime', 'attribute.FileAttribute', 'attribute.BasicFileAttributes',
            'attribute.PosixFileAttributes', 'attribute.DosFileAttributes',
            'attribute.FileAttributeView', 'attribute.BasicFileAttributeView',
            'attribute.PosixFileAttributeView', 'attribute.DosFileAttributeView',
            'InvalidPathException', 'FileSystemException', 'NoSuchFileException',
            'FileAlreadyExistsException', 'DirectoryNotEmptyException', 'AtomicMoveNotSupportedException',
            'AccessDeniedException', 'FileSystemNotFoundException', 'ProviderNotFoundException'
        ],
        'java.lang': [
            'String', 'Integer', 'Long', 'Double', 'Float', 'Boolean', 'Character',
            'Byte', 'Short', 'Object', 'Class', 'System', 'Math', 'StrictMath',
            'Thread', 'Runnable', 'ThreadGroup', 'ThreadLocal', 'InheritableThreadLocal',
            'StringBuilder', 'StringBuffer', 'CharSequence', 'Comparable', 'Cloneable',
            'Appendable', 'Readable', 'AutoCloseable', 'Iterable', 'Override', 'Deprecated',
            'SuppressWarnings', 'SafeVarargs', 'FunctionalInterface', 'Exception',
            'RuntimeException', 'Error', 'Throwable', 'NullPointerException',
            'IllegalArgumentException', 'IllegalStateException', 'UnsupportedOperationException',
            'IndexOutOfBoundsException', 'ArrayIndexOutOfBoundsException',
            'StringIndexOutOfBoundsException', 'ArithmeticException', 'ClassCastException',
            'NumberFormatException', 'InterruptedException', 'ReflectiveOperationException',
            'ClassNotFoundException', 'InstantiationException', 'IllegalAccessException',
            'NoSuchFieldException', 'NoSuchMethodException', 'CloneNotSupportedException',
            'OutOfMemoryError', 'StackOverflowError', 'AssertionError', 'Package',
            'Process', 'ProcessBuilder', 'Runtime', 'SecurityManager', 'Void',
            'Enum', 'Record', 'annotation.Annotation', 'annotation.Retention',
            'annotation.Target', 'annotation.Documented', 'annotation.Inherited',
            'annotation.Repeatable', 'annotation.RetentionPolicy', 'annotation.ElementType',
            'ref.Reference', 'ref.WeakReference', 'ref.SoftReference', 'ref.PhantomReference',
            'ref.ReferenceQueue', 'reflect.Method', 'reflect.Field', 'reflect.Constructor',
            'reflect.Modifier', 'reflect.InvocationTargetException', 'reflect.Array',
            'invoke.MethodHandle', 'invoke.MethodHandles', 'invoke.MethodType'
        ]
    }

    if package in common_packages:
        return set(common_packages[package])

    return set()

def expand_wildcards(file_path: Path) -> bool:
    """Expand wildcard imports in a single file. Returns True if modified."""
    try:
        content = file_path.read_text(encoding='utf-8')
        original_content = content

        wildcards = find_wildcard_imports(content)
        if not wildcards:
            return False

        used_symbols = find_used_symbols(content)

        # Build replacement imports for each wildcard
        for package, is_static in wildcards:
            package_classes = get_package_classes(package)
            matching_classes = used_symbols & package_classes

            if matching_classes:
                # Build specific imports
                specific_imports = []
                for cls in sorted(matching_classes):
                    if is_static:
                        specific_imports.append(f"import static {package}.{cls};")
                    else:
                        specific_imports.append(f"import {package}.{cls};")

                # Replace the wildcard import
                if is_static:
                    wildcard_pattern = f"import static {re.escape(package)}\.\\*;"
                else:
                    wildcard_pattern = f"import {re.escape(package)}\.\\*;"

                replacement = '\n'.join(specific_imports)
                content = re.sub(wildcard_pattern, replacement, content)

        # Only write if changed
        if content != original_content:
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

    # Find all Java files with wildcard imports
    result = subprocess.run(
        ['grep', '-rl', '--include=*.java', r'import.*\.\*;', str(root / 'src')],
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        print("No files with wildcard imports found or error occurred.")
        return

    files = [Path(f.strip()) for f in result.stdout.split('\n') if f.strip()]
    print(f"Found {len(files)} files with wildcard imports")
    print()

    modified_count = 0
    for file_path in files:
        if expand_wildcards(file_path):
            modified_count += 1

    print()
    print(f"Modified {modified_count}/{len(files)} files")

if __name__ == '__main__':
    main()
