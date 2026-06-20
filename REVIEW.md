# REVIEW.md — log-assert

Combined code review rulebook. Every PR and agent-generated change must pass all three review
lenses below before merging. Apply them in order: Veteran → Security → Adversarial.

---

## 1. Veteran Review

*Axis-driven, uncompromising. No hedging. No "good enough for now."*

### Review axes

| Axis | Directive | Reject / Flag |
|------|-----------|---------------|
| **Architecture** | Enforce single responsibility | Anti-patterns, SRP violations, tight coupling between `core/`, `jboss/`, and `junit5/` packages |
| **Logic** | Simplify | Nested conditionals > 2 levels, implicit state, boolean flag soup |
| **Syntax** | Clarify | Clever hacks, cryptic one-liners, unnecessary `var` where type is non-obvious |
| **Formatting** | Align | Any deviation from Google Java Format — run `mvn fmt:check` |
| **Resources** | Optimize | Unbounded allocations, handler leaks (installed but never removed), unclosed `AutoCloseable` |
| **Naming** | Standardize | Generic names (`data`, `obj`, `temp`, `flag`) — replace with domain terms from AGENTS.md |
| **Tests** | Demand | Missing coverage for any public method, untested error paths, tests that only test happy path |
| **Errors** | Handle strictly | Bare `catch (Exception e) {}`, swallowed `IllegalAccessException`, `RuntimeException` without message |

### Output format

For every defect:

```
[AXIS] <file>:<line>
Defect: <what is wrong and why>
Risk:   <what breaks or degrades if unfixed>
Fix:    <concrete corrected code>
```

End every review with: `N defects found. Blocking: X. Non-blocking: Y.`

### Project-specific veteran rules

**Architecture**
- `core/` must have **zero** dependencies on `jboss/` or `junit5/`. Dependency flows one way:
  `junit5/` → `jboss/` → `core/`. Violations break the adapter pattern.
- `LogCaptor` is a public API boundary. Any change to its method signatures is a breaking change
  and requires a major version bump.
- `LogCaptorStore` is an internal type. It must never appear in public API signatures.

**Logic**
- `LogCaptorStore`: `append`, `snapshot`, and `clear` must all be `synchronized`. If one is
  changed, review all three together. Partial synchronization is worse than none.
- `LogCaptorExtension.getCaptor()` walks the parent `ExtensionContext` chain — any shortcut
  that skips the walk will break nested test classes and `@RegisterExtension static`.
- Level override logic in `LogCaptorImpl.withMinLevel()` saves `previousLevel` only on the
  **first** call. Logic that re-saves on subsequent calls will silently corrupt restore behavior.

**Resources**
- Every `install()` must have a corresponding `uninstall()` in a `finally` block or
  `AutoCloseable.close()`. Handler leaks pollute subsequent tests in the same JVM.
- `EchoLogs` console handlers re-attached in `beforeEach` must always be removed in
  `afterEach`'s `finally` block — not in the `try` body.

**Tests**
- `LogCaptorExtensionTest` uses the JUnit Platform Launcher to run inner test classes.
  Do not replace these with simple unit tests — the lifecycle behavior can only be verified
  by actually running the extension through the JUnit engine.
- Any new annotation (`@InjectLogCaptor`, `@EchoLogs`, etc.) must have tests for:
  method-level annotation, class-level annotation, and annotation inheritance from superclass.

---

## 2. Security Review

*This is a library that executes inside users' CI pipelines and test suites.
A compromised or misbehaving library is a supply-chain attack vector.*

### Mandatory checks on every PR

#### 2.1 Network / process / filesystem
- [ ] No `java.net.*`, `HttpClient`, `URL.openConnection()`, `Socket`, `ServerSocket` in `src/main/`.
- [ ] No `Runtime.getRuntime().exec()` or `ProcessBuilder` anywhere.
- [ ] No `java.nio.file.Files.write*` or `FileOutputStream` in `src/main/`.
- [ ] No DNS resolution (`InetAddress.getByName()`) anywhere.

#### 2.2 Dangerous reflection
- [ ] `setAccessible(true)` is used only in `LogCaptorExtension.injectFields()` and for no other purpose.
- [ ] `InaccessibleObjectException` is caught and re-thrown as `LogCaptorConfigurationException`
  with a user-actionable message — never silently swallowed.
- [ ] No use of `MethodHandles.lookup().unreflect()` or `VarHandle` for mutable state access
  outside of existing approved patterns.

#### 2.3 Serialization & classloading
- [ ] No `ObjectInputStream`, `ObjectOutputStream`, or custom `readObject()` / `writeObject()`.
- [ ] No dynamic `ClassLoader` construction or `Class.forName()` with user-supplied strings.
- [ ] No use of `javax.script.*` (ScriptEngine, Nashorn).

#### 2.4 Secrets & credentials
- [ ] `git log --all --full-history -- settings-central.xml` returns zero results.
- [ ] `git grep -i "password\|secret\|token\|api.key\|credential"` returns zero results in
  `src/` and any tracked config file.
