# log-assert

In-memory log capture with AssertJ-style assertions for JUnit 5.

Designed for **Quarkus** and any **JBoss Log Manager** environment.  
SLF4J API + JUnit 5 extension + fluent AssertJ DSL.

---

## Quick start

### Maven dependency

```xml
<dependency>
  <groupId>io.github.usmanakram232</groupId>
  <artifactId>log-assert</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

### Minimal example

```java
import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;
import io.github.logassert.core.LogCaptor;
import io.github.logassert.junit5.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.slf4j.event.Level;

@ExtendWith(LogCaptorExtension.class)
class OrderServiceTest {

    @InjectLogCaptor
    LogCaptor logCaptor;       // auto-injected and auto-cleared before each test

    @Test
    void payment_failure_is_logged() {
        orderService.processPayment(invalidRequest);

        assertThat(logCaptor)
            .atLevel(Level.ERROR)
            .fromLogger(OrderService.class)
            .hasSize(1)
            .single()
            .hasFormattedMessageContaining("Payment rejected")
            .hasThrowable(IllegalArgumentException.class);
    }
}
```

---

## Quarkus setup

Quarkus re-initialises the JBoss Log Manager after the extension class is loaded, which would
uninstall any handler registered via `@ExtendWith`. Use the **`@RegisterExtension static`** form
so the extension runs *after* Quarkus has finished resetting the log manager:

```java
import io.quarkus.test.junit.QuarkusTest;
import io.github.logassert.junit5.*;
import org.junit.jupiter.api.extension.RegisterExtension;

@QuarkusTest
class PaymentResourceTest {

    @RegisterExtension                         // MUST be static
    static LogCaptorExtension logExt = LogCaptorExtension.create();

    @InjectLogCaptor
    LogCaptor logCaptor;

    @Test
    void captures_payment_error() {
        // ... trigger code under test ...

        assertThat(logCaptor)
            .atLevel(Level.ERROR)
            .isNotEmpty();
    }
}
```

> **Why `static`?**  JUnit 5's `@RegisterExtension` on a *static* field registers the extension
> as a class-level extension (like `@BeforeAll`/`@AfterAll`), giving it a chance to install the
> capture handler *after* the Quarkus test machinery has finished setting up the log manager.
> A non-static field would register it as an instance-level extension, which runs too early.

---

## Annotations

### `@InjectLogCaptor`

Marks a `LogCaptor` field on a test class for automatic injection by `LogCaptorExtension`. The
field is set (and the captor is cleared) before each test method.

```java
@InjectLogCaptor
LogCaptor captor;
```

The captor can also be received as a method parameter:

```java
@Test
void example(LogCaptor captor) { ... }
```

### `@PrintLogsOnFailure`

When present on a test class or method, dumps all captured log entries to `System.err` when that
test fails. Useful for diagnosing flaky or unexpected failures without permanently enabling verbose
logging.

```java
@ExtendWith(LogCaptorExtension.class)
@PrintLogsOnFailure                    // dump logs whenever any test in this class fails
class MyTest { ... }
```

Can also be placed on a single method:

```java
@Test
@PrintLogsOnFailure                    // dump logs only when this test fails
void noisy_integration_test() { ... }
```

### `@EchoLogs`

Re-enables console log output for the annotated test scope. By default, `LogCaptorExtension`
detaches all console/stream handlers from the root logger to prevent test noise. Annotating with
`@EchoLogs` signals that log output should still be forwarded to the console for that scope.

```java
@EchoLogs(minimumLevel = Level.DEBUG)   // echo DEBUG and above to console during this test
@Test
void debug_heavy_test() { ... }
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `minimumLevel` | `TRACE` | Minimum SLF4J level to echo. |

### `@FailOnUncheckedError`

Causes the test to fail in `afterEach` if any `ERROR`-level log entries remain in the captor that
were not asserted on. This is a safety net to catch unexpected error logging.

```java
@ExtendWith(LogCaptorExtension.class)
@FailOnUncheckedError                  // any un-asserted ERROR entry fails the test
class StrictServiceTest { ... }
```

> The check fires *after* the test body. If the test body already threw an `AssertionError`, the
> `@FailOnUncheckedError` failure is reported as a *separate* failure.

---

## Assertion API reference

### Entry point

```java
import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;
```

`LogCaptorAssertions` is deliberately named to avoid clashing with AssertJ's own
`org.assertj.core.api.Assertions` when both are statically imported in the same class.

```java
// From a LogCaptor (snapshot taken at call time)
assertThat(logCaptor) ...

// From a pre-fetched list
List<LogEntry> entries = logCaptor.getLogs();
assertThat(entries) ...
```

### `LogsAssert` — filters (chainable, return new `LogsAssert`)

| Method | Description |
|--------|-------------|
| `atLevel(Level level)` | Keep only entries at exactly the given SLF4J level. |
| `fromLogger(Class<?> clazz)` | Keep only entries whose logger name starts with `clazz.getName()`. |
| `fromLogger(String prefix)` | Keep only entries whose logger name starts with `prefix`. |
| `containingMessage(String substring)` | Keep only entries whose formatted message contains `substring` (case-sensitive). |
| `withMdcEntry(String key, String value)` | Keep only entries whose MDC context has `key` mapped to `value`. |

Filters return a **new** `LogsAssert` over the narrowed list and do not mutate the original.

### `LogsAssert` — terminal assertions

