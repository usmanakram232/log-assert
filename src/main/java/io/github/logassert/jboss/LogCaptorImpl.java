package io.github.logassert.jboss;

import io.github.logassert.core.LogCaptor;
import io.github.logassert.core.LogEntry;
import java.util.List;
import org.slf4j.event.Level;

/**
 * JBoss Log Manager implementation of {@link LogCaptor}.
 *
 * <p>Installs a {@link LogCaptorHandler} on the JBoss root logger, captures all emitted log
 * records into a {@link LogCaptorStore}, and exposes them for assertion. The original log-level
 * configuration is restored when {@link #close()} (or {@link #resetConfiguration()}) is called.
 *
 * <p>Typical lifecycle managed by a JUnit 5 extension:
 *
 * <pre>{@code
 * captor = new LogCaptorImpl();
 * captor.install();      // beforeAll / beforeEach
 * captor.clearLogs();    // beforeEach (isolation)
 * // … test runs …
 * captor.close();        // afterAll — uninstalls handler and restores level
 * }</pre>
 */
public final class LogCaptorImpl implements LogCaptor {

  private final LogCaptorStore store;
  private final LogCaptorHandler handler;

  /** JUL level that was in effect before any {@link #withMinLevel} call; used for restore. */
  private java.util.logging.Level previousLevel;

  /** {@code true} once the root logger level has been overridden by {@link #withMinLevel}. */
  private boolean levelOverridden = false;

  /**
   * Creates a new {@code LogCaptorImpl} backed by a default-capacity {@link LogCaptorStore}.
   *
   * <p>Call {@link #install()} to attach to the JBoss root logger before any logging occurs.
   */
  public LogCaptorImpl() {
    this(new LogCaptorStore());
  }

  /**
   * Creates a new {@code LogCaptorImpl} backed by the supplied {@code store}.
   *
   * <p>Useful in unit tests that need to inspect the store directly or set a custom capacity.
   *
   * @param store the store to use; must not be {@code null}
   */
  public LogCaptorImpl(LogCaptorStore store) {
    this.store = store;
    this.handler = new LogCaptorHandler(store);
  }

  /**
   * Attaches the capture handler to the JBoss root logger.
   *
   * <p>Should be called once, before any log statements that need to be captured. Calling {@code
   * install()} multiple times is safe but results in duplicate entries.
   */
  public void install() {
    java.util.logging.LogManager.getLogManager().getLogger("").addHandler(handler);
  }

  /**
   * Removes the capture handler from the JBoss root logger.
   *
   * <p>After this call, new log events are no longer captured. Already-captured entries remain in
   * the store until {@link #clearLogs()} is called.
   */
  public void uninstall() {
    java.util.logging.LogManager.getLogManager().getLogger("").removeHandler(handler);
  }

  // ── LogCaptor implementation ───────────────────────────────────────────────

  /** {@inheritDoc} */
  @Override
  public List<LogEntry> getLogs() {
    return store.snapshot();
  }

  /** {@inheritDoc} */
  @Override
  public void clearLogs() {
    store.clear();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Restores the root-logger level to the value it had before the first {@link
   * #withMinLevel(Level)} call in this scope. Does nothing if the level was never overridden.
   */
  @Override
  public void resetConfiguration() {
    if (levelOverridden) {
      java.util.logging.LogManager.getLogManager().getLogger("").setLevel(previousLevel);
      levelOverridden = false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Saves the current root-logger level on the first call (so it can be restored later) and
   * then sets the level to the JUL equivalent of {@code level}. Subsequent calls in the same scope
   * update the level without re-saving.
   */
  @Override
  public LogCaptor withMinLevel(Level level) {
    java.util.logging.Logger root = java.util.logging.LogManager.getLogManager().getLogger("");
    if (!levelOverridden) {
      previousLevel = root.getLevel();
      levelOverridden = true;
    }
    root.setLevel(toJulLevel(level));
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Resets any level overrides, then uninstalls the capture handler. Safe to call multiple
   * times.
   */
  @Override
  public void close() {
    resetConfiguration();
    uninstall();
  }

  // ── Internal helpers ───────────────────────────────────────────────────────

  /**
   * Converts an SLF4J {@link Level} to the equivalent {@link java.util.logging.Level}.
   *
   * <p>Mapping:
   *
   * <ul>
   *   <li>{@code TRACE} → {@link java.util.logging.Level#FINEST}
   *   <li>{@code DEBUG} → {@link java.util.logging.Level#FINE}
   *   <li>{@code INFO} → {@link java.util.logging.Level#INFO}
   *   <li>{@code WARN} → {@link java.util.logging.Level#WARNING}
   *   <li>{@code ERROR} → {@link java.util.logging.Level#SEVERE}
   * </ul>
   */
  private static java.util.logging.Level toJulLevel(Level level) {
    return switch (level) {
      case TRACE -> java.util.logging.Level.FINEST;
      case DEBUG -> java.util.logging.Level.FINE;
      case INFO -> java.util.logging.Level.INFO;
      case WARN -> java.util.logging.Level.WARNING;
      case ERROR -> java.util.logging.Level.SEVERE;
    };
  }
}
