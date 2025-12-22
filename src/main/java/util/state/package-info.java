/**
 * Save state management and persistence.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link util.state.SaveState} - Immutable save state data class</li>
 *   <li>{@link util.state.SaveStateRepository} - Repository interface for save operations</li>
 *   <li>{@link util.state.FileSaveStateRepository} - File-based save repository implementation</li>
 *   <li>{@link util.state.SaveStateIO} - Low-level save/load I/O operations</li>
 *   <li>{@link util.state.SaveStateSerializer} - Serialization logic</li>
 *   <li>{@link util.state.BinarySaveManager} - Binary format save/load (faster)</li>
 *   <li>{@link util.state.SaveFileManager} - File discovery and management</li>
 * </ul>
 *
 * <h2>Design (Phase 1 Refactoring)</h2>
 * <p>Extracted from monolithic SaveStateManager for better separation of concerns:</p>
 * <ul>
 *   <li>Repository pattern for persistence abstraction</li>
 *   <li>Separate serialization from file management</li>
 *   <li>Support for both text and binary formats</li>
 * </ul>
 *
 * @see util.SaveStateManager
 */
package util.state;
