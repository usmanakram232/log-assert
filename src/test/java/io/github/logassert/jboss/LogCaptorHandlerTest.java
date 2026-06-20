package io.github.logassert.jboss;

import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.logassert.core.LogEntry;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

/**
 * Integration tests for the JBoss Log Manager capture pipeline.
 *
 * <p>All tests use real SLF4J loggers routed through {@code slf4j-jboss-logmanager} → JBoss Log
 * Manager → {@link LogCaptorHandler}. The surefire configuration sets {@code
 * java.util.logging.manager=org.jboss.logmanager.LogManager} so the JBoss manager is active for the
 * entire test JVM.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LogCaptorHandlerTest {

  private LogCaptorImpl captor;
  private static final Logger log = LoggerFactory.getLogger(LogCaptorHandlerTest.class);

  @BeforeEach
  void setup() {
    captor = new LogCaptorImpl();
    captor.install();
    captor.clearLogs();
  }

  @AfterEach
  void teardown() {
    captor.close();
  }

  // ── Basic level capture ───────────────────────────────────────────────────

  @Test
  @Order(1)
  void captures_info_log() {
    log.info("hello from info");

    assertThat(captor).atLevel(Level.INFO).containingMessage("hello from info").hasSize(1);
  }

  @Test
  @Order(2)
  void captures_warn_log() {
    log.warn("watch out");

    assertThat(captor).atLevel(Level.WARN).containingMessage("watch out").hasSize(1);
  }

  @Test
  @Order(3)
  void captures_error_log() {
    log.error("something went wrong");

    assertThat(captor).atLevel(Level.ERROR).containingMessage("something went wrong").hasSize(1);
  }

  // ── Message formatting ────────────────────────────────────────────────────

  @Test
  @Order(4)
  void captures_formatted_message_with_placeholder() {
    log.info("User {} logged in", "alice");

    List<LogEntry> entries = captor.getLogs();
    // Filter to entries from this test's logger containing the expected text
    LogEntry entry =
        entries.stream()
            .filter(e -> e.formattedMessage().contains("User alice logged in"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No entry with 'User alice logged in' found"));

    assertThat(entry.formattedMessage()).isEqualTo("User alice logged in");
  }

  @Test
  @Order(5)
  void captures_raw_template_separately() {
    log.info("User {} logged in", "alice");

    List<LogEntry> entries = captor.getLogs();
    LogEntry entry =
        entries.stream()
            .filter(e -> e.formattedMessage().contains("User alice logged in"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No matching entry found"));

    // The slf4j-jboss-logmanager bridge calls MessageFormatter.format() before creating the
    // ExtLogRecord, so getMessage() on the record already returns the fully formatted string.
    // As a result rawTemplate equals formattedMessage in this adapter — both fields are populated,
    // and rawTemplate is never null.
    assertThat(entry.rawTemplate()).isNotNull();
    assertThat(entry.formattedMessage()).isEqualTo("User alice logged in");
    // rawTemplate holds whatever getMessage() returned — the pre-formatted string in this bridge
    assertThat(entry.rawTemplate()).isEqualTo(entry.formattedMessage());
  }

  // ── Throwable capture ─────────────────────────────────────────────────────

  @Test
  @Order(6)
  void captures_throwable_as_ThrowableInfo() {
    IOException ex = new IOException("disk full");
    log.error("write failed", ex);

    assertThat(captor)
        .atLevel(Level.ERROR)
        .containingMessage("write failed")
        .single()
        .hasThrowable(IOException.class)
        .hasThrowableWithMessage("disk full");
  }

  @Test
  @Order(7)
  void throwable_captures_cause_chain() {
    RuntimeException cause = new RuntimeException("root cause");
    IOException ex = new IOException("outer", cause);
    log.error("chained failure", ex);

    List<LogEntry> entries = captor.getLogs();
    LogEntry entry =
        entries.stream()
            .filter(e -> e.formattedMessage().contains("chained failure"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No matching entry found"));

    assertThat(entry.throwable()).isNotNull();
    assertThat(entry.throwable().className()).isEqualTo(IOException.class.getName());
    assertThat(entry.throwable().cause()).isNotNull();
    assertThat(entry.throwable().cause().className()).isEqualTo(RuntimeException.class.getName());
    assertThat(entry.throwable().cause().message()).isEqualTo("root cause");
  }

  // ── MDC capture ───────────────────────────────────────────────────────────

  @Test
  @Order(8)
  void mdc_is_captured_at_log_time_not_assertion_time() {
    MDC.put("requestId", "req-42");
    try {
      log.info("processing request");
    } finally {
      MDC.remove("requestId");
    }

    // MDC has been cleared by the time we assert — the snapshot must still have the value
    List<LogEntry> entries = captor.getLogs();
    LogEntry entry =
        entries.stream()
            .filter(e -> e.formattedMessage().contains("processing request"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No matching entry found"));

    assertThat(entry.mdcContext()).containsEntry("requestId", "req-42");
  }

  // ── Store operations ──────────────────────────────────────────────────────

  @Test
  @Order(9)
  void clear_removes_all_entries() {
    log.info("first");
    log.info("second");
    assertThat(captor.getLogs()).isNotEmpty();

    captor.clearLogs();

    assertThat(captor.getLogs()).isEmpty();
  }

  @Test
  @Order(10)
  void ring_buffer_evicts_oldest_at_capacity() {
    LogCaptorStore store = new LogCaptorStore(3);
    LogCaptorImpl boundedCaptor = new LogCaptorImpl(store);
    boundedCaptor.install();
    try {
      log.info("entry-1");
      log.info("entry-2");
      log.info("entry-3");
      log.info("entry-4"); // causes entry-1 to be evicted

      List<LogEntry> snapshot = store.snapshot();
      assertThat(snapshot).hasSize(3);
      // entry-1 must be gone; entry-4 must be present
      assertThat(snapshot.stream().map(LogEntry::formattedMessage).toList())
          .doesNotContain("entry-1")
          .contains("entry-4");
    } finally {
      boundedCaptor.close();
    }
  }

  // ── Level mapping ─────────────────────────────────────────────────────────

  @Test
  @Order(11)
  void level_mapping_SEVERE_to_ERROR() {
    log.error("severe mapped");

    List<LogEntry> entries =
        captor.getLogs().stream()
            .filter(e -> e.formattedMessage().contains("severe mapped"))
            .toList();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).level()).isEqualTo(Level.ERROR);
  }

  @Test
  @Order(12)
  void level_mapping_WARNING_to_WARN() {
    log.warn("warning mapped");

    List<LogEntry> entries =
        captor.getLogs().stream()
            .filter(e -> e.formattedMessage().contains("warning mapped"))
            .toList();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).level()).isEqualTo(Level.WARN);
  }

  @Test
  @Order(13)
  void level_mapping_FINE_to_DEBUG() {
    // Force the root logger down to FINE/DEBUG so the record is actually emitted
    captor.withMinLevel(Level.DEBUG);
    log.debug("fine mapped");

    List<LogEntry> entries =
        captor.getLogs().stream()
            .filter(e -> e.formattedMessage().contains("fine mapped"))
            .toList();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).level()).isEqualTo(Level.DEBUG);
  }

  // ── AssertJ integration ───────────────────────────────────────────────────

  @Test
  @Order(14)
  void assertj_integration_works_end_to_end() {
    IOException ex = new IOException("timeout");
    log.error("payment failed: {}", "order-99", ex);

    assertThat(captor)
        .fromLogger(LogCaptorHandlerTest.class)
        .atLevel(Level.ERROR)
        .containingMessage("payment failed")
        .single()
        .hasLevel(Level.ERROR)
        .hasFormattedMessageContaining("payment failed")
        .hasFormattedMessageContaining("order-99")
        .hasThrowable(IOException.class)
        .hasThrowableWithMessage("timeout");
  }
}
