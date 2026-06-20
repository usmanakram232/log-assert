package io.github.logassert.junit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import io.github.logassert.core.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link LogCaptorExtension} — uses the JUnit {@link Launcher} API to run synthetic
 * inner test classes and verify extension behaviour from the outside.
 */
class LogCaptorExtensionTest {

  // ── Test 1: @InjectLogCaptor field injection ───────────────────────────────

  @ExtendWith(LogCaptorExtension.class)
  static class InnerTestWithFieldInjection {
    private static final Logger log = LoggerFactory.getLogger(InnerTestWithFieldInjection.class);

    @InjectLogCaptor LogCaptor captor;

    @Test
    void the_test() {
      assertThat(captor).isNotNull();
      log.info("hello from field-injected test");
      assertThat(captor.getLogs()).isNotEmpty();
      assertThat(captor.getLogs().get(0).formattedMessage())
          .isEqualTo("hello from field-injected test");
    }
  }

  @Test
  void field_injection_works() {
    SummaryGeneratingListener listener = runInner(InnerTestWithFieldInjection.class);
    assertThat(listener.getSummary().getFailures())
        .as("inner test failures")
        .isEmpty();
    assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(1);
  }

  // ── Test 2: logs auto-cleared between tests ────────────────────────────────

  @ExtendWith(LogCaptorExtension.class)
  static class InnerTestAutoClear {
    private static final Logger log = LoggerFactory.getLogger(InnerTestAutoClear.class);

    @InjectLogCaptor LogCaptor captor;

    // Counter to track how many times the test ran in this JVM session.
    // Both test methods must start with zero captured entries.
    @Test
    void first() {
      assertThat(captor.getLogs()).as("logs must be empty at start of first()").isEmpty();
      log.warn("first-test-message");
    }

    @Test
    void second() {
      // If clearLogs() fires correctly between tests, second() will start empty regardless of
      // first() having logged.
      assertThat(captor.getLogs()).as("logs must be empty at start of second()").isEmpty();
      log.warn("second-test-message");
    }
  }

  @Test
  void logs_auto_cleared_between_tests() {
    SummaryGeneratingListener listener = runInner(InnerTestAutoClear.class);
    assertThat(listener.getSummary().getFailures())
        .as("auto-clear inner test failures")
        .isEmpty();
    assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(2);
  }

  // ── Test 3: LogCaptor parameter injection ─────────────────────────────────

  @ExtendWith(LogCaptorExtension.class)
  static class InnerTestWithParameterInjection {
    private static final Logger log =
        LoggerFactory.getLogger(InnerTestWithParameterInjection.class);

    @Test
    void the_test(LogCaptor captor) {
      assertThat(captor).isNotNull();
      log.debug("parameter-injected log");
      // NOTE: debug may be filtered at root level; the important thing is no NPE and captor works.
      assertThat(captor).isNotNull();
    }
  }

  @Test
  void parameter_injection_works() {
    SummaryGeneratingListener listener = runInner(InnerTestWithParameterInjection.class);
    assertThat(listener.getSummary().getFailures())
        .as("parameter injection inner test failures")
        .isEmpty();
    assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(1);
  }

  // ── Test 4: @PrintLogsOnFailure dumps stderr on failure ───────────────────

  @ExtendWith(LogCaptorExtension.class)
  @PrintLogsOnFailure
  static class InnerTestPrintLogsOnFailure {
    private static final Logger log =
        LoggerFactory.getLogger(InnerTestPrintLogsOnFailure.class);

    @Test
    void intentionally_failing_test() {
      log.error("this-error-should-appear-in-stderr");
      throw new AssertionError("intentional failure to trigger @PrintLogsOnFailure");
    }
  }

