package io.github.logassert.junit5;

import io.github.logassert.core.LogCaptor;
import io.github.logassert.core.LogEntry;
import io.github.logassert.jboss.LogCaptorImpl;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit 5 extension that provides in-memory log capture for test classes.
 *
 * <p>Register on a test class with:
 *
 * <pre>{@code
 * @ExtendWith(LogCaptorExtension.class)
 * class MyTest { ... }
 * }</pre>
 *
 * or via a static field:
 *
 * <pre>{@code
 * @RegisterExtension
 * static LogCaptorExtension extension = LogCaptorExtension.create();
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong>
 *
 * <ul>
 *   <li>{@code beforeAll} — installs the log capture handler and runs a self-test probe.
 *   <li>{@code beforeEach} — clears captured logs, resets MDC, injects {@link InjectLogCaptor}
 *       fields.
 *   <li>{@code afterEach} — checks {@link FailOnUncheckedError}, resets level overrides.
 *   <li>{@code testFailed} — dumps logs to {@code System.err} when {@link PrintLogsOnFailure} is
 *       present.
 *   <li>{@code afterAll} — uninstalls the handler and re-attaches any console handlers.
 * </ul>
 *
 * <p>Supports {@link LogCaptor} as a test method parameter via {@link ParameterResolver}.
 */
public final class LogCaptorExtension
    implements BeforeAllCallback,
        BeforeEachCallback,
        AfterEachCallback,
        AfterAllCallback,
        TestWatcher,
        ParameterResolver {

  /** Namespace used to scope values stored in the {@link ExtensionContext.Store}. */
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(LogCaptorExtension.class);

  private static final String CAPTOR_KEY = "logCaptor";
  private static final String CONSOLE_HANDLERS_KEY = "consoleHandlers";

  /** Factory method — allows using {@code @RegisterExtension static} syntax. */
  public static LogCaptorExtension create() {
    return new LogCaptorExtension();
  }

  // ── BeforeAllCallback ──────────────────────────────────────────────────────

  @Override
  public void beforeAll(ExtensionContext ctx) {
    // 1. Force JBoss LogManager initialization so the root logger exists.
    java.util.logging.LogManager.getLogManager().getLogger("");

    // 2. Warn (don't fail) if concurrent execution is detected — log isolation is not guaranteed.
    if (isConcurrentExecution(ctx)) {
      System.err.println(
          "[log-assert] WARNING: @Execution(CONCURRENT) detected on "
              + ctx.getRequiredTestClass().getSimpleName()
              + ". LogCaptor does not guarantee entry isolation between concurrent tests.");
    }

    // 3. Detach console/stream handlers from the root logger to suppress test noise.
    java.util.logging.Logger root =
        java.util.logging.LogManager.getLogManager().getLogger("");
    List<java.util.logging.Handler> consoleHandlers =
        Arrays.stream(root.getHandlers())
            .filter(
                h ->
                    h instanceof java.util.logging.StreamHandler
                        || h.getClass().getName().contains("Console")
                        || h.getClass().getName().contains("console"))
            .collect(Collectors.toCollection(ArrayList::new));
    consoleHandlers.forEach(root::removeHandler);
    ctx.getStore(NAMESPACE).put(CONSOLE_HANDLERS_KEY, consoleHandlers);

    // 4. Create and install the capture handler.
    LogCaptorImpl captor = new LogCaptorImpl();
    captor.install();
    ctx.getStore(NAMESPACE).put(CAPTOR_KEY, captor);

    // 5. Self-test: emit a known probe entry and verify it was captured.
    captor.clearLogs();
    java.util.logging.Logger testLogger =
        java.util.logging.LogManager.getLogManager().getLogger("");
    testLogger.severe("__log_assert_self_test__");
    List<LogEntry> selfTestEntries = captor.getLogs();
    if (selfTestEntries.stream()
        .noneMatch(e -> e.formattedMessage().contains("__log_assert_self_test__"))) {
      // Re-attach console handlers before throwing — don't leave them detached on failure.
      consoleHandlers.forEach(root::addHandler);
      captor.uninstall();
      throw new LogCaptorConfigurationException(
          "[log-assert] Self-test FAILED: LogCaptorHandler was installed on the root logger "
              + "but did not receive the probe log entry. "
              + "This usually means another component reset the root logger's handler list "
              + "after installation. Consider using LogCaptorQuarkusResource instead of "
              + "@RegisterExtension static for Quarkus tests.");
    }
    captor.clearLogs(); // discard the self-test entry before any test runs
  }

  // ── BeforeEachCallback ─────────────────────────────────────────────────────

  @Override
  public void beforeEach(ExtensionContext ctx) {
    LogCaptorImpl captor = getCaptor(ctx);
    captor.clearLogs();
    // Clear SLF4J MDC — it is thread-local; start each test with a clean slate.
    org.slf4j.MDC.clear();
    // Inject @InjectLogCaptor fields on the test instance.
    ctx.getTestInstance().ifPresent(instance -> injectFields(instance, captor));
  }

  // ── AfterEachCallback ──────────────────────────────────────────────────────

  @Override
  public void afterEach(ExtensionContext ctx) {
    LogCaptorImpl captor = getCaptor(ctx);
    // Always reset level overrides first.
    captor.resetConfiguration();
    // @FailOnUncheckedError: fail if any ERROR entries were not consumed by assertions.
    if (hasAnnotation(ctx, FailOnUncheckedError.class)) {
      List<LogEntry> errors =
          captor.getLogs().stream()
              .filter(e -> e.level() == org.slf4j.event.Level.ERROR)
              .toList();
      if (!errors.isEmpty()) {
        throw new AssertionError(
            String.format(
                "[log-assert] @FailOnUncheckedError: %d ERROR log entr%s were not asserted:%n%s",
                errors.size(),
                errors.size() == 1 ? "y" : "ies",
                formatEntries(errors)));
      }
    }
  }

  // ── AfterAllCallback ───────────────────────────────────────────────────────

  @Override
  @SuppressWarnings("unchecked")
  public void afterAll(ExtensionContext ctx) {
    java.util.logging.Logger root =
        java.util.logging.LogManager.getLogManager().getLogger("");
    // Use try/finally to guarantee console handlers are re-attached even if uninstall() throws.
    try {
      LogCaptorImpl captor = (LogCaptorImpl) ctx.getStore(NAMESPACE).get(CAPTOR_KEY);
      if (captor != null) {
        captor.uninstall();
      }
    } finally {
      List<java.util.logging.Handler> consoleHandlers =
          (List<java.util.logging.Handler>) ctx.getStore(NAMESPACE).get(CONSOLE_HANDLERS_KEY);
      if (consoleHandlers != null) {
        consoleHandlers.forEach(root::addHandler);
      }
    }
  }

  // ── TestWatcher ────────────────────────────────────────────────────────────

  @Override
  public void testFailed(ExtensionContext ctx, Throwable cause) {
    if (hasAnnotation(ctx, PrintLogsOnFailure.class)) {
      LogCaptorImpl captor = getCaptor(ctx);
      List<LogEntry> logs = captor.getLogs();
      if (!logs.isEmpty()) {
        System.err.println(
            "[log-assert] Captured logs for failed test '" + ctx.getDisplayName() + "':");
        logs.forEach(e -> System.err.println(formatEntry(e)));
      }
    }
  }

  // ── ParameterResolver ──────────────────────────────────────────────────────

  @Override
  public boolean supportsParameter(ParameterContext paramCtx, ExtensionContext extCtx) {
    return LogCaptor.class.isAssignableFrom(paramCtx.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext paramCtx, ExtensionContext extCtx) {
    return getCaptor(extCtx);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Retrieves the {@link LogCaptorImpl} stored during {@code beforeAll}.
   *
   * <p>Walks up the {@link ExtensionContext} parent chain because {@code beforeAll} stores in the
   * class-level context while {@code beforeEach}/{@code afterEach} have a method-level context.
   */
  private LogCaptorImpl getCaptor(ExtensionContext ctx) {
    ExtensionContext current = ctx;
    while (current != null) {
      Object value = current.getStore(NAMESPACE).get(CAPTOR_KEY);
      if (value != null) {
        return (LogCaptorImpl) value;
      }
      current = current.getParent().orElse(null);
    }
    throw new IllegalStateException(
        "[log-assert] LogCaptorImpl not found in ExtensionContext store. "
            + "Did you register LogCaptorExtension correctly?");
  }

  /**
   * Injects the captor into all {@link InjectLogCaptor}-annotated {@link LogCaptor} fields on
   * {@code testInstance}, walking the class hierarchy.
   */
  private void injectFields(Object testInstance, LogCaptor captor) {
    Class<?> clazz = testInstance.getClass();
    while (clazz != null && clazz != Object.class) {
      for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(InjectLogCaptor.class)
            && LogCaptor.class.isAssignableFrom(field.getType())) {
          field.setAccessible(true);
          try {
            field.set(testInstance, captor);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(
                "[log-assert] Cannot inject @InjectLogCaptor field: " + field.getName(), e);
          }
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  /**
   * Returns {@code true} if the annotation is present on the test method or test class (in that
   * order of precedence).
   */
  private boolean hasAnnotation(
      ExtensionContext ctx, Class<? extends Annotation> annotation) {
    Optional<? extends Annotation> onMethod =
        ctx.getTestMethod()
            .flatMap(m -> AnnotationSupport.findAnnotation(m, annotation));
    if (onMethod.isPresent()) {
      return true;
    }
    return ctx.getTestClass()
        .flatMap(c -> AnnotationSupport.findAnnotation(c, annotation))
        .isPresent();
  }

  /** Returns {@code true} when the test class is annotated with {@code @Execution(CONCURRENT)}. */
  private boolean isConcurrentExecution(ExtensionContext ctx) {
    return AnnotationSupport.findAnnotation(ctx.getRequiredTestClass(), Execution.class)
        .map(e -> e.value() == ExecutionMode.CONCURRENT)
        .orElse(false);
  }

  private static String formatEntries(List<LogEntry> entries) {
    return entries.stream().map(LogCaptorExtension::formatEntry).collect(Collectors.joining("\n"));
  }

  private static String formatEntry(LogEntry e) {
    String base =
        String.format("  [%s] %s - %s", e.level(), e.loggerName(), e.formattedMessage());
    if (e.throwable() != null) {
      base += " (" + e.throwable().simpleClassName() + ")";
    }
    return base;
  }
}
