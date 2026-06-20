# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- `atLevelAtLeast(Level)` filter on `LogsAssert` — keeps entries at or above a minimum severity
- `hasThrowableWithMessageContaining(String)` on `LogEntryAssert` — partial match on throwable message

### Fixed
- `hasSize(1)` now produces "1 log entry" (singular) instead of "1 log entries"
- `snapshot()` in `LogCaptorStore` is now `synchronized` (consistent with `append` and `clear`)
- `setAccessible(true)` in field injection now catches `InaccessibleObjectException` and throws
  `LogCaptorConfigurationException` with an actionable message including the required `opens` directive
- `getCaptor()` now throws `LogCaptorConfigurationException` instead of `IllegalStateException`

### Changed
- GPG signing and Central publishing moved to a `release` Maven profile — `mvn verify` now works
  without a GPG key configured
- `maven-compiler-plugin` switched from `<source>`/`<target>` to `<release>21</release>`
- Added `maven-enforcer-plugin` requiring Java 21+ and Maven 3.9+
- Added `project.build.outputTimestamp` for reproducible builds

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

[Unreleased]: https://github.com/usmanakram232/log-assert/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/usmanakram232/log-assert/releases/tag/v1.0.0
