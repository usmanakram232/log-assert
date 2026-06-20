package io.github.logassert.jboss;

import io.github.logassert.core.LogEntry;
import io.github.logassert.core.ThrowableInfo;
import java.util.Map;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.slf4j.event.Level;

/**
 * JBoss Log Manager {@link ExtHandler} that captures log records into a {@link LogCaptorStore}.
 *
 * <p>Installed on the JBoss root logger by {@link LogCaptorImpl#install()} and removed by {@link
 * LogCaptorImpl#uninstall()}. The handler captures all levels (level filtering is the
 * responsibility of assertion code, not the capture pipeline).
 *
 * <p>This class is internal to the {@code jboss} package; callers should interact with {@link
 * LogCaptorImpl} instead.
 */
public final class LogCaptorHandler extends ExtHandler {

  private final LogCaptorStore store;

  /**
   * Creates a handler that appends captured records to {@code store}.
   *
   * @param store the store to append to; must not be {@code null}
   */
  public LogCaptorHandler(LogCaptorStore store) {
    this.store = store;
    // Capture everything — assertion-time filtering determines what is relevant
    setLevel(java.util.logging.Level.ALL);
  }

  /**
   * Called by JBoss Log Manager for each loggable record. Converts the record to a {@link
   * LogEntry} snapshot and appends it to the store.
   *
   * @param record the log record; skipped if {@code null} or not loggable
   */
  @Override
  protected void doPublish(ExtLogRecord record) {
    if (record == null || !isLoggable(record)) {
      return;
    }
    store.append(toLogEntry(record));
    flush();
  }

  /** No-op: in-memory handler does not buffer output. */
  @Override
  public void flush() {
    // no-op
  }

  /** No-op: in-memory handler holds no resources. */
  @Override
  public void close() throws SecurityException {
    // no-op
  }

  // ── Conversion helpers ─────────────────────────────────────────────────────

  private static LogEntry toLogEntry(ExtLogRecord record) {
    return new LogEntry(
        record.getInstant(),
        mapLevel(record.getLevel()),
        record.getLoggerName() != null ? record.getLoggerName() : "",
        record.getFormattedMessage() != null ? record.getFormattedMessage() : "",
        record.getMessage() != null ? record.getMessage() : "",
        ThrowableInfo.from(record.getThrown()),
        safeMdc(record),
        record.getThreadName() != null ? record.getThreadName() : "",
        record.getLongThreadID(),
        null /* markerName — placeholder, always null until marker support is added */);
  }

  /**
   * Maps a JUL / JBoss log level integer value to the corresponding SLF4J {@link Level}.
   *
   * <p>JBoss extends the JUL level range:
   *
   * <ul>
   *   <li>≥ 1000 → SEVERE / ERROR / FATAL → {@code ERROR}
   *   <li>≥ 900 → WARNING / WARN → {@code WARN}
   *   <li>≥ 800 → INFO → {@code INFO}
   *   <li>≥ 500 → FINE / CONFIG / DEBUG → {@code DEBUG}
   *   <li>&lt; 500 → FINER / FINEST / TRACE → {@code TRACE}
   * </ul>
   */
  private static Level mapLevel(java.util.logging.Level level) {
    if (level == null) {
      return Level.INFO;
    }
    int v = level.intValue();
    if (v >= 1000) return Level.ERROR; // SEVERE, JBoss ERROR/FATAL
    if (v >= 900) return Level.WARN; // WARNING, JBoss WARN
    if (v >= 800) return Level.INFO; // INFO
    if (v >= 500) return Level.DEBUG; // FINE, CONFIG, JBoss DEBUG
    return Level.TRACE; // FINER, FINEST, JBoss TRACE
  }

  /**
   * Returns a defensive copy of the MDC map from {@code record}, or an empty map if the call
   * fails.
   *
   * <p>MDC access can throw in edge cases (e.g. partially torn-down thread-local state), so we
   * guard defensively.
   */
  private static Map<String, String> safeMdc(ExtLogRecord record) {
    try {
      Map<String, String> mdc = record.getMdcCopy();
      return mdc != null ? mdc : Map.of();
    } catch (Exception e) {
      return Map.of();
    }
  }
}
