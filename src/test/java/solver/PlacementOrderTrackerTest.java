package solver;

import org.junit.jupiter.api.*;
import util.SaveStateManager.PlacementInfo;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PlacementOrderTracker.
 * Tests placement history tracking, backtracking support, and state restoration.
 *
 * Critical for ensuring solver can properly track and restore game state.
 */
@DisplayName("PlacementOrderTracker Tests")
class PlacementOrderTrackerTest {

    private PlacementOrderTracker tracker;

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create tracker with default constructor")
    void testDefaultConstructor() {
        tracker = new PlacementOrderTracker();

        assertNotNull(tracker, "Tracker should not be null");
        assertEquals(0, tracker.getCurrentDepth(), "Initial depth should be 0");
        assertTrue(tracker.getInitialFixedPieces().isEmpty(),
                  "Initial fixed pieces should be empty");
    }

    @Test
    @DisplayName("Should create tracker with initial fixed pieces")
    void testConstructorWithFixedPieces() {
        List<PlacementInfo> fixedPieces = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0)
        );

        tracker = new PlacementOrderTracker(fixedPieces);

        assertNotNull(tracker, "Tracker should not be null");
        assertEquals(2, tracker.getInitialFixedPieces().size(),
                    "Should store fixed pieces");
        assertEquals(0, tracker.getCurrentDepth(),
                    "Depth should be 0 before initialization");
    }

    @Test
    @DisplayName("Should handle null fixed pieces in constructor")
    void testConstructorWithNullFixedPieces() {
        tracker = new PlacementOrderTracker(null);

        assertNotNull(tracker, "Tracker should not be null");
        assertEquals(0, tracker.getInitialFixedPieces().size(),
                    "Should have empty fixed pieces list");
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Should initialize empty history")
    void testInitialize() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        assertTrue(tracker.isTracking(), "Should be tracking after initialization");
        assertEquals(0, tracker.getCurrentDepth(), "Depth should be 0");
    }

    @Test
    @DisplayName("Should initialize with fixed pieces")
    void testInitializeWithFixedPieces() {
        tracker = new PlacementOrderTracker();

        List<PlacementInfo> fixedPieces = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0),
            new PlacementInfo(0, 2, 3, 0)
        );

        tracker.initializeWithFixedPieces(fixedPieces);

        assertTrue(tracker.isTracking(), "Should be tracking");
        assertEquals(3, tracker.getCurrentDepth(), "Should have 3 fixed pieces");
    }

    @Test
    @DisplayName("Should handle null in initializeWithFixedPieces")
    void testInitializeWithNullFixedPieces() {
        tracker = new PlacementOrderTracker();

        assertDoesNotThrow(() -> {
            tracker.initializeWithFixedPieces(null);
        }, "Should handle null fixed pieces");

        assertTrue(tracker.isTracking(), "Should still be tracking");
        assertEquals(0, tracker.getCurrentDepth(), "Depth should be 0");
    }

    @Test
    @DisplayName("Should initialize with preloaded history")
    void testInitializeWithHistory() {
        tracker = new PlacementOrderTracker();

        List<PlacementInfo> preloadedOrder = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0),
            new PlacementInfo(1, 0, 3, 0),
            new PlacementInfo(1, 1, 4, 0)
        );

        tracker.initializeWithHistory(preloadedOrder);

        assertTrue(tracker.isTracking(), "Should be tracking");
        assertEquals(4, tracker.getCurrentDepth(), "Should have 4 placements");

        List<PlacementInfo> history = tracker.getPlacementHistory();
        assertEquals(4, history.size(), "History should contain 4 placements");
        assertEquals(1, history.get(0).pieceId, "First piece should be piece 1");
    }

    @Test
    @DisplayName("Should handle null in initializeWithHistory")
    void testInitializeWithNullHistory() {
        tracker = new PlacementOrderTracker();

        assertDoesNotThrow(() -> {
            tracker.initializeWithHistory(null);
        }, "Should handle null history");

        assertTrue(tracker.isTracking(), "Should be tracking");
        assertEquals(0, tracker.getCurrentDepth(), "Depth should be 0");
    }

    // ==================== Placement Recording Tests ====================

    @Test
    @DisplayName("Should record single placement")
    void testRecordPlacement() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);

        assertEquals(1, tracker.getCurrentDepth(), "Depth should be 1");

        PlacementInfo placement = tracker.getPlacement(0);
        assertNotNull(placement, "Placement should not be null");
        assertEquals(0, placement.row, "Row should match");
        assertEquals(0, placement.col, "Column should match");
        assertEquals(1, placement.pieceId, "Piece ID should match");
        assertEquals(0, placement.rotation, "Rotation should match");
    }

    @Test
    @DisplayName("Should record multiple placements in order")
    void testRecordMultiplePlacements() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);
        tracker.recordPlacement(0, 1, 2, 1);
        tracker.recordPlacement(1, 0, 3, 2);

        assertEquals(3, tracker.getCurrentDepth(), "Should have 3 placements");

        List<PlacementInfo> history = tracker.getPlacementHistory();
        assertEquals(3, history.size(), "History should have 3 placements");
        assertEquals(1, history.get(0).pieceId, "First should be piece 1");
        assertEquals(2, history.get(1).pieceId, "Second should be piece 2");
        assertEquals(3, history.get(2).pieceId, "Third should be piece 3");
    }

    @Test
    @DisplayName("Should record placement even without explicit initialization")
    void testRecordPlacementWithoutInitialization() {
        tracker = new PlacementOrderTracker();
        // Don't call initialize() - but placementHistory is auto-initialized in constructor

        tracker.recordPlacement(0, 0, 1, 0);

        // Actually works because constructor initializes placementHistory
        assertEquals(1, tracker.getCurrentDepth(), "Depth should be 1");
    }

    // ==================== Backtracking Tests ====================

    @Test
    @DisplayName("Should remove last placement")
    void testRemoveLastPlacement() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);
        tracker.recordPlacement(0, 1, 2, 1);

        PlacementInfo removed = tracker.removeLastPlacement();

        assertNotNull(removed, "Removed placement should not be null");
        assertEquals(2, removed.pieceId, "Should remove piece 2");
        assertEquals(1, tracker.getCurrentDepth(), "Depth should decrease to 1");
    }

    @Test
    @DisplayName("Should handle remove from empty history")
    void testRemoveLastPlacementEmpty() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        PlacementInfo removed = tracker.removeLastPlacement();

        assertNull(removed, "Should return null for empty history");
        assertEquals(0, tracker.getCurrentDepth(), "Depth should remain 0");
    }

    @Test
    @DisplayName("Should handle multiple removals")
    void testMultipleRemovals() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);
        tracker.recordPlacement(0, 1, 2, 1);
        tracker.recordPlacement(1, 0, 3, 2);

        PlacementInfo removed1 = tracker.removeLastPlacement();
        PlacementInfo removed2 = tracker.removeLastPlacement();
        PlacementInfo removed3 = tracker.removeLastPlacement();
        PlacementInfo removed4 = tracker.removeLastPlacement();

        assertEquals(3, removed1.pieceId, "First removal should be piece 3");
        assertEquals(2, removed2.pieceId, "Second removal should be piece 2");
        assertEquals(1, removed3.pieceId, "Third removal should be piece 1");
        assertNull(removed4, "Fourth removal should be null");
        assertEquals(0, tracker.getCurrentDepth(), "Depth should be 0");
    }

    // ==================== History Management Tests ====================

    @Test
    @DisplayName("Should get placement history")
    void testGetPlacementHistory() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);
        tracker.recordPlacement(0, 1, 2, 1);

        List<PlacementInfo> history = tracker.getPlacementHistory();

        assertNotNull(history, "History should not be null");
        assertEquals(2, history.size(), "History should have 2 placements");

        // Verify returned list is a copy (defensive copy)
        history.clear();
        assertEquals(2, tracker.getCurrentDepth(),
                    "Clearing returned list should not affect tracker");
    }

    @Test
    @DisplayName("Should clear history")
    void testClearHistory() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);
        tracker.recordPlacement(0, 1, 2, 1);
        tracker.recordPlacement(1, 0, 3, 2);

        tracker.clearHistory();

        assertEquals(0, tracker.getCurrentDepth(), "Depth should be 0 after clear");
        assertTrue(tracker.getPlacementHistory().isEmpty(),
                  "History should be empty");
    }

    @Test
    @DisplayName("Should get current depth")
    void testGetCurrentDepth() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        assertEquals(0, tracker.getCurrentDepth(), "Initial depth should be 0");

        tracker.recordPlacement(0, 0, 1, 0);
        assertEquals(1, tracker.getCurrentDepth(), "Depth should be 1");

        tracker.recordPlacement(0, 1, 2, 1);
        assertEquals(2, tracker.getCurrentDepth(), "Depth should be 2");

        tracker.removeLastPlacement();
        assertEquals(1, tracker.getCurrentDepth(), "Depth should be 1 after removal");
    }

    // ==================== Fixed Pieces Management Tests ====================

    @Test
    @DisplayName("Should get initial fixed pieces")
    void testGetInitialFixedPieces() {
        List<PlacementInfo> fixedPieces = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0)
        );

        tracker = new PlacementOrderTracker(fixedPieces);

        List<PlacementInfo> retrieved = tracker.getInitialFixedPieces();

        assertNotNull(retrieved, "Fixed pieces should not be null");
        assertEquals(2, retrieved.size(), "Should have 2 fixed pieces");

        // Verify defensive copy
        retrieved.clear();
        assertEquals(2, tracker.getInitialFixedPieces().size(),
                    "Clearing returned list should not affect tracker");
    }

    @Test
    @DisplayName("Should set initial fixed pieces")
    void testSetInitialFixedPieces() {
        tracker = new PlacementOrderTracker();

        List<PlacementInfo> fixedPieces = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0),
            new PlacementInfo(0, 2, 3, 0)
        );

        tracker.setInitialFixedPieces(fixedPieces);

        List<PlacementInfo> retrieved = tracker.getInitialFixedPieces();
        assertEquals(3, retrieved.size(), "Should have 3 fixed pieces");
        assertEquals(1, retrieved.get(0).pieceId, "First piece should be piece 1");
    }

    @Test
    @DisplayName("Should replace existing fixed pieces")
    void testReplaceFixedPieces() {
        List<PlacementInfo> initial = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0)
        );

        tracker = new PlacementOrderTracker(initial);
        assertEquals(1, tracker.getInitialFixedPieces().size(),
                    "Should have 1 initial fixed piece");

        List<PlacementInfo> newFixed = Arrays.asList(
            new PlacementInfo(0, 0, 10, 0),
            new PlacementInfo(0, 1, 20, 0)
        );

        tracker.setInitialFixedPieces(newFixed);

        List<PlacementInfo> retrieved = tracker.getInitialFixedPieces();
        assertEquals(2, retrieved.size(), "Should have 2 fixed pieces");
        assertEquals(10, retrieved.get(0).pieceId, "Should have new pieces");
    }

    @Test
    @DisplayName("Should handle null in setInitialFixedPieces")
    void testSetNullFixedPieces() {
        tracker = new PlacementOrderTracker();

        List<PlacementInfo> initial = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0)
        );
        tracker.setInitialFixedPieces(initial);

        // Set null - should not crash but may not change state
        assertDoesNotThrow(() -> {
            tracker.setInitialFixedPieces(null);
        }, "Should handle null without exception");
    }

    // ==================== Specific Placement Access Tests ====================

    @Test
    @DisplayName("Should get placement by index")
    void testGetPlacementByIndex() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 10, 0);
        tracker.recordPlacement(0, 1, 20, 1);
        tracker.recordPlacement(1, 0, 30, 2);

        PlacementInfo p0 = tracker.getPlacement(0);
        PlacementInfo p1 = tracker.getPlacement(1);
        PlacementInfo p2 = tracker.getPlacement(2);

        assertEquals(10, p0.pieceId, "Index 0 should be piece 10");
        assertEquals(20, p1.pieceId, "Index 1 should be piece 20");
        assertEquals(30, p2.pieceId, "Index 2 should be piece 30");
    }

    @Test
    @DisplayName("Should return null for invalid index")
    void testGetPlacementInvalidIndex() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);

        assertNull(tracker.getPlacement(-1), "Negative index should return null");
        assertNull(tracker.getPlacement(1), "Out of bounds index should return null");
        assertNull(tracker.getPlacement(100), "Large index should return null");
    }

    // ==================== Tracking State Tests ====================

    @Test
    @DisplayName("Should report tracking status")
    void testIsTracking() {
        tracker = new PlacementOrderTracker();

        // Constructor auto-initializes placementHistory, so tracking is always true
        assertTrue(tracker.isTracking(), "Should be tracking after construction");

        tracker.initialize();
        assertTrue(tracker.isTracking(), "Should still be tracking after initialize");
    }

    @Test
    @DisplayName("Should track after initialization with fixed pieces")
    void testIsTrackingAfterInitWithFixed() {
        tracker = new PlacementOrderTracker();

        // Constructor auto-initializes, so already tracking
        assertTrue(tracker.isTracking(), "Should be tracking after construction");

        List<PlacementInfo> fixed = Arrays.asList(new PlacementInfo(0, 0, 1, 0));
        tracker.initializeWithFixedPieces(fixed);

        assertTrue(tracker.isTracking(), "Should still be tracking after init with fixed");
    }

    @Test
    @DisplayName("Should track after initialization with history")
    void testIsTrackingAfterInitWithHistory() {
        tracker = new PlacementOrderTracker();

        // Constructor auto-initializes, so already tracking
        assertTrue(tracker.isTracking(), "Should be tracking after construction");

        List<PlacementInfo> history = Arrays.asList(new PlacementInfo(0, 0, 1, 0));
        tracker.initializeWithHistory(history);

        assertTrue(tracker.isTracking(), "Should still be tracking after init with history");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle large placement history")
    void testLargePlacementHistory() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        // Add 1000 placements
        for (int i = 0; i < 1000; i++) {
            tracker.recordPlacement(i % 10, i % 10, i, i % 4);
        }

        assertEquals(1000, tracker.getCurrentDepth(), "Should have 1000 placements");

        PlacementInfo first = tracker.getPlacement(0);
        PlacementInfo last = tracker.getPlacement(999);

        assertEquals(0, first.pieceId, "First piece should be 0");
        assertEquals(999, last.pieceId, "Last piece should be 999");
    }

    @Test
    @DisplayName("Should handle clear and reinitialize")
    void testClearAndReinitialize() {
        tracker = new PlacementOrderTracker();
        tracker.initialize();

        tracker.recordPlacement(0, 0, 1, 0);
        tracker.recordPlacement(0, 1, 2, 1);

        tracker.clearHistory();
        assertEquals(0, tracker.getCurrentDepth(), "Depth should be 0 after clear");

        // Should be able to record again
        tracker.recordPlacement(1, 1, 10, 2);
        assertEquals(1, tracker.getCurrentDepth(), "Should be able to record after clear");
    }
}
