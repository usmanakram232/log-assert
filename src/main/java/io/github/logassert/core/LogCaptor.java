package io.github.logassert.core;

import java.util.List;
import org.slf4j.event.Level;

/**
 * Contract for an in-memory log capture scope.
 *
 * <p>Implementations attach to the underlying logging framework (e.g. JBoss Log Manager), intercept
 * log events, and make them available as {@link LogEntry} snapshots. Each instance represents one
 * capture scope and should be closed (or managed by the JUnit 5 extension) when the scope ends.
 *
 * <p>Usage via try-with-resources:
 *
 * <pre>{@code
 * try (LogCaptor captor = LogCaptorFactory.forClass(MyService.class)) {
 *     myService.doSomething();
 *     LogCaptorAssertions.assertThat(captor)
 *         .atLevel(Level.ERROR)
 *         .hasSize(1);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface LogCaptor extends AutoCloseable {

  /**
   * Returns an unmodifiable snapshot of all log entries captured at the time of this call.
   *
   * <p>Subsequent log statements do <em>not</em> affect the returned list. Call this method again
   * to get a fresh snapshot reflecting any entries logged after the previous call.
   *
   * <p>The list is ordered oldest-first (chronological insertion order).
   *
   * @return immutable snapshot of captured entries; never {@code null}
   * @since 1.0.0
   */
  List<LogEntry> getLogs();

  /**
   * Removes all captured log entries.
   *
   * <p>Called automatically by the JUnit 5 extension before each test method to ensure isolation.
   *
   * @since 1.0.0
   */
  void clearLogs();

  /**
   * Restores any log-level overrides that were applied via {@link #withMinLevel(Level)}.
   *
   * <p>Called automatically by the JUnit 5 extension in {@code afterEach}.
   *
   * @since 1.0.0
   */
  void resetConfiguration();

  /**
   * Lowers the effective log level to at least {@code level} for this capture scope.
   *
   * <p>This allows tests to capture finer-grained events (e.g. DEBUG) even when the logger is
   * normally configured at a higher level. The original level is restored by {@link
   * #resetConfiguration()}.
   *
   * <p><strong>Note:</strong> only the <em>first</em> call saves the original level; subsequent
   * calls update the active level without re-saving, ensuring that {@link #resetConfiguration()}
   * always restores the level that was in effect <em>before</em> this scope started.
   *
   * @param level the minimum level to capture
   * @return {@code this} for fluent chaining
   * @since 1.0.0
   */
  LogCaptor withMinLevel(Level level);

  /**
   * Detaches from the logging framework and restores original state.
   *
   * <p>Idempotent — safe to call multiple times.
   *
   * @since 1.0.0
   */
  @Override
  void close();
}
