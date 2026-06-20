package io.github.logassert.assertj;

import io.github.logassert.core.LogEntry;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.slf4j.event.Level;

/**
 * AssertJ assertion class for a list of {@link LogEntry} instances.
 *
 * <p>Filters narrow the captured list and each returns a <em>new</em> {@code LogsAssert} over the
 * narrowed view — they do not mutate this instance. Terminal assertions verify the current view.
 * Navigation methods ({@link #single()}, {@link #first()}, {@link #last()}) return a {@link
 * LogEntryAssert} for fine-grained checks on a single entry.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * LogCaptorAssertions.assertThat(captor)
 *     .atLevel(Level.ERROR)
 *     .containingMessage("timeout")
 *     .single()
 *     .hasThrowable(SocketTimeoutException.class);
 * }</pre>
 */
public class LogsAssert extends AbstractAssert<LogsAssert, List<LogEntry>> {

  LogsAssert(List<LogEntry> entries) {
    super(List.copyOf(entries), LogsAssert.class);
  }

  // ── Filters — each returns a new LogsAssert over a narrowed list ─────────

  /**
   * Keeps only entries whose {@link LogEntry#level()} exactly matches {@code level}.
   *
   * @param level the SLF4J level to filter by
   * @return new {@code LogsAssert} over the filtered list
   */
  public LogsAssert atLevel(Level level) {
    return new LogsAssert(actual.stream().filter(e -> e.level() == level).toList());
  }

  /**
   * Keeps only entries whose {@link LogEntry#loggerName()} starts with {@code clazz.getName()}.
   *
   * @param clazz the class whose name is used as a logger-name prefix
   * @return new {@code LogsAssert} over the filtered list
   */
  public LogsAssert fromLogger(Class<?> clazz) {
    return fromLogger(clazz.getName());
  }

  /**
   * Keeps only entries whose {@link LogEntry#loggerName()} starts with {@code prefix}.
   *
   * @param prefix the logger-name prefix to match
   * @return new {@code LogsAssert} over the filtered list
   */
  public LogsAssert fromLogger(String prefix) {
    return new LogsAssert(actual.stream().filter(e -> e.loggerName().startsWith(prefix)).toList());
  }

  /**
   * Keeps only entries whose {@link LogEntry#formattedMessage()} contains {@code substring}.
   *
   * @param substring the substring to search for (case-sensitive)
   * @return new {@code LogsAssert} over the filtered list
   */
  public LogsAssert containingMessage(String substring) {
    return new LogsAssert(
        actual.stream().filter(e -> e.formattedMessage().contains(substring)).toList());
  }

  /**
   * Keeps only entries whose MDC context contains {@code key} mapped to {@code value}.
   *
   * @param key the MDC key
   * @param value the expected MDC value
   * @return new {@code LogsAssert} over the filtered list
   */
  public LogsAssert withMdcEntry(String key, String value) {
    return new LogsAssert(
        actual.stream().filter(e -> Objects.equals(value, e.mdcContext().get(key))).toList());
  }

  // ── Terminal assertions ───────────────────────────────────────────────────

  /**
   * Asserts that the number of captured entries equals {@code expected}.
   *
   * <p>On failure the error message includes the <em>full list</em> of captured entries so the
   * developer can see what was actually logged without needing to add extra debug output.
   *
   * @param expected the exact number of entries expected
   * @return {@code this}
   */
  public LogsAssert hasSize(int expected) {
    if (actual.size() != expected) {
      failWithMessage(
          "Expected %d log entries but found %d.%nCaptured entries:%n%s",
          expected, actual.size(), formatEntries(actual));
    }
    return this;
  }

  /**
   * Asserts that no entries were captured.
   *
   * <p>On failure the error message lists all captured entries.
   *
   * @return {@code this}
   */
  public LogsAssert isEmpty() {
    if (!actual.isEmpty()) {
      failWithMessage(
          "Expected no log entries but found %d.%nCaptured entries:%n%s",
          actual.size(), formatEntries(actual));
    }
    return this;
  }

  /**
   * Asserts that at least one entry was captured.
   *
   * @return {@code this}
   */
  public LogsAssert isNotEmpty() {
    if (actual.isEmpty()) {
      failWithMessage("Expected at least one log entry but the list was empty.");
    }
    return this;
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  /**
   * Asserts that exactly one entry was captured, then returns a {@link LogEntryAssert} over it.
   *
   * @return assertion for the single entry
   * @throws AssertionError if the number of entries is not exactly 1 (message includes all entries)
   */
  public LogEntryAssert single() {
    hasSize(1);
    return new LogEntryAssert(actual.get(0));
  }

  /**
   * Asserts that at least one entry was captured, then returns a {@link LogEntryAssert} over the
   * first entry.
   *
   * @return assertion for the first entry
   * @throws AssertionError if the list is empty
   */
  public LogEntryAssert first() {
    isNotEmpty();
    return new LogEntryAssert(actual.get(0));
  }

  /**
   * Asserts that at least one entry was captured, then returns a {@link LogEntryAssert} over the
   * last entry.
   *
   * @return assertion for the last entry
   * @throws AssertionError if the list is empty
   */
  public LogEntryAssert last() {
    isNotEmpty();
    return new LogEntryAssert(actual.get(actual.size() - 1));
  }

  // ── Formatting helpers ────────────────────────────────────────────────────

  private static String formatEntries(List<LogEntry> entries) {
    if (entries.isEmpty()) {
      return "  (none)";
    }
    return entries.stream().map(LogsAssert::formatEntry).collect(Collectors.joining("\n"));
  }

  private static String formatEntry(LogEntry entry) {
    StringBuilder sb = new StringBuilder();
    sb.append("  [").append(entry.level()).append("] ");
    sb.append(entry.loggerName()).append(" - ");
    sb.append(entry.formattedMessage());
    if (entry.throwable() != null) {
      sb.append(" (").append(entry.throwable().simpleClassName()).append(")");
    }
    return sb.toString();
  }
}
