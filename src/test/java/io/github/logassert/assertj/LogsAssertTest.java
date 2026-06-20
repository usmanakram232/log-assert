package io.github.logassert.assertj;

import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.logassert.core.LogEntry;
import io.github.logassert.core.ThrowableInfo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class LogsAssertTest {

  // ── Filter: atLevel ───────────────────────────────────────────────────────

  @Test
  void atLevel_keepsOnlyEntriesAtThatLevel() {
    List<LogEntry> entries =
        List.of(
            entry(Level.ERROR, "com.example.Foo", "error happened"),
            entry(Level.INFO, "com.example.Foo", "all good"),
            entry(Level.WARN, "com.example.Bar", "warning here"));

    assertThat(entries).atLevel(Level.ERROR).hasSize(1);
  }

  @Test
  void atLevel_noMatch_returnsEmptyList() {
    List<LogEntry> entries =
        List.of(
            entry(Level.INFO, "com.example.Foo", "info"),
            entry(Level.WARN, "com.example.Foo", "warn"));

    assertThat(entries).atLevel(Level.DEBUG).isEmpty();
  }

  @Test
  void atLevel_multipleMatches_returnsAll() {
    List<LogEntry> entries =
        List.of(
            entry(Level.ERROR, "com.example.Foo", "first error"),
            entry(Level.INFO, "com.example.Foo", "info"),
            entry(Level.ERROR, "com.example.Bar", "second error"));

    assertThat(entries).atLevel(Level.ERROR).hasSize(2);
  }

  // ── Filter: fromLogger(Class) ─────────────────────────────────────────────

  @Test
  void fromLogger_withClass_usesPrefixMatch() {
    String thisClassName =
        LogsAssertTest.class.getName(); // io.github.logassert.assertj.LogsAssertTest
    List<LogEntry> entries =
        List.of(
            entry(Level.INFO, thisClassName, "from this class"),
            entry(Level.INFO, thisClassName + "$Inner", "from inner class"),
            entry(Level.INFO, "com.unrelated.Foo", "from other class"));

    assertThat(entries).fromLogger(LogsAssertTest.class).hasSize(2);
  }

  // ── Filter: fromLogger(String) ────────────────────────────────────────────

  @Test
  void fromLogger_withString_usesPrefixMatch() {
    List<LogEntry> entries =
        List.of(
            entry(Level.INFO, "com.example.service.PaymentService", "payment"),
            entry(Level.INFO, "com.example.service.UserService", "user"),
            entry(Level.INFO, "com.other.Controller", "other"));

    assertThat(entries).fromLogger("com.example.service").hasSize(2);
  }

  @Test
  void fromLogger_exactMatch_alsoWorks() {
    List<LogEntry> entries =
        List.of(
            entry(Level.INFO, "com.example.Foo", "exact"),
            entry(Level.INFO, "com.example.FooBar", "longer"), // starts with same prefix
            entry(Level.INFO, "com.example.Bar", "different"));

    assertThat(entries).fromLogger("com.example.Foo").hasSize(2); // "Foo" and "FooBar"
  }

  // ── Filter: containingMessage ─────────────────────────────────────────────

  @Test
  void containingMessage_filtersOnFormattedMessage() {
    List<LogEntry> entries =
        List.of(
            entry(Level.ERROR, "com.example.Foo", "Payment failed: timeout"),
            entry(Level.ERROR, "com.example.Foo", "Payment succeeded"),
            entry(Level.INFO, "com.example.Foo", "Request received: failed validation"));

    assertThat(entries).containingMessage("failed").hasSize(2);
  }

  @Test
  void containingMessage_caseSensitive() {
    List<LogEntry> entries = List.of(entry(Level.INFO, "com.example.Foo", "Hello World"));

    assertThat(entries).containingMessage("Hello").hasSize(1);
    assertThat(entries).containingMessage("hello").isEmpty(); // case-sensitive
  }

  // ── Filter: withMdcEntry ──────────────────────────────────────────────────

  @Test
  void withMdcEntry_keepsOnlyMatchingEntries() {
    List<LogEntry> entries =
        List.of(
            entryWithMdc(Level.INFO, "com.example.Foo", "msg1", Map.of("requestId", "req-1")),
            entryWithMdc(Level.INFO, "com.example.Foo", "msg2", Map.of("requestId", "req-2")),
            entryWithMdc(Level.INFO, "com.example.Foo", "msg3", Map.of("other", "value")));

    assertThat(entries).withMdcEntry("requestId", "req-1").hasSize(1);
  }

  // ── Terminal: hasSize ─────────────────────────────────────────────────────

  @Test
  void hasSize_passes_whenCountMatches() {
    assertThat(List.of(entry(Level.INFO, "Foo", "a"), entry(Level.INFO, "Foo", "b"))).hasSize(2);
  }

  @Test
  void hasSize_fails_withFullEntryListInMessage() {
    List<LogEntry> entries =
        List.of(
            entry(Level.ERROR, "com.example.PaymentService", "Payment failed"),
            entry(Level.WARN, "com.example.UserService", "User not found"));

    assertThatThrownBy(() -> assertThat(entries).hasSize(5))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected 5 log entries but found 2")
        .hasMessageContaining("[ERROR]")
        .hasMessageContaining("com.example.PaymentService")
        .hasMessageContaining("Payment failed")
        .hasMessageContaining("[WARN]")
        .hasMessageContaining("com.example.UserService")
        .hasMessageContaining("User not found");
  }

  @Test
  void hasSize_fails_forEmptyList_showsNone() {
    assertThatThrownBy(() -> assertThat(List.<LogEntry>of()).hasSize(1))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected 1 log entries but found 0")
        .hasMessageContaining("(none)");
  }

  // ── Terminal: isEmpty ─────────────────────────────────────────────────────

  @Test
  void isEmpty_passes_onEmptyList() {
    assertThat(List.<LogEntry>of()).isEmpty();
  }

  @Test
  void isEmpty_fails_withEntryListInMessage() {
    List<LogEntry> entries = List.of(entry(Level.ERROR, "com.example.Foo", "unexpected error"));

    assertThatThrownBy(() -> assertThat(entries).isEmpty())
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected no log entries but found 1")
        .hasMessageContaining("[ERROR]")
        .hasMessageContaining("com.example.Foo")
        .hasMessageContaining("unexpected error");
  }

  // ── Terminal: isNotEmpty ──────────────────────────────────────────────────

  @Test
  void isNotEmpty_passes_whenEntriesExist() {
    assertThat(List.of(entry(Level.INFO, "Foo", "x"))).isNotEmpty();
  }

  @Test
  void isNotEmpty_fails_withDescriptiveMessage() {
    assertThatThrownBy(() -> assertThat(List.<LogEntry>of()).isNotEmpty())
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected at least one log entry but the list was empty");
  }

  // ── Navigation: single ────────────────────────────────────────────────────

  @Test
  void single_returnsLogEntryAssert_whenExactlyOneEntry() {
    List<LogEntry> entries = List.of(entry(Level.ERROR, "com.example.Foo", "single error"));

    LogEntryAssert ea = assertThat(entries).single();
    // Verify it wraps the correct entry
    ea.hasFormattedMessage("single error");
  }

  @Test
  void single_fails_withAssertionError_whenNoEntries() {
    assertThatThrownBy(() -> assertThat(List.<LogEntry>of()).single())
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected 1 log entries but found 0");
  }

  @Test
  void single_fails_withAssertionError_whenTwoEntries() {
    List<LogEntry> entries =
        List.of(entry(Level.INFO, "Foo", "first"), entry(Level.INFO, "Foo", "second"));

    assertThatThrownBy(() -> assertThat(entries).single())
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected 1 log entries but found 2");
  }

  // ── Navigation: first / last ──────────────────────────────────────────────

  @Test
  void first_returnsFirstEntry() {
    List<LogEntry> entries =
        List.of(entry(Level.INFO, "Foo", "alpha"), entry(Level.INFO, "Foo", "beta"));

    assertThat(entries).first().hasFormattedMessage("alpha");
  }

  @Test
  void last_returnsLastEntry() {
    List<LogEntry> entries =
        List.of(entry(Level.INFO, "Foo", "alpha"), entry(Level.INFO, "Foo", "beta"));

    assertThat(entries).last().hasFormattedMessage("beta");
  }

  // ── Filter chaining ───────────────────────────────────────────────────────

  @Test
  void filterChaining_atLevelThenContainingMessage() {
    List<LogEntry> entries =
        List.of(
            entry(Level.ERROR, "com.example.Foo", "Payment failed: timeout"),
            entry(Level.ERROR, "com.example.Foo", "Connection refused"),
            entry(Level.WARN, "com.example.Foo", "Payment retry attempted"));

    assertThat(entries)
        .atLevel(Level.ERROR)
        .containingMessage("failed")
        .single()
        .hasFormattedMessage("Payment failed: timeout");
  }

  @Test
  void filterChaining_threeLayers() {
    List<LogEntry> entries =
        List.of(
            entryWithMdc(Level.ERROR, "com.example.PaymentService", "fail", Map.of("env", "prod")),
            entryWithMdc(Level.ERROR, "com.example.PaymentService", "fail", Map.of("env", "dev")),
            entryWithMdc(Level.WARN, "com.example.PaymentService", "warn", Map.of("env", "prod")),
            entryWithMdc(Level.ERROR, "com.example.UserService", "fail", Map.of("env", "prod")));

    assertThat(entries)
        .atLevel(Level.ERROR)
        .fromLogger("com.example.PaymentService")
        .withMdcEntry("env", "prod")
        .hasSize(1);
  }

  // ── Throwable formatting in failure messages ──────────────────────────────

  @Test
  void hasSize_failureMessage_includesThrowableSimpleClassName() {
    ThrowableInfo ti = ThrowableInfo.from(new java.net.SocketTimeoutException("timed out"));
    LogEntry entryWithEx =
        new LogEntry(
            Instant.now(),
            Level.ERROR,
            "com.example.Foo",
            "Request failed",
            "Request failed",
            ti,
            Map.of(),
            "main",
            1L,
            null);

    assertThatThrownBy(() -> assertThat(List.of(entryWithEx)).hasSize(0))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SocketTimeoutException");
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
}
