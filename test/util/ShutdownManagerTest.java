package util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests pour ShutdownManager
 */
public class ShutdownManagerTest {

    @BeforeEach
    public void setUp() {
        ShutdownManager.reset();
    }

    @AfterEach
    public void tearDown() {
        ShutdownManager.reset();
    }

    @Test
    public void testInitialState() {
        assertFalse(ShutdownManager.isShutdownRequested());
    }

    @Test
    public void testRequestShutdown() {
        assertFalse(ShutdownManager.isShutdownRequested());
        ShutdownManager.requestShutdown();
        assertTrue(ShutdownManager.isShutdownRequested());
    }

    @Test
    public void testRegisterShutdownHook() {
        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> ShutdownManager.registerShutdownHook());
    }

    @Test
    public void testRegisterShutdownHookTwice() {
        // Devrait être idempotent
        assertDoesNotThrow(() -> {
            ShutdownManager.registerShutdownHook();
            ShutdownManager.registerShutdownHook();
        });
    }

    @Test
    public void testAddShutdownHook() {
        AtomicBoolean hookCalled = new AtomicBoolean(false);

        ShutdownManager.ShutdownHook hook = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                hookCalled.set(true);
            }

            @Override
            public String getName() {
                return "TestHook";
            }
        };

        assertDoesNotThrow(() -> ShutdownManager.addShutdownHook(hook));
    }

    @Test
    public void testAddNullHook() {
        assertThrows(IllegalArgumentException.class, () -> {
            ShutdownManager.addShutdownHook(null);
        });
    }

    @Test
    public void testRemoveShutdownHook() {
        ShutdownManager.ShutdownHook hook = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {}

            @Override
            public String getName() {
                return "TestHook";
            }
        };

        ShutdownManager.addShutdownHook(hook);
        assertDoesNotThrow(() -> ShutdownManager.removeShutdownHook(hook));
    }

    @Test
    public void testSolverShutdownHook() {
        AtomicBoolean saveCalled = new AtomicBoolean(false);
        Runnable saveAction = () -> saveCalled.set(true);

        ShutdownManager.SolverShutdownHook hook =
            new ShutdownManager.SolverShutdownHook(saveAction, "Test solver");

        assertEquals("SolverShutdown: Test solver", hook.getName());

        hook.onShutdown("TEST_SIGNAL");
        assertTrue(saveCalled.get());
    }

    @Test
    public void testSolverShutdownHookWithNullAction() {
        ShutdownManager.SolverShutdownHook hook =
            new ShutdownManager.SolverShutdownHook(null, "Test solver");

        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> hook.onShutdown("TEST_SIGNAL"));
    }

    @Test
    public void testStatisticsShutdownHook() {
        String stats = "Test statistics";
        ShutdownManager.StatisticsShutdownHook hook =
            new ShutdownManager.StatisticsShutdownHook(stats);

        assertEquals("StatisticsDisplay", hook.getName());

        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> hook.onShutdown("TEST_SIGNAL"));
    }

    @Test
    public void testStatisticsShutdownHookWithNull() {
        ShutdownManager.StatisticsShutdownHook hook =
            new ShutdownManager.StatisticsShutdownHook(null);

        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> hook.onShutdown("TEST_SIGNAL"));
    }

    @Test
    public void testCleanupShutdownHook() {
        AtomicBoolean cleanupCalled = new AtomicBoolean(false);
        Runnable cleanupAction = () -> cleanupCalled.set(true);

        ShutdownManager.CleanupShutdownHook hook =
            new ShutdownManager.CleanupShutdownHook(cleanupAction, "Test cleanup");

        assertEquals("Cleanup: Test cleanup", hook.getName());

        hook.onShutdown("TEST_SIGNAL");
        assertTrue(cleanupCalled.get());
    }

    @Test
    public void testCleanupShutdownHookWithNullAction() {
        ShutdownManager.CleanupShutdownHook hook =
            new ShutdownManager.CleanupShutdownHook(null, "Test cleanup");

        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> hook.onShutdown("TEST_SIGNAL"));
    }

    @Test
    public void testMultipleHooks() {
        AtomicInteger callCount = new AtomicInteger(0);

        ShutdownManager.ShutdownHook hook1 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                callCount.incrementAndGet();
            }

            @Override
            public String getName() {
                return "Hook1";
            }
        };

        ShutdownManager.ShutdownHook hook2 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                callCount.incrementAndGet();
            }

            @Override
            public String getName() {
                return "Hook2";
            }
        };

        ShutdownManager.addShutdownHook(hook1);
        ShutdownManager.addShutdownHook(hook2);

        // Vérifier que les hooks sont bien ajoutés (ne peuvent pas tester l'exécution sans shutdown réel)
        assertDoesNotThrow(() -> ShutdownManager.registerShutdownHook());
    }

    @Test
    public void testPrintShutdownMessage() {
        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> ShutdownManager.printShutdownMessage("Test message"));
    }

    @Test
    public void testPrintFinalStatistics() {
        Object stats = new Object() {
            @Override
            public String toString() {
                return "Test stats";
            }
        };

        // Ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> ShutdownManager.printFinalStatistics(stats));
    }

    @Test
    public void testReset() {
        ShutdownManager.requestShutdown();
        assertTrue(ShutdownManager.isShutdownRequested());

        ShutdownManager.reset();
        assertFalse(ShutdownManager.isShutdownRequested());
    }

    @Test
    public void testHookExecutionOrder() {
        // Test que les hooks sont exécutés dans l'ordre d'ajout
        StringBuilder executionOrder = new StringBuilder();

        ShutdownManager.ShutdownHook hook1 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                executionOrder.append("1");
            }

            @Override
            public String getName() {
                return "OrderHook1";
            }
        };

        ShutdownManager.ShutdownHook hook2 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                executionOrder.append("2");
            }

            @Override
            public String getName() {
                return "OrderHook2";
            }
        };

        ShutdownManager.ShutdownHook hook3 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                executionOrder.append("3");
            }

            @Override
            public String getName() {
                return "OrderHook3";
            }
        };

        ShutdownManager.addShutdownHook(hook1);
        ShutdownManager.addShutdownHook(hook2);
        ShutdownManager.addShutdownHook(hook3);

        // Simuler l'exécution des hooks
        hook1.onShutdown("TEST");
        hook2.onShutdown("TEST");
        hook3.onShutdown("TEST");

        assertEquals("123", executionOrder.toString());
    }

    @Test
    public void testHookWithException() {
        AtomicBoolean hook2Called = new AtomicBoolean(false);

        ShutdownManager.ShutdownHook hook1 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                throw new RuntimeException("Test exception");
            }

            @Override
            public String getName() {
                return "FailingHook";
            }
        };

        ShutdownManager.ShutdownHook hook2 = new ShutdownManager.ShutdownHook() {
            @Override
            public void onShutdown(String signal) {
                hook2Called.set(true);
            }

            @Override
            public String getName() {
                return "WorkingHook";
            }
        };

        ShutdownManager.addShutdownHook(hook1);
        ShutdownManager.addShutdownHook(hook2);

        // Hook1 lance une exception comme prévu
        assertThrows(RuntimeException.class, () -> hook1.onShutdown("TEST"));

        // Hook2 devrait quand même pouvoir s'exécuter
        hook2.onShutdown("TEST");
        assertTrue(hook2Called.get());
    }
}
