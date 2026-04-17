/**
 * Save state persistence helpers.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link util.state.SaveStateIO} - Low-level save/load I/O operations</li>
 *   <li>{@link util.state.SaveStateSerializer} - Serialization logic</li>
 *   <li>{@link util.state.BinarySaveManager} - Binary format save/load (faster)</li>
 *   <li>{@link util.state.SaveFileManager} - File discovery and management</li>
 * </ul>
 *
 * <p>The unfinished {@code SaveStateRepository} / {@code FileSaveStateRepository}
 * pair plus the duplicate {@code SaveState} class were removed: they were
 * never wired up in production, the repository was a circular shim around
 * the legacy static {@link util.SaveStateManager}, and {@code findByConfigId}
 * returned an empty {@link model.Board}. Callers continue to use
 * {@link util.SaveStateManager} (decoupled from solver code via
 * {@link util.SaveStore} for testability).</p>
 *
 * @see util.SaveStateManager
 * @see util.SaveStore
 */
package util.state;
