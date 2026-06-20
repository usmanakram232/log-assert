package io.github.logassert.assertj;

import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.logassert.core.LogEntry;
import io.github.logassert.core.ThrowableInfo;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class LogEntryAssertTest {

  // ── hasFormattedMessage (exact) ───────────────────────────────────────────

  @Test
  void hasFormattedMessage_passes_onExactMatch() {
    LogEntry entry = entry(Level.INFO, "com.example.Foo", "Payment processed successfully");

    assertThat(List.of(entry)).single().hasFormattedMessage("Payment processed successfully");
  }

  @Test
  void hasFormattedMessage_fails_onMismatch_withActualValueInMessage() {
    LogEntry entry = entry(Level.INFO, "com.example.Foo", "actual message text");

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasFormattedMessage("expected message"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("expected message")
        .hasMessageContaining("actual message text");
  }

  // ── hasFormattedMessageContaining ────────────────────────────────────────

  @Test
  void hasFormattedMessageContaining_passes_whenSubstringPresent() {
    LogEntry entry = entry(Level.ERROR, "com.example.Foo", "Payment failed: connection timeout");

    assertThat(List.of(entry)).single().hasFormattedMessageContaining("connection timeout");
  }

  @Test
  void hasFormattedMessageContaining_fails_whenSubstringAbsent() {
    LogEntry entry = entry(Level.ERROR, "com.example.Foo", "Payment succeeded");

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasFormattedMessageContaining("failed"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("failed")
        .hasMessageContaining("Payment succeeded");
  }

  // ── hasFormattedMessageMatching ───────────────────────────────────────────

  @Test
  void hasFormattedMessageMatching_passes_whenPatternMatches() {
    LogEntry entry = entry(Level.INFO, "com.example.Foo", "User 42 logged in");

    assertThat(List.of(entry))
        .single()
        .hasFormattedMessageMatching(Pattern.compile("User \\d+ logged in"));
  }

  @Test
  void hasFormattedMessageMatching_fails_whenPatternDoesNotMatch() {
    LogEntry entry = entry(Level.INFO, "com.example.Foo", "User abc logged in");

    assertThatThrownBy(
            () ->
                assertThat(List.of(entry))
                    .single()
                    .hasFormattedMessageMatching(Pattern.compile("User \\d+ logged in")))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("User \\d+ logged in")
        .hasMessageContaining("User abc logged in");
  }

  // ── hasRawTemplate ────────────────────────────────────────────────────────

  @Test
  void hasRawTemplate_passes_onExactMatch() {
    LogEntry entry =
        new LogEntry(
            Instant.now(),
            Level.INFO,
            "com.example.Foo",
            "User 42 logged in",
            "User {} logged in",
            null,
            Map.of(),
            "main",
            1L,
            null);

    assertThat(List.of(entry)).single().hasRawTemplate("User {} logged in");
  }

  @Test
  void hasRawTemplate_fails_onMismatch() {
    LogEntry entry =
        new LogEntry(
            Instant.now(),
            Level.INFO,
            "com.example.Foo",
            "User 42 logged in",
            "User {} logged in",
            null,
            Map.of(),
            "main",
            1L,
            null);

    assertThatThrownBy(() -> assertThat(List.of(entry)).single().hasRawTemplate("Order {} placed"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Order {} placed")
        .hasMessageContaining("User {} logged in");
  }

  // ── hasLevel ──────────────────────────────────────────────────────────────

  @Test
  void hasLevel_passes_whenLevelMatches() {
    LogEntry entry = entry(Level.WARN, "com.example.Foo", "low disk space");

    assertThat(List.of(entry)).single().hasLevel(Level.WARN);
  }

  @Test
  void hasLevel_fails_withActualLevel() {
    LogEntry entry = entry(Level.INFO, "com.example.Foo", "msg");

    assertThatThrownBy(() -> assertThat(List.of(entry)).single().hasLevel(Level.ERROR))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("ERROR")
        .hasMessageContaining("INFO");
  }

  // ── hasLoggerName ─────────────────────────────────────────────────────────

  @Test
  void hasLoggerName_passes_onExactMatch() {
    LogEntry entry = entry(Level.INFO, "com.example.PaymentService", "msg");

    assertThat(List.of(entry)).single().hasLoggerName("com.example.PaymentService");
  }

  @Test
  void hasLoggerName_fails_withActualName() {
    LogEntry entry = entry(Level.INFO, "com.example.PaymentService", "msg");

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasLoggerName("com.example.OrderService"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("com.example.OrderService")
        .hasMessageContaining("com.example.PaymentService");
  }

  // ── hasMdcEntry ───────────────────────────────────────────────────────────

  @Test
  void hasMdcEntry_passes_whenKeyValuePresent() {
    LogEntry entry = entryWithMdc(Level.INFO, "Foo", "msg", Map.of("requestId", "req-456"));

    assertThat(List.of(entry)).single().hasMdcEntry("requestId", "req-456");
  }

  @Test
  void hasMdcEntry_fails_whenKeyAbsent() {
    LogEntry entry = entryWithMdc(Level.INFO, "Foo", "msg", Map.of("other", "value"));

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasMdcEntry("requestId", "expected-value"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("requestId")
        .hasMessageContaining("expected-value");
  }

  @Test
  void hasMdcEntry_fails_whenValueMismatch() {
    LogEntry entry = entryWithMdc(Level.INFO, "Foo", "msg", Map.of("requestId", "actual-value"));

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasMdcEntry("requestId", "expected-value"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("expected-value")
        .hasMessageContaining("actual-value");
  }

  // ── hasThrowable ──────────────────────────────────────────────────────────

  @Test
  void hasThrowable_passes_whenTypeMatches() {
    ThrowableInfo ti = ThrowableInfo.from(new SocketTimeoutException("read timed out"));
    LogEntry entry = entryWithThrowable(Level.ERROR, "com.example.Foo", "Request failed", ti);

    assertThat(List.of(entry)).single().hasThrowable(SocketTimeoutException.class);
  }

  @Test
  void hasThrowable_fails_whenNoThrowableCaptured() {
    LogEntry entry = entry(Level.ERROR, "com.example.Foo", "Request failed");

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasThrowable(SocketTimeoutException.class))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SocketTimeoutException")
        .hasMessageContaining("no throwable was captured");
  }

  @Test
  void hasThrowable_fails_whenTypeMismatch() {
    ThrowableInfo ti = ThrowableInfo.from(new IllegalArgumentException("bad arg"));
    LogEntry entry = entryWithThrowable(Level.ERROR, "com.example.Foo", "error", ti);

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasThrowable(SocketTimeoutException.class))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SocketTimeoutException")
        .hasMessageContaining("IllegalArgumentException");
  }

  // ── hasThrowableWithMessage ───────────────────────────────────────────────

  @Test
  void hasThrowableWithMessage_passes_whenMessageMatches() {
    ThrowableInfo ti = ThrowableInfo.from(new RuntimeException("exact error message"));
    LogEntry entry = entryWithThrowable(Level.ERROR, "Foo", "oops", ti);

    assertThat(List.of(entry)).single().hasThrowableWithMessage("exact error message");
  }

  @Test
  void hasThrowableWithMessage_fails_whenMessageMismatch() {
    ThrowableInfo ti = ThrowableInfo.from(new RuntimeException("actual message"));
    LogEntry entry = entryWithThrowable(Level.ERROR, "Foo", "oops", ti);

    assertThatThrownBy(
            () -> assertThat(List.of(entry)).single().hasThrowableWithMessage("expected message"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("expected message")
        .hasMessageContaining("actual message");
  }

  // ── hasNoThrowable ────────────────────────────────────────────────────────

  @Test
  void hasNoThrowable_passes_whenThrowableIsNull() {
    LogEntry entry = entry(Level.INFO, "com.example.Foo", "everything is fine");

    assertThat(List.of(entry)).single().hasNoThrowable();
  }

  @Test
  void hasNoThrowable_fails_whenThrowableIsPresent() {
    ThrowableInfo ti = ThrowableInfo.from(new RuntimeException("unexpected boom"));
    LogEntry entry = entryWithThrowable(Level.ERROR, "com.example.Foo", "exploded", ti);

    assertThatThrownBy(() -> assertThat(List.of(entry)).single().hasNoThrowable())
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("RuntimeException")
        .hasMessageContaining("unexpected boom");
  }

  // ── Chained assertions ────────────────────────────────────────────────────

  @Test
  void assertionsChain_fluently() {
    ThrowableInfo ti = ThrowableInfo.from(new SocketTimeoutException("connection timed out"));
    LogEntry entry =
        new LogEntry(
            Instant.now(),
            Level.ERROR,
            "com.example.PaymentService",
            "Payment failed for user 42",
            "Payment failed for user {}",
            ti,
            Map.of("requestId", "req-789", "userId", "42"),
            "http-1",
            10L,
            null);

    assertThat(List.of(entry))
        .single()
        .hasLevel(Level.ERROR)
        .hasLoggerName("com.example.PaymentService")
        .hasFormattedMessageContaining("Payment failed")
        .hasRawTemplate("Payment failed for user {}")
        .hasMdcEntry("requestId", "req-789")
        .hasMdcEntry("userId", "42")
        .hasThrowable(SocketTimeoutException.class)
        .hasThrowableWithMessage("connection timed out");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static LogEntry entry(Level level, String loggerName, String message) {
    return new LogEntry(
        Instant.now(), level, loggerName, message, message, null, Map.of(), "main", 1L, null);
  }

  private static LogEntry entryWithMdc(
      Level level, String loggerName, String message, Map<String, String> mdc) {
    return new LogEntry(
        Instant.now(), level, loggerName, message, message, null, mdc, "main", 1L, null);
  }

  private static LogEntry entryWithThrowable(
      Level level, String loggerName, String message, ThrowableInfo throwable) {
    return new LogEntry(
        Instant.now(), level, loggerName, message, message, throwable, Map.of(), "main", 1L, null);
  }
}
