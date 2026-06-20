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
 */
public interface LogCaptor extends AutoCloseable {

  /**
   * Returns an unmodifiable snapshot of all log entries captured so far.
   *
   * <p>The returned list is a point-in-time copy; entries logged after this call do not affect it.
   *
   * @return immutable list of captured entries, newest entries last; never {@code null}
   */
  List<LogEntry> getLogs();

  /**
   * Removes all captured log entries.
   *
   * <p>Called automatically by the JUnit 5 extension before each test method to ensure isolation.
   */
  void clearLogs();

  /**
   * Restores any log-level overrides that were applied via {@link #withMinLevel(Level)}.
   *
   * <p>Called automatically by the JUnit 5 extension in {@code afterEach}.
   */
  void resetConfiguration();

  /**
   * Lowers the effective log level to at least {@code level} for this capture scope.
   *
   * <p>This allows tests to capture finer-grained events (e.g. DEBUG) even when the logger is
   * normally configured at a higher level. The original level is restored by {@link
   * #resetConfiguration()}.
   *
   * @param level the minimum level to capture
   * @return {@code this} for fluent chaining
   */
  LogCaptor withMinLevel(Level level);

  /**
   * Detaches from the logging framework and restores original state.
   *
   * <p>Idempotent — safe to call multiple times.
   */
  @Override
  void close();
}
