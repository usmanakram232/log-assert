package io.github.logassert.assertj;

import io.github.logassert.core.LogEntry;
import java.util.Objects;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;
import org.slf4j.event.Level;

/**
 * AssertJ assertion class for a single {@link LogEntry}.
 *
 * <p>Instances are obtained from navigation methods on {@link LogsAssert}:
 *
 * <pre>{@code
 * LogCaptorAssertions.assertThat(captor)
 *     .atLevel(Level.ERROR)
 *     .single()
 *     .hasFormattedMessageContaining("timeout")
 *     .hasThrowable(SocketTimeoutException.class);
 * }</pre>
 *
 * <p>All failure messages include the actual value so the developer never has to guess what was
 * logged.
 */
public class LogEntryAssert extends AbstractAssert<LogEntryAssert, LogEntry> {

  LogEntryAssert(LogEntry entry) {
    super(entry, LogEntryAssert.class);
  }

  // ── formattedMessage assertions ───────────────────────────────────────────

  /**
   * Asserts that the fully-resolved formatted message exactly equals {@code expected}.
   *
   * @param expected the exact expected message string
   * @return {@code this}
   */
  public LogEntryAssert hasFormattedMessage(String expected) {
    isNotNull();
    if (!Objects.equals(expected, actual.formattedMessage())) {
      failWithMessage(
          "Expected formattedMessage to be:%n  <%s>%nbut was:%n  <%s>",
          expected, actual.formattedMessage());
    }
    return this;
  }

  /**
   * Asserts that the formatted message contains {@code substring} (case-sensitive).
   *
   * @param substring the expected substring
   * @return {@code this}
   */
  public LogEntryAssert hasFormattedMessageContaining(String substring) {
    isNotNull();
    if (substring == null || !actual.formattedMessage().contains(substring)) {
      failWithMessage(
          "Expected formattedMessage to contain <%s> but was <%s>",
          substring, actual.formattedMessage());
    }
    return this;
  }

  /**
   * Asserts that the formatted message matches {@code regex} (using {@link
   * java.util.regex.Matcher#find()}, not {@link java.util.regex.Matcher#matches()}).
   *
   * @param regex the compiled pattern to search for
   * @return {@code this}
   */
  public LogEntryAssert hasFormattedMessageMatching(Pattern regex) {
    isNotNull();
    if (!regex.matcher(actual.formattedMessage()).find()) {
      failWithMessage(
          "Expected formattedMessage to match pattern <%s> but was <%s>",
          regex.pattern(), actual.formattedMessage());
    }
    return this;
  }

  // ── rawTemplate assertion ─────────────────────────────────────────────────

  /**
   * Asserts that the raw SLF4J message template (before {@code {}} substitution) equals {@code
   * expected}.
   *
   * @param expected the expected template string
   * @return {@code this}
   */
  public LogEntryAssert hasRawTemplate(String expected) {
    isNotNull();
    if (!Objects.equals(expected, actual.rawTemplate())) {
      failWithMessage(
          "Expected rawTemplate to be <%s> but was <%s>", expected, actual.rawTemplate());
    }
    return this;
  }

  // ── level / logger assertions ─────────────────────────────────────────────

  /**
   * Asserts that the log level is exactly {@code expected}.
   *
   * @param expected the expected SLF4J level
   * @return {@code this}
   */
  public LogEntryAssert hasLevel(Level expected) {
    isNotNull();
    if (actual.level() != expected) {
      failWithMessage("Expected level to be <%s> but was <%s>", expected, actual.level());
    }
    return this;
  }

  /**
   * Asserts that the logger name is exactly {@code expected}.
   *
   * @param expected the expected fully-qualified logger name
   * @return {@code this}
   */
  public LogEntryAssert hasLoggerName(String expected) {
    isNotNull();
    if (!expected.equals(actual.loggerName())) {
      failWithMessage("Expected loggerName to be <%s> but was <%s>", expected, actual.loggerName());
    }
    return this;
  }

  // ── MDC assertion ─────────────────────────────────────────────────────────

  /**
   * Asserts that the MDC context at log time contained {@code key} mapped to {@code value}.
   *
   * @param key the MDC key
   * @param value the expected MDC value
   * @return {@code this}
   */
  public LogEntryAssert hasMdcEntry(String key, String value) {
    isNotNull();
    String actualValue = actual.mdcContext().get(key);
    if (!Objects.equals(value, actualValue)) {
      failWithMessage(
          "Expected MDC key <%s> to have value <%s> but was <%s>", key, value, actualValue);
    }
    return this;
  }

  // ── throwable assertions ──────────────────────────────────────────────────

  /**
   * Asserts that a throwable was captured and its class name equals {@code type.getName()}.
   *
   * @param type the expected throwable class
   * @return {@code this}
   */
  public LogEntryAssert hasThrowable(Class<? extends Throwable> type) {
    isNotNull();
    if (actual.throwable() == null) {
      failWithMessage(
          "Expected a throwable of type <%s> but no throwable was captured in entry: %s",
          type.getName(), formatEntry(actual));
    } else if (!actual.throwable().isOfType(type)) {
      failWithMessage(
          "Expected throwable of type <%s> but was <%s>",
          type.getName(), actual.throwable().className());
    }
    return this;
  }

  /**
   * Asserts that a throwable was captured and its message contains {@code substring}
   * (case-sensitive).
   *
   * @param substring the expected substring within the throwable message
   * @return {@code this}
   */
  public LogEntryAssert hasThrowableWithMessageContaining(String substring) {
    isNotNull();
    if (actual.throwable() == null) {
      failWithMessage(
          "Expected a throwable with message containing <%s> but no throwable was captured"
              + " in entry: %s",
          substring, formatEntry(actual));
    } else if (actual.throwable().message() == null
        || !actual.throwable().message().contains(substring)) {
      failWithMessage(
          "Expected throwable message to contain <%s> but was <%s>",
          substring, actual.throwable().message());
    }
    return this;
  }

  /**
   * Asserts that a throwable was captured and its message equals {@code message}.
   *
   * @param message the expected throwable message
   * @return {@code this}
   */
  public LogEntryAssert hasThrowableWithMessage(String message) {
    isNotNull();
    if (actual.throwable() == null) {
      failWithMessage(
          "Expected a throwable with message <%s> but no throwable was captured in entry: %s",
          message, formatEntry(actual));
    } else if (!Objects.equals(message, actual.throwable().message())) {
      failWithMessage(
          "Expected throwable message to be <%s> but was <%s>",
          message, actual.throwable().message());
    }
    return this;
  }

  /**
   * Asserts that no throwable was captured for this entry.
   *
   * @return {@code this}
   */
  public LogEntryAssert hasNoThrowable() {
    isNotNull();
    if (actual.throwable() != null) {
      failWithMessage(
          "Expected no throwable but found <%s> with message <%s>",
          actual.throwable().className(), actual.throwable().message());
    }
    return this;
  }

  // ── Formatting helper ─────────────────────────────────────────────────────

  private static String formatEntry(LogEntry entry) {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(entry.level()).append("] ");
    sb.append(entry.loggerName()).append(" - ").append(entry.formattedMessage());
    if (entry.throwable() != null) {
      sb.append(" (").append(entry.throwable().simpleClassName()).append(")");
    }
    return sb.toString();
  }
}