| Method | Description |
|--------|-------------|
| `hasSize(int n)` | Assert exactly `n` entries. Failure message lists all captured entries. |
| `isEmpty()` | Assert no entries captured. |
| `isNotEmpty()` | Assert at least one entry captured. |

### `LogsAssert` — navigation (return `LogEntryAssert`)

| Method | Description |
|--------|-------------|
| `single()` | Assert exactly one entry, then return an assertion for it. |
| `first()` | Assert at least one entry, then return an assertion for the first. |
| `last()` | Assert at least one entry, then return an assertion for the last. |

### `LogEntryAssert` — single-entry assertions

#### Message

| Method | Description |
|--------|-------------|
| `hasFormattedMessage(String expected)` | Assert the fully-resolved message equals `expected`. |
| `hasFormattedMessageContaining(String substring)` | Assert the resolved message contains `substring`. |
| `hasFormattedMessageMatching(Pattern regex)` | Assert the resolved message matches the regex (uses `find()`, not `matches()`). |
| `hasRawTemplate(String expected)` | Assert the raw SLF4J template before `{}` substitution. |

> **Bridge note:** when using `slf4j-jboss-logmanager`, both `formattedMessage` and `rawTemplate`
> contain the fully resolved string. Use `hasFormattedMessageContaining` in all environments.

#### Level & logger

| Method | Description |
|--------|-------------|
| `hasLevel(Level level)` | Assert the log level exactly. |
| `hasLoggerName(String name)` | Assert the fully-qualified logger name exactly. |

#### MDC

| Method | Description |
|--------|-------------|
| `hasMdcEntry(String key, String value)` | Assert the MDC context at log time contained `key → value`. |

#### Throwable

| Method | Description |
|--------|-------------|
| `hasThrowable(Class<? extends Throwable> type)` | Assert a throwable was captured and its class equals `type`. |
| `hasThrowableWithMessage(String message)` | Assert a throwable was captured and its message equals `message`. |
| `hasNoThrowable()` | Assert no throwable was attached to this log entry. |

---

## `LogCaptor` API

```java
logCaptor.getLogs()              // unmodifiable snapshot at call time; call again for fresh view
logCaptor.clearLogs()            // remove all captured entries (called automatically in beforeEach)
logCaptor.withMinLevel(Level)    // lower effective capture level (restored in afterEach)
logCaptor.resetConfiguration()   // restore original log level (called automatically in afterEach)
logCaptor.close()                // resetConfiguration() + uninstall handler (called in afterAll)
```

---

## `LogEntry` fields

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | `Instant` | Wall-clock time of the event. |
| `level` | `org.slf4j.event.Level` | SLF4J level (TRACE/DEBUG/INFO/WARN/ERROR). |
| `loggerName` | `String` | Fully-qualified logger name. |
| `formattedMessage` | `String` | Fully resolved message — `{}` placeholders substituted. |
| `rawTemplate` | `String` | Raw SLF4J template before substitution (may equal `formattedMessage`). |
| `throwable` | `ThrowableInfo` | Serialized exception snapshot, or `null`. |
| `mdcContext` | `Map<String,String>` | Unmodifiable copy of MDC at log time. |
| `threadName` | `String` | Name of the thread that emitted the event. |
| `threadId` | `long` | ID of the thread that emitted the event. |
| `markerName` | `String` | SLF4J marker name — always `null` in v1. |

---

## Full example

```java
import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;

import io.github.logassert.core.LogCaptor;
import io.github.logassert.junit5.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

@ExtendWith(LogCaptorExtension.class)
@FailOnUncheckedError                  // unexpected ERRORs will fail the test
@PrintLogsOnFailure                    // show captured logs when a test fails
class UserServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);

    @InjectLogCaptor
    LogCaptor logCaptor;

    @Test
    void info_with_mdc() {
        MDC.put("userId", "alice");
        log.info("User {} logged in", "alice");

        assertThat(logCaptor)
            .atLevel(Level.INFO)
            .withMdcEntry("userId", "alice")
            .single()
            .hasFormattedMessageContaining("alice")
            .hasNoThrowable();
    }

    @Test
    void error_with_exception() {
        RuntimeException ex = new RuntimeException("boom");
        log.error("Processing failed", ex);

        assertThat(logCaptor)
            .atLevel(Level.ERROR)
            .single()
            .hasFormattedMessageContaining("Processing failed")
            .hasThrowable(RuntimeException.class)
            .hasThrowableWithMessage("boom");
    }

    @Test
    @EchoLogs(minimumLevel = Level.DEBUG)   // print logs to console for this test only
    void debug_trace_scenario() {
        log.debug("step 1");
        log.debug("step 2");

        assertThat(logCaptor)
            .atLevel(Level.DEBUG)
            .hasSize(2);
    }
}
```

---

## Limitations

- **JBoss Log Manager only.** The capture handler is a JBoss `ExtHandler`; it does not work with
  Logback or java.util.logging alone.
- **No concurrent test isolation.** The capture handler is attached to the root logger and sees
  all log events in the JVM. Tests running concurrently share the same captor; log isolation is
  not guaranteed. A warning is printed when `@Execution(CONCURRENT)` is detected.
- **`rawTemplate` not available via JBoss bridge.** When routing SLF4J through
  `slf4j-jboss-logmanager`, both `formattedMessage` and `rawTemplate` contain the resolved
  message. The raw template is not preserved by the bridge.
- **Markers not supported in v1.** `LogEntry.markerName()` is always `null`.
