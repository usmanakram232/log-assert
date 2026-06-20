# AGENTS.md — log-assert

Agent instructions for working in this repository. Read this before touching any file.

---

## What this project is

`log-assert` is a **JUnit 5 test-utility library** for Java/Quarkus projects.  
It captures log records from **JBoss Log Manager** in-memory during a test and exposes them
via a fluent **AssertJ-style DSL**.  
It is published to Maven Central under `io.github.usmanakram232:log-assert`.

This is a **library** — it runs inside users' test suites. Every design decision must account
for the fact that this code executes in someone else's process.

---

## Non-negotiable constraints

These are invariants. Never violate them regardless of how convenient it would be.

| # | Invariant |
|---|-----------|
| 1 | **No network I/O.** No HTTP, no DNS, no socket of any kind — ever, for any reason. |
| 2 | **No file-system writes** outside of what JBoss Log Manager itself does. |
| 3 | **No `Runtime.exec()` or `ProcessBuilder`** anywhere in `src/main/`. |
| 4 | **No `System.exit()`** — never kill the test runner. |
| 5 | **No hardcoded secrets** — credentials, tokens, API keys must never appear in source or git. |
| 6 | **No SNAPSHOT dependencies** in released artifacts. |
| 7 | **`LogCaptorStore` must be fully thread-safe** — `append`, `snapshot`, and `clear` must all be `synchronized`. |
| 8 | **Every public API method must have a Javadoc comment.** |
| 9 | **`settings-central.xml` must never be committed.** It is gitignored; keep it that way. |

---

## Package layout

```
src/main/java/io/github/logassert/
│
├── core/                   Pure-Java API — no framework dependencies
│   ├── LogCaptor.java      Public interface: getLogs(), clearLogs(), withMinLevel(), close()
│   ├── LogEntry.java       Immutable value type: level, loggerName, formattedMessage, throwable
│   └── ThrowableInfo.java  Immutable snapshot of a Throwable (simpleClassName, message, cause)
│
├── jboss/                  JBoss Log Manager adapter
│   ├── LogCaptorHandler.java   JUL Handler — translates LogRecord → LogEntry, feeds LogCaptorStore
│   ├── LogCaptorImpl.java      LogCaptor implementation: install/uninstall handler, delegate to store
│   └── LogCaptorStore.java     Thread-safe ring-buffer of LogEntry (ConcurrentLinkedDeque, cap 10_000)
│
└── junit5/                 JUnit 5 extension + annotations
    ├── LogCaptorExtension.java           BeforeAll/AfterAll/BeforeEach/AfterEach/TestWatcher
    ├── InjectLogCaptor.java              Field injection annotation
    ├── EchoLogs.java                     Re-attach console handlers for one test
    ├── FailOnUncheckedError.java         Fail if ERROR entries remain after test
    ├── PrintLogsOnFailure.java           Dump captured logs to stderr on test failure
    └── LogCaptorConfigurationException   RuntimeException for misconfiguration
```

---

## Domain language

Use these exact terms. Do not invent synonyms.

| Term | Meaning |
|------|---------|
| **LogCaptor** | The public handle a test holds to read/clear captured entries |
| **LogEntry** | An immutable record of one intercepted log event |
| **LogCaptorStore** | The in-memory ring-buffer that holds LogEntry instances |
| **LogCaptorHandler** | The JUL `Handler` installed on the root logger to intercept records |
| **LogCaptorImpl** | The concrete `LogCaptor` that wires Handler + Store + level management |
| **LogCaptorExtension** | The JUnit 5 extension managing the full lifecycle |
| **capture scope** | The period between `install()` and `close()` |
| **self-test probe** | The `__log_assert_self_test__` entry emitted in `beforeAll` to verify the handler is active |
| **ring-buffer eviction** | Dropping the oldest entry when the store reaches `maxEntries` |
| **console handler** | Any `StreamHandler` / `ConsoleHandler` detached during capture and re-attached after |

---

## Build & test commands

```bash
# Compile and run all 90 tests
mvn test

# Full build including sources + javadoc jars (no deploy)
mvn package

# Publish to Maven Central (requires settings-central.xml with valid token)
mvn clean deploy -s settings-central.xml

# Format source code (Google Java Format)
mvn fmt:format

# Check formatting without modifying files
mvn fmt:check
```

Tests **must** pass before every commit. The expected result is always:
```
Tests run: 90, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Testing rules

- Every public method on `LogCaptor`, `LogsAssert`, and `LogEntryAssert` must have at least one test.
- Every error path (bad input, misconfiguration) must have a test asserting the exact exception type and message substring.
- Tests live in mirror packages under `src/test/java/`.
- Use `JUnit Platform Launcher` for tests that verify the extension's own lifecycle (see `LogCaptorExtensionTest`).
- Never add test-only methods or fields to production classes.
- Never mock `LogCaptorStore`, `LogCaptorHandler`, or `LogCaptorImpl` in unit tests — instantiate them directly.

---

## Code style

- **Google Java Format** via `fmt-maven-plugin`. Run `mvn fmt:format` before committing.
- Java 21 — use `switch` expressions, records, `var`, sealed interfaces where they improve clarity.
- No Lombok. No code generation beyond what the JDK provides.
- Prefer `List.copyOf()` over `Collections.unmodifiableList()`.
- Prefer `Optional` in return positions over `null` — except inside `LogCaptorStore` internals for performance.
- All `synchronized` methods must document in Javadoc why synchronization is needed.

---

## Git workflow

- Branch from `main`, prefix: `feat/`, `fix/`, `chore/`, `docs/`.
- Commit messages follow Conventional Commits: `type(scope): description`.
- Never force-push `main`.
- Never commit: `settings-central.xml`, `target/`, `*.class`, `.idea/`, `*.iml`.
- Run `mvn test` and `mvn fmt:check` before every commit.
- Tag releases: `git tag v1.0.0 && git push origin v1.0.0`.

---

## What agents must not do

- Do not add any dependency that is not `optional` or `test` scope without explicit approval.
- Do not change `LogCaptorStore` concurrency model without updating all three of `append`, `snapshot`, and `clear` together.
- Do not remove the self-test probe in `beforeAll` — it is the only guarantee the handler is active.
- Do not add `System.exit()`, `Runtime.halt()`, or `Thread.stop()`.
- Do not add any static mutable state outside of `LogCaptorStore` (which is already instance-scoped).
- Do not silently swallow exceptions — propagate as `LogCaptorConfigurationException` with a diagnostic message.
- Do not write to `System.out` in production code. `System.err` is allowed only for the two documented cases: concurrent-execution warning and `@PrintLogsOnFailure`.
