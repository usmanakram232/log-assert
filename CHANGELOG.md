# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.2] — 2026-06-20

### Added
- `atLevelAtLeast(Level)` filter on `LogsAssert` — keeps entries at or above a minimum severity
  (e.g. `atLevelAtLeast(Level.WARN)` captures WARN and ERROR)
- `hasThrowableWithMessageContaining(String)` on `LogEntryAssert` — partial match on throwable
  message; complements the existing exact-match `hasThrowableWithMessage`
- `@since 1.0.0` Javadoc tags on every public type and method
- `ThrowableInfo.MAX_CAUSE_DEPTH` promoted to `public static final` — consumers can reference it
- `RELEASING.md` — step-by-step release checklist for maintainers
- 5 new tests (95 total): `@EchoLogs` lifecycle, annotation superclass inheritance
  (`@FailOnUncheckedError`, `@InjectLogCaptor`, `@PrintLogsOnFailure`), `withMinLevel` double-call invariant

### Fixed
- `hasSize(1)` failure message now reads "1 log entry" (singular) instead of "1 log entries"
- `snapshot()` in `LogCaptorStore` is now `synchronized`, consistent with `append` and `clear`
- `setAccessible(true)` in field injection now catches `InaccessibleObjectException` and throws
  `LogCaptorConfigurationException` with a message containing the exact `opens` directive needed
- `getCaptor()` now throws `LogCaptorConfigurationException` instead of bare `IllegalStateException`
- `@FailOnUncheckedError` and `@PrintLogsOnFailure` now carry `@Inherited` so annotations placed
  on a superclass are correctly detected by the extension in subclass tests (real bug)
- `@FailOnUncheckedError` Javadoc corrected: check fires only when the test itself passed;
  documents the `clearLogs()` pattern for asserting ERROR entries without triggering the check

### Changed
- `LogCaptorStore` internal storage changed from `ConcurrentLinkedDeque` + `AtomicInteger` to
  `ArrayDeque` + plain `int` — all access is already serialised by `synchronized`; no behaviour change
- `LogCaptorExtension` EchoLogs handler-level key changed from `System.identityHashCode(h)` to
  `consoleHandlers.indexOf(h)` — deterministic and collision-free
- GPG signing and Central Portal publishing moved to `-Prelease` Maven profile —
  `mvn verify` now works on any machine without a GPG key configured
- `maven-compiler-plugin` switched from `<source>`/`<target>` to `<release>21</release>`
- Added `maven-enforcer-plugin` requiring Java 21+ and Maven 3.9+
- Added `project.build.outputTimestamp` for reproducible builds
- Javadoc `<doclint>none</doclint>` → `<doclint>all,-missing</doclint>` — stricter validation
- `quarkus-junit5` version extracted to `${quarkus.junit5.version}` property

---

## [1.0.1] — 2026-06-20

### Added
- `atLevelAtLeast(Level)` filter, `hasThrowableWithMessageContaining(String)` *(backported to 1.0.2)*
- Project website via GitHub Pages (`docs/index.html`)
- CI workflow (`.github/workflows/ci.yml`) — runs tests + format check on every push and PR
- `LICENSE` (Apache 2.0), `CONTRIBUTING.md`, `CHANGELOG.md`, `SECURITY.md`
- SVG logo and social preview image
- `AGENTS.md`, `REVIEW.md` — agent and review guidelines

### Fixed
- `snapshot()` in `LogCaptorStore` synchronized
- `setAccessible` guarded with `InaccessibleObjectException`
- `getCaptor()` throws `LogCaptorConfigurationException`
- `hasSize(1)` singular grammar
- Javadoc `@link Pattern#find()` → `Matcher#find()`
- GPG plugin moved to release profile; `maven-enforcer-plugin` added
- assertj bumped 3.26.3 → 3.27.7 (CVE-2026-24400 XXE fix)

### Changed
- README: version `1.0.0-SNAPSHOT` → `1.0.0`, added badges, logo, compat matrix, why section
- `@FailOnUncheckedError` README doc corrected (fires only when test passed)

---

## [1.0.0] — 2026-06-20

Initial public release.

### Added
- `LogCaptor` interface and `LogCaptorImpl` — in-memory capture of JBoss Log Manager events
- `LogEntry` record — immutable snapshot: level, loggerName, formattedMessage, rawTemplate,
  throwable, mdcContext, timestamp, threadName, threadId, markerName
- `ThrowableInfo` record — serialized exception snapshot with cause chain (depth-limited to 20)
- `LogCaptorStore` — thread-safe ring-buffer (default 10,000 entries)
- `LogCaptorHandler` — JUL `ExtHandler` translating `LogRecord` → `LogEntry`
- `LogCaptorAssertions` — AssertJ entry point (`assertThat(LogCaptor)`, `assertThat(List<LogEntry>)`)
- `LogsAssert` — fluent filter + assertion DSL over a list of entries
  - Filters: `atLevel`, `fromLogger`, `containingMessage`, `withMdcEntry`
  - Terminal: `hasSize`, `isEmpty`, `isNotEmpty`
  - Navigation: `single`, `first`, `last`
- `LogEntryAssert` — single-entry assertion DSL
  - Message: `hasFormattedMessage`, `hasFormattedMessageContaining`, `hasFormattedMessageMatching`, `hasRawTemplate`
  - Level/logger: `hasLevel`, `hasLoggerName`
  - MDC: `hasMdcEntry`
  - Throwable: `hasThrowable`, `hasThrowableWithMessage`, `hasNoThrowable`
- `LogCaptorExtension` — JUnit 5 `BeforeAll/AfterAll/BeforeEach/AfterEach/TestWatcher/ParameterResolver`
- `@InjectLogCaptor` — field injection annotation
- `@EchoLogs` — re-attach console handlers for a test scope with optional `minimumLevel`
- `@FailOnUncheckedError` — fail if ERROR entries remain after test (when test itself passed)
- `@PrintLogsOnFailure` — dump captured logs to stderr on test failure
- `LogCaptorConfigurationException` — runtime exception for misconfiguration with diagnostic messages
- 90 unit and integration tests

[Unreleased]: https://github.com/usmanakram232/log-assert/compare/v1.0.2...HEAD
[1.0.2]: https://github.com/usmanakram232/log-assert/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/usmanakram232/log-assert/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/usmanakram232/log-assert/releases/tag/v1.0.0

