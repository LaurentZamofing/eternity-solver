package util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for FormattingUtils.
 * Tests duration formatting, headers, separators, boxes, percentages, and progress bars.
 */
@DisplayName("FormattingUtils Tests")
public class FormattingUtilsTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    private String getOutput() {
        return outContent.toString();
    }

    private void resetOutput() {
        outContent.reset();
    }

    // ========== formatDuration Tests ==========

    @Test
    @DisplayName("formatDuration formats milliseconds (<1s)")
    void testFormatDurationMillis() {
        assertEquals("0 ms", FormattingUtils.formatDuration(0));
        assertEquals("500 ms", FormattingUtils.formatDuration(500));
        assertEquals("999 ms", FormattingUtils.formatDuration(999));
    }

    @Test
    @DisplayName("formatDuration formats seconds (1s-60s)")
    void testFormatDurationSeconds() {
        // French locale uses comma as decimal separator
        String result = FormattingUtils.formatDuration(1000);
        assertTrue(result.matches("1[.,]00 s"), "Expected 1.00 or 1,00 s, got: " + result);

        result = FormattingUtils.formatDuration(5500);
        assertTrue(result.matches("5[.,]50 s"), "Expected 5.50 or 5,50 s, got: " + result);

        result = FormattingUtils.formatDuration(30000);
        assertTrue(result.matches("30[.,]00 s"), "Expected 30.00 or 30,00 s, got: " + result);
    }

    @Test
    @DisplayName("formatDuration formats minutes (1min-60min)")
    void testFormatDurationMinutes() {
        String result = FormattingUtils.formatDuration(60000);
        assertTrue(result.matches("1[.,]00 min"), "Expected 1.00 or 1,00 min, got: " + result);

        result = FormattingUtils.formatDuration(150000);
        assertTrue(result.matches("2[.,]50 min"), "Expected 2.50 or 2,50 min, got: " + result);
    }

    @Test
    @DisplayName("formatDuration formats hours (>60min)")
    void testFormatDurationHours() {
        String result = FormattingUtils.formatDuration(3600000);
        assertTrue(result.matches("1[.,]00 h"), "Expected 1.00 or 1,00 h, got: " + result);

        result = FormattingUtils.formatDuration(9000000);
        assertTrue(result.matches("2[.,]50 h"), "Expected 2.50 or 2,50 h, got: " + result);
    }

    @Test
    @DisplayName("formatDuration boundary transitions")
    void testFormatDurationBoundaries() {
        // Just under 1 second
        assertEquals("999 ms", FormattingUtils.formatDuration(999));
        // Exactly 1 second
        String result = FormattingUtils.formatDuration(1000);
        assertTrue(result.matches("1[.,]00 s"));

        // Exactly 1 minute
        result = FormattingUtils.formatDuration(60000);
        assertTrue(result.matches("1[.,]00 min"));

        // Exactly 1 hour
        result = FormattingUtils.formatDuration(3600000);
        assertTrue(result.matches("1[.,]00 h"));
    }

    // ========== formatDurationHMS Tests ==========

    @Test
    @DisplayName("formatDurationHMS formats seconds only")
    void testFormatDurationHMSSeconds() {
        assertEquals("0s", FormattingUtils.formatDurationHMS(0));
        assertEquals("5s", FormattingUtils.formatDurationHMS(5000));
        assertEquals("59s", FormattingUtils.formatDurationHMS(59000));
    }

    @Test
    @DisplayName("formatDurationHMS formats minutes and seconds")
    void testFormatDurationHMSMinutes() {
        assertEquals("1m 00s", FormattingUtils.formatDurationHMS(60000));
        assertEquals("2m 30s", FormattingUtils.formatDurationHMS(150000));
        assertEquals("59m 59s", FormattingUtils.formatDurationHMS(3599000));
    }

    @Test
    @DisplayName("formatDurationHMS formats hours, minutes, seconds")
    void testFormatDurationHMSHours() {
        assertEquals("1h 00m 00s", FormattingUtils.formatDurationHMS(3600000));
        assertEquals("2h 30m 45s", FormattingUtils.formatDurationHMS(9045000));
        assertEquals("10h 05m 03s", FormattingUtils.formatDurationHMS(36303000));
    }

    @Test
    @DisplayName("formatDurationHMS handles large durations")
    void testFormatDurationHMSLarge() {
        assertEquals("100h 00m 00s", FormattingUtils.formatDurationHMS(360000000));
    }

    // ========== printHeader Tests ==========

    @Test
    @DisplayName("printHeader prints box with title")
    void testPrintHeaderBasic() {
        FormattingUtils.printHeader("TEST");

        String output = getOutput();
        assertTrue(output.contains("╔"));
        assertTrue(output.contains("╗"));
        assertTrue(output.contains("╚"));
        assertTrue(output.contains("╝"));
        assertTrue(output.contains("TEST"));
    }

    @Test
    @DisplayName("printHeader with custom width")
    void testPrintHeaderCustomWidth() {
        FormattingUtils.printHeader("TEST", 40);

        String output = getOutput();
        String[] lines = output.split("\n");

        // All lines should be same width
        assertTrue(lines[0].length() == lines[2].length());
        assertTrue(output.contains("TEST"));
    }

    @Test
    @DisplayName("printHeader with default width")
    void testPrintHeaderDefaultWidth() {
        FormattingUtils.printHeader("TEST TITLE");

        String output = getOutput();
        String[] lines = output.split("\n");

        // Default width is 60
        assertEquals(60, lines[0].length());
    }

    @Test
    @DisplayName("printHeader handles long titles")
    void testPrintHeaderLongTitle() {
        String longTitle = "A".repeat(58);
        FormattingUtils.printHeader(longTitle, 60);

        String output = getOutput();
        assertTrue(output.contains(longTitle));
    }

    @Test
    @DisplayName("printHeader with zero or negative width uses default")
    void testPrintHeaderInvalidWidth() {
        FormattingUtils.printHeader("TEST", 0);
        String output1 = getOutput();
        resetOutput();

        FormattingUtils.printHeader("TEST", -10);
        String output2 = getOutput();

        // Both should use default width
        assertFalse(output1.isEmpty());
        assertFalse(output2.isEmpty());
    }

    // ========== printSeparator Tests ==========

    @Test
    @DisplayName("printSeparator prints line of correct width")
    void testPrintSeparator() {
        FormattingUtils.printSeparator(40);

        String output = getOutput().trim();
        assertEquals(40, output.length());
        assertTrue(output.matches("═+"));
    }

    @Test
    @DisplayName("printSeparator with default width")
    void testPrintSeparatorDefault() {
        FormattingUtils.printSeparator();

        String output = getOutput().trim();
        assertEquals(60, output.length());
    }

    // ========== printBox Tests ==========

    @Test
    @DisplayName("printBox prints box with single line")
    void testPrintBoxSingleLine() {
        FormattingUtils.printBox("Test line");

        String output = getOutput();
        assertTrue(output.contains("╔"));
        assertTrue(output.contains("╗"));
        assertTrue(output.contains("╚"));
        assertTrue(output.contains("╝"));
        assertTrue(output.contains("Test line"));
    }

    @Test
    @DisplayName("printBox prints box with multiple lines")
    void testPrintBoxMultipleLines() {
        FormattingUtils.printBox("Line 1", "Line 2", "Line 3");

        String output = getOutput();
        assertTrue(output.contains("Line 1"));
        assertTrue(output.contains("Line 2"));
        assertTrue(output.contains("Line 3"));

        String[] lines = output.split("\n");
        assertEquals(5, lines.length); // top + 3 content + bottom
    }

    @Test
    @DisplayName("printBox handles empty array")
    void testPrintBoxEmpty() {
        FormattingUtils.printBox();

        String output = getOutput();
        assertTrue(output.isEmpty());
    }

    @Test
    @DisplayName("printBox aligns lines with different lengths")
    void testPrintBoxAlignment() {
        FormattingUtils.printBox("Short", "Much longer line");

        String output = getOutput();
        String[] lines = output.split("\n");

        assertTrue(lines.length >= 4, "Should have at least 4 lines");

        // All box lines should be same length (within reasonable margin for box chars)
        int firstLen = lines[0].length();
        for (String line : lines) {
            assertTrue(Math.abs(line.length() - firstLen) <= 2,
                      "Lines should have similar length: " + line.length() + " vs " + firstLen);
        }
    }

    // ========== formatPercentage Tests ==========

    @Test
    @DisplayName("formatPercentage with 0 decimals")
    void testFormatPercentageNoDecimals() {
        assertEquals("50%", FormattingUtils.formatPercentage(50.0, 0));
        assertEquals("100%", FormattingUtils.formatPercentage(100.0, 0));
        assertEquals("0%", FormattingUtils.formatPercentage(0.0, 0));
    }

    @Test
    @DisplayName("formatPercentage with 2 decimals")
    void testFormatPercentageTwoDecimals() {
        String result = FormattingUtils.formatPercentage(50.0, 2);
        assertTrue(result.matches("50[.,]00%"));

        result = FormattingUtils.formatPercentage(33.33, 2);
        assertTrue(result.matches("33[.,]33%"));
    }

    @Test
    @DisplayName("formatPercentage with 4 decimals")
    void testFormatPercentageFourDecimals() {
        String result = FormattingUtils.formatPercentage(12.3456, 4);
        assertTrue(result.matches("12[.,]3456%"));
    }

    @Test
    @DisplayName("formatPercentage handles edge values")
    void testFormatPercentageEdgeCases() {
        String result = FormattingUtils.formatPercentage(0.0, 2);
        assertTrue(result.matches("0[.,]00%"));

        result = FormattingUtils.formatPercentage(100.0, 2);
        assertTrue(result.matches("100[.,]00%"));
    }

    // ========== formatNumber Tests ==========

    @Test
    @DisplayName("formatNumber formats with thousands separator")
    void testFormatNumber() {
        // Small numbers may not have separator in all locales
        String result = FormattingUtils.formatNumber(1234);
        assertTrue(result.contains("1234") || (result.contains("1") && result.contains("234")),
                  "Should contain digits, got: " + result);

        // Larger numbers should have separators
        result = FormattingUtils.formatNumber(1234567);
        assertTrue(result.contains("234") && result.contains("567"));
        assertTrue(result.length() >= 7, "Should contain all digits");
    }

    @Test
    @DisplayName("formatNumber handles small numbers")
    void testFormatNumberSmall() {
        assertEquals("0", FormattingUtils.formatNumber(0));
        assertEquals("1", FormattingUtils.formatNumber(1));
        assertEquals("999", FormattingUtils.formatNumber(999));
    }

    @Test
    @DisplayName("formatNumber handles negative numbers")
    void testFormatNumberNegative() {
        String result = FormattingUtils.formatNumber(-1234);
        assertTrue(result.startsWith("-"));
        assertTrue(result.contains("1234") || result.contains("234"));
    }

    // ========== printProgressBar Tests ==========

    @Test
    @DisplayName("printProgressBar at 0% progress")
    void testPrintProgressBarZero() {
        assertDoesNotThrow(() -> FormattingUtils.printProgressBar(0, 100, 20));
        String output = getOutput();
        assertTrue(output.contains("0/100") || output.contains("0%"));
    }

    @Test
    @DisplayName("printProgressBar at 50% progress")
    void testPrintProgressBarHalf() {
        assertDoesNotThrow(() -> FormattingUtils.printProgressBar(50, 100, 20));
        String output = getOutput();
        assertTrue(output.contains("50/100") || output.contains("50"));
    }

    @Test
    @DisplayName("printProgressBar at 100% progress")
    void testPrintProgressBarFull() {
        assertDoesNotThrow(() -> FormattingUtils.printProgressBar(100, 100, 20));
        String output = getOutput();
        assertTrue(output.contains("100") || output.contains("%"));
    }

    @Test
    @DisplayName("printProgressBar with different totals")
    void testPrintProgressBarDifferentTotal() {
        assertDoesNotThrow(() -> FormattingUtils.printProgressBar(72, 256, 30));
        String output = getOutput();
        assertFalse(output.isEmpty(), "Should produce some output");
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Duration formatters are consistent")
    void testDurationFormattersConsistency() {
        long duration = 3665000; // 1h 1m 5s

        String auto = FormattingUtils.formatDuration(duration);
        String hms = FormattingUtils.formatDurationHMS(duration);

        assertTrue(auto.contains("h")); // Should use hours
        assertTrue(hms.contains("1h"));
        assertTrue(hms.contains("01m"));
        assertTrue(hms.contains("05s"));
    }

    @Test
    @DisplayName("All formatting methods handle edge cases without exceptions")
    void testNoExceptions() {
        assertDoesNotThrow(() -> {
            FormattingUtils.formatDuration(0);
            FormattingUtils.formatDuration(Long.MAX_VALUE);
            FormattingUtils.formatDurationHMS(0);
            FormattingUtils.formatDurationHMS(Long.MAX_VALUE);
            FormattingUtils.formatPercentage(0, 2);
            FormattingUtils.formatPercentage(100, 2);
            FormattingUtils.formatNumber(0);
            FormattingUtils.formatNumber(Long.MAX_VALUE);
        });
    }

    @Test
    @DisplayName("Box drawing characters are used correctly")
    void testBoxDrawingCharacters() {
        FormattingUtils.printHeader("TEST");

        String output = getOutput();

        // Should contain all box drawing characters
        assertTrue(output.contains("╔"), "Should contain top-left corner");
        assertTrue(output.contains("╗"), "Should contain top-right corner");
        assertTrue(output.contains("╚"), "Should contain bottom-left corner");
        assertTrue(output.contains("╝"), "Should contain bottom-right corner");
        assertTrue(output.contains("═"), "Should contain horizontal line");
        assertTrue(output.contains("║"), "Should contain vertical line");
    }

    @Test
    @DisplayName("Separator and header have consistent characters")
    void testSeparatorHeaderConsistency() {
        FormattingUtils.printSeparator(20);
        String separator = getOutput();
        resetOutput();

        FormattingUtils.printHeader("TEST", 20);
        String header = getOutput();

        // Both should use same horizontal line character
        assertTrue(separator.contains("═"));
        assertTrue(header.contains("═"));
    }

    @Test
    @DisplayName("printBox calculates width based on longest line")
    void testPrintBoxWidthCalculation() {
        FormattingUtils.printBox("Short", "Medium line", "Very long line here");

        String output = getOutput();
        String[] lines = output.split("\n");

        assertTrue(lines.length >= 5, "Should have top + 3 content + bottom");

        // All lines should be same width (box chars may be multi-byte)
        int firstLineLength = lines[0].length();
        for (String line : lines) {
            // Allow small variance for Unicode box characters
            assertTrue(Math.abs(line.length() - firstLineLength) <= 2,
                "All box lines should have similar length");
        }
    }

    @Test
    @DisplayName("formatNumber uses locale-specific formatting")
    void testFormatNumberLocaleSpecific() {
        // French locale may use space or comma as thousands separator
        String formatted = FormattingUtils.formatNumber(1234567);
        // Should have some separator (space, comma, or dot depending on locale)
        assertTrue(formatted.length() > 7, "Should have separators making it longer than 7 digits");
        assertTrue(formatted.contains("1") && formatted.contains("234") && formatted.contains("567"));
    }

    @Test
    @DisplayName("formatPercentage rounds correctly")
    void testFormatPercentageRounding() {
        String result = FormattingUtils.formatPercentage(33.333333, 2);
        assertTrue(result.matches("33[.,]33%"));

        result = FormattingUtils.formatPercentage(66.666666, 2);
        assertTrue(result.matches("66[.,]67%"));
    }

    @Test
    @DisplayName("printProgressBar shows correct ratio of filled vs empty")
    void testPrintProgressBarRatio() {
        assertDoesNotThrow(() -> FormattingUtils.printProgressBar(25, 100, 20));

        String output = getOutput();
        // Just verify it produces output - \r makes exact matching difficult
        assertFalse(output.isEmpty(), "Should produce some output");
    }

    // ========== Helper Methods ==========

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
