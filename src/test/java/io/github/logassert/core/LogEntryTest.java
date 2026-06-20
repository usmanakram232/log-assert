package io.github.logassert.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class LogEntryTest {

  // ── MDC defensive copy ────────────────────────────────────────────────────

  @Test
  void mdcContext_isUnmodifiable_mutatingBuilderMapDoesNotAffectEntry() {
    Map<String, String> mdc = new HashMap<>();
    mdc.put("requestId", "abc-123");

    LogEntry entry = buildEntry(mdc);

    // Mutate the original map — entry must not be affected
    mdc.put("userId", "42");
    mdc.remove("requestId");

    assertThat(entry.mdcContext()).containsOnlyKeys("requestId");
    assertThat(entry.mdcContext().get("requestId")).isEqualTo("abc-123");
  }

  @Test
  void mdcContext_returnedMap_isUnmodifiable() {
    LogEntry entry = buildEntry(Map.of("k", "v"));

    assertThatThrownBy(() -> entry.mdcContext().put("new", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void mdcContext_emptyMap_isUnmodifiable() {
    LogEntry entry = buildEntry(new HashMap<>());

    assertThatThrownBy(() -> entry.mdcContext().put("k", "v"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  // ── Record accessor coverage ──────────────────────────────────────────────

  @Test
  void allFields_areAccessibleViaRecordAccessors() {
    Instant now = Instant.parse("2024-06-20T10:00:00Z");
    ThrowableInfo throwable = ThrowableInfo.from(new RuntimeException("boom"));
    Map<String, String> mdc = Map.of("traceId", "xyz");

    LogEntry entry =
        new LogEntry(
            now,
            Level.WARN,
            "com.example.PaymentService",
            "Payment failed for user 42",
            "Payment failed for user {}",
            throwable,
            mdc,
            "http-thread-1",
            99L,
            "MY_MARKER");

    assertThat(entry.timestamp()).isEqualTo(now);
    assertThat(entry.level()).isEqualTo(Level.WARN);
    assertThat(entry.loggerName()).isEqualTo("com.example.PaymentService");
    assertThat(entry.formattedMessage()).isEqualTo("Payment failed for user 42");
    assertThat(entry.rawTemplate()).isEqualTo("Payment failed for user {}");
    assertThat(entry.throwable()).isNotNull();
    assertThat(entry.throwable().message()).isEqualTo("boom");
    assertThat(entry.mdcContext()).containsEntry("traceId", "xyz");
    assertThat(entry.threadName()).isEqualTo("http-thread-1");
    assertThat(entry.threadId()).isEqualTo(99L);
    assertThat(entry.markerName()).isEqualTo("MY_MARKER");
  }

  @Test
  void nullableFields_acceptNull() {
    LogEntry entry =
        new LogEntry(
            Instant.now(),
            Level.INFO,
            "com.example.Foo",
            "msg",
            "msg",
            null, // throwable — nullable
            Map.of(),
            "main",
            1L,
            null // markerName — nullable
            );

    assertThat(entry.throwable()).isNull();
    assertThat(entry.markerName()).isNull();
  }

  // ── Record equals / hashCode ──────────────────────────────────────────────

  @Test
  void twoEntriesWithEqualFields_areEqual() {
    Instant fixed = Instant.parse("2024-01-01T00:00:00Z");
    Map<String, String> mdc = Map.of("k", "v");

    LogEntry e1 =
        new LogEntry(
            fixed, Level.INFO, "com.example.Foo", "hello", "hello", null, mdc, "main", 1L, null);
    LogEntry e2 =
        new LogEntry(
            fixed, Level.INFO, "com.example.Foo", "hello", "hello", null, mdc, "main", 1L, null);

    assertThat(e1).isEqualTo(e2);
    assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
  }

  @Test
  void twoEntriesWithDifferentLevel_areNotEqual() {
    Instant fixed = Instant.parse("2024-01-01T00:00:00Z");

    LogEntry e1 =
        new LogEntry(
            fixed, Level.INFO, "com.example.Foo", "msg", "msg", null, Map.of(), "main", 1L, null);
    LogEntry e2 =
        new LogEntry(
            fixed, Level.ERROR, "com.example.Foo", "msg", "msg", null, Map.of(), "main", 1L, null);

    assertThat(e1).isNotEqualTo(e2);
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private static LogEntry buildEntry(Map<String, String> mdc) {
    return new LogEntry(
        Instant.now(),
        Level.INFO,
        "com.example.Foo",
        "hello",
        "hello",
        null,
        mdc,
        "main",
        1L,
        null);
  }
}