- [ ] No Base64-encoded or hex-encoded blobs in source that decode to credentials.

#### 2.5 Dependency audit
- [ ] All new dependencies are `optional` or `test` scope — no `compile` scope additions
  without explicit justification.
- [ ] No SNAPSHOT dependencies in `pom.xml` at release time.
- [ ] Run `mvn dependency:tree` and review every new transitive dependency.
- [ ] No dependency on `com.sun.*`, `sun.*`, or any internal JDK API.

#### 2.6 Build supply-chain
- [ ] All plugin versions are pinned (no `LATEST`, no `RELEASE`, no open ranges).
- [ ] `central-publishing-maven-plugin` version is the latest stable from
  `repo1.maven.org/maven2/org/sonatype/central/central-publishing-maven-plugin/maven-metadata.xml`.

#### 2.7 Concurrency safety
- [ ] `LogCaptorStore.append()`, `snapshot()`, and `clear()` are all `synchronized`.
- [ ] No new static mutable fields added to any class in `src/main/`.
- [ ] No `ThreadLocal` state in `src/main/` that could leak between tests.

### Severity levels

| Severity | Examples | Action |
|----------|---------|--------|
| **Critical** | Network call, credential in source, deserialization gadget | Block merge immediately |
| **High** | Unsynchronized shared state, uncaught `InaccessibleObjectException`, `System.exit()` | Block merge |
| **Medium** | Missing `finally` cleanup, new undeclared transitive dep, swallowed exception | Block merge unless waivered |
| **Low** | Non-pinned plugin version, missing Javadoc on internal method | Fix before release |
| **Info** | `System.err.println` (approved uses only), style deviation | Note in review |

### Finding format

```
[SEC-NNN] Severity: <Critical|High|Medium|Low|Info>
File:    <path>:<line>
Pattern: <CWE or OWASP category if applicable>
Finding: <what was found>
Impact:  <what an attacker or broken library could do>
Fix:     <exact remediation>
```

---

## 3. Adversarial Review

*Assume the code is wrong until proven otherwise. Challenge every assumption.
This is a pre-mortem, not a post-mortem.*

### The five adversarial questions

For every non-trivial change, answer all five before approving:

**Q1 — What breaks under concurrent execution?**
This library is used in test suites that may run with `@Execution(CONCURRENT)`.
Map every shared mutable object touched by the change and prove the locking is correct.
"It's unlikely to be used concurrently" is not an acceptable answer.

**Q2 — What does a Quarkus test environment do differently?**
Quarkus re-initialises the JBoss Log Manager after the extension class is loaded.
Any change that installs or uninstalls handlers must be verified to survive a log-manager reset.
The self-test probe in `beforeAll` must still pass.

**Q3 — What happens if the change is in a users' dependency tree for 5 years?**
Semantic versioning is a contract. Ask:
- Does this change the behaviour of any existing public API method?
- Does this add a new transitive dependency that could conflict with user projects?
- Does this narrow or widen any thrown exception type?

If yes to any: increment the version accordingly or don't merge.

**Q4 — Could this change be exploited as a supply-chain attack?**
Assume a malicious actor submitted this PR. Re-read it looking for:
- Subtle exfiltration hidden in logging or error messages.
- A time-bomb (`if (System.currentTimeMillis() > X)`) or env-var trigger.
- A dependency version bump to a compromised artifact.
- An obfuscated string, a large binary blob, or a Base64-encoded payload.

**Q5 — What is the failure mode when the worst case happens?**
For each new code path, describe the exact user-visible failure when it goes wrong.
Is the error message actionable? Does the library leave the JVM in a clean state
(handlers removed, levels restored, no leaked threads)?

### Adversarial checklist

- [ ] Every new `catch` block either re-throws or produces a log/exception with a diagnostic
  message. Empty catch blocks are always blocked.
- [ ] Every new public method has a documented contract for `null` inputs.
- [ ] No new method returns `null` where callers will not expect it — use `Optional` or throw.
- [ ] The change does not introduce any new static initialiser that could silently fail.
- [ ] If a new annotation is added, there is a test that uses it on a superclass and verifies
  inheritance behaviour explicitly.
- [ ] If a new exception type is added, it extends `RuntimeException` (not `Error`) and has
  both a `(String message)` and a `(String message, Throwable cause)` constructor.
- [ ] Version in `pom.xml` matches the intended release. `1.0.0-SNAPSHOT` must never be deployed
  to Maven Central.

---

## Review sign-off checklist

A PR is ready to merge when all of the following are true:

```
[ ] mvn test         → Tests run: 90, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS
[ ] mvn fmt:check    → BUILD SUCCESS (no formatting violations)
[ ] Veteran review   → 0 blocking defects
[ ] Security review  → 0 Critical, 0 High findings
[ ] Adversarial Q1–Q5 → all answered
[ ] git grep for secrets → 0 results
[ ] No new compile-scope non-optional dependency without justification
[ ] Javadoc present on all public methods added or modified
```
