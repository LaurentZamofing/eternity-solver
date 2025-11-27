package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour CommandLineInterface
 */
public class CommandLineInterfaceTest {

    private CommandLineInterface cli;

    @BeforeEach
    public void setUp() {
        cli = new CommandLineInterface();
    }

    @Test
    public void testHelpOption() {
        String[] args = {"--help"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isHelpRequested());
    }

    @Test
    public void testHelpShortOption() {
        String[] args = {"-h"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isHelpRequested());
    }

    @Test
    public void testVersionOption() {
        String[] args = {"--version"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isVersionRequested());
    }

    @Test
    public void testVerboseOption() {
        String[] args = {"--verbose"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isVerbose());
        assertFalse(cli.isQuiet());
    }

    @Test
    public void testVerboseShortOption() {
        String[] args = {"-v"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isVerbose());
    }

    @Test
    public void testQuietOption() {
        String[] args = {"--quiet"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isQuiet());
        assertFalse(cli.isVerbose());
    }

    @Test
    public void testParallelOption() {
        String[] args = {"--parallel"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isParallel());
    }

    @Test
    public void testPuzzleWithEquals() {
        String[] args = {"--puzzle=puzzle_6x12"};
        assertTrue(cli.parse(args));
        assertEquals("puzzle_6x12", cli.getPuzzleName());
    }

    @Test
    public void testPuzzleWithSpace() {
        String[] args = {"--puzzle", "puzzle_6x12"};
        assertTrue(cli.parse(args));
        assertEquals("puzzle_6x12", cli.getPuzzleName());
    }

    @Test
    public void testPuzzlePositional() {
        String[] args = {"puzzle_6x12"};
        assertTrue(cli.parse(args));
        assertEquals("puzzle_6x12", cli.getPuzzleName());
    }

    @Test
    public void testThreadsWithEquals() {
        String[] args = {"--threads=8"};
        assertTrue(cli.parse(args));
        assertEquals(8, cli.getThreads());
    }

    @Test
    public void testThreadsWithSpace() {
        String[] args = {"--threads", "8"};
        assertTrue(cli.parse(args));
        assertEquals(8, cli.getThreads());
    }

    @Test
    public void testThreadsShortOption() {
        String[] args = {"-t", "8"};
        assertTrue(cli.parse(args));
        assertEquals(8, cli.getThreads());
    }

    @Test
    public void testThreadsInvalidValue() {
        String[] args = {"--threads", "abc"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testThreadsNegativeValue() {
        String[] args = {"--threads", "-5"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testThreadsZeroValue() {
        String[] args = {"--threads", "0"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testTimeoutWithEquals() {
        String[] args = {"--timeout=3600"};
        assertTrue(cli.parse(args));
        assertEquals(3600, cli.getTimeout());
    }

    @Test
    public void testTimeoutWithSpace() {
        String[] args = {"--timeout", "3600"};
        assertTrue(cli.parse(args));
        assertEquals(3600, cli.getTimeout());
    }

    @Test
    public void testMinDepthWithEquals() {
        String[] args = {"--min-depth=10"};
        assertTrue(cli.parse(args));
        assertEquals(10, cli.getMinDepth());
    }

    @Test
    public void testMinDepthZeroAllowed() {
        String[] args = {"--min-depth", "0"};
        assertTrue(cli.parse(args));
        assertEquals(0, cli.getMinDepth());
    }

    @Test
    public void testMinDepthNegativeNotAllowed() {
        String[] args = {"--min-depth", "-5"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testNoSingletonsOption() {
        String[] args = {"--no-singletons"};
        assertTrue(cli.parse(args));
        assertFalse(cli.useSingletons());
    }

    @Test
    public void testSingletonsDefaultTrue() {
        String[] args = {};
        assertTrue(cli.parse(args));
        assertTrue(cli.useSingletons());
    }

    @Test
    public void testMultipleOptions() {
        String[] args = {"--verbose", "--parallel", "--threads", "4", "puzzle_6x12"};
        assertTrue(cli.parse(args));
        assertTrue(cli.isVerbose());
        assertTrue(cli.isParallel());
        assertEquals(4, cli.getThreads());
        assertEquals("puzzle_6x12", cli.getPuzzleName());
    }

    @Test
    public void testUnknownLongOption() {
        String[] args = {"--unknown"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
        assertTrue(cli.getErrorMessage().contains("unknown"));
    }

    @Test
    public void testUnknownShortOption() {
        String[] args = {"-x"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testMissingPuzzleOptionValue() {
        String[] args = {"--puzzle"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testMissingThreadsOptionValue() {
        String[] args = {"--threads"};
        assertFalse(cli.parse(args));
        assertTrue(cli.hasError());
    }

    @Test
    public void testEmptyArgs() {
        String[] args = {};
        assertTrue(cli.parse(args));
        assertNull(cli.getPuzzleName());
        assertFalse(cli.isVerbose());
        assertFalse(cli.isParallel());
    }

    @Test
    public void testComplexCombination() {
        String[] args = {
            "-v", "-p",
            "--puzzle=eternity2_p1",
            "--threads", "16",
            "--timeout=7200",
            "--min-depth=100",
            "--no-singletons"
        };
        assertTrue(cli.parse(args));
        assertTrue(cli.isVerbose());
        assertTrue(cli.isParallel());
        assertEquals("eternity2_p1", cli.getPuzzleName());
        assertEquals(16, cli.getThreads());
        assertEquals(7200, cli.getTimeout());
        assertEquals(100, cli.getMinDepth());
        assertFalse(cli.useSingletons());
    }
}