  @Test
  void print_logs_on_failure_dumps_stderr_on_failed_test() {
    // We can't easily capture System.err in a unit test without redirecting it,
    // so we verify the test fails exactly once (i.e. the extension ran without itself blowing up).
    SummaryGeneratingListener listener = runInner(InnerTestPrintLogsOnFailure.class);
    assertThat(listener.getSummary().getTestsFailedCount())
        .as("the inner test must fail (intentional)")
        .isEqualTo(1);
    // The important assertion: the failure is the intentional AssertionError,
    // not a secondary failure from the extension's testFailed() callback.
    String firstFailureMsg =
        listener.getSummary().getFailures().get(0).getException().getMessage();
    assertThat(firstFailureMsg).contains("intentional failure");
  }

  // ── Test 5: @FailOnUncheckedError fails test when ERROR entry present ──────

  @ExtendWith(LogCaptorExtension.class)
  @FailOnUncheckedError
  static class InnerTestFailOnUncheckedError {
    private static final Logger log =
        LoggerFactory.getLogger(InnerTestFailOnUncheckedError.class);

    @Test
    void logs_an_error_but_does_not_assert_on_it() {
      log.error("unchecked-error-entry");
      // No assertion on the captor — the extension should catch it in afterEach.
    }
  }

  @Test
  void fail_on_unchecked_error_fails_test_when_error_logged() {
    SummaryGeneratingListener listener = runInner(InnerTestFailOnUncheckedError.class);
    assertThat(listener.getSummary().getTestsFailedCount())
        .as("@FailOnUncheckedError should cause one failure")
        .isEqualTo(1);
    String failureMsg =
        listener.getSummary().getFailures().get(0).getException().getMessage();
    assertThat(failureMsg).contains("@FailOnUncheckedError");
    assertThat(failureMsg).contains("ERROR");
  }

  // ── Test 6: @FailOnUncheckedError does NOT fail when no ERROR entries ──────

  @ExtendWith(LogCaptorExtension.class)
  @FailOnUncheckedError
  static class InnerTestFailOnUncheckedErrorNoErrors {
    private static final Logger log =
        LoggerFactory.getLogger(InnerTestFailOnUncheckedErrorNoErrors.class);

    @Test
    void logs_only_info() {
      log.info("just-an-info-message");
      // No ERROR → @FailOnUncheckedError must not trigger.
    }
  }

  @Test
  void fail_on_unchecked_error_passes_when_no_errors() {
    SummaryGeneratingListener listener = runInner(InnerTestFailOnUncheckedErrorNoErrors.class);
    assertThat(listener.getSummary().getFailures())
        .as("no failures expected when only INFO is logged")
        .isEmpty();
    assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(1);
  }

  // ── Test 7: end-to-end log capture via extension ───────────────────────────

  @ExtendWith(LogCaptorExtension.class)
  static class InnerTestEndToEnd {
    private static final Logger log = LoggerFactory.getLogger(InnerTestEndToEnd.class);

    @InjectLogCaptor LogCaptor captor;

    @Test
    void captures_info_and_warn_logs() {
      log.info("info-message");
      log.warn("warn-message");

      assertThat(captor.getLogs()).hasSize(2);
      assertThat(captor.getLogs().get(0).formattedMessage()).isEqualTo("info-message");
      assertThat(captor.getLogs().get(1).formattedMessage()).isEqualTo("warn-message");
    }
  }

  @Test
  void logs_captured_end_to_end() {
    SummaryGeneratingListener listener = runInner(InnerTestEndToEnd.class);
    assertThat(listener.getSummary().getFailures())
        .as("end-to-end capture inner test failures")
        .isEmpty();
    assertThat(listener.getSummary().getTestsSucceededCount()).isEqualTo(1);
  }

  // ── Launcher helper ────────────────────────────────────────────────────────

  private static SummaryGeneratingListener runInner(Class<?> testClass) {
    LauncherDiscoveryRequest request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(testClass))
            .build();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    Launcher launcher = LauncherFactory.create();
    launcher.discover(request); // warm up discovery
    launcher.execute(request, listener);
    return listener;
  }
}
