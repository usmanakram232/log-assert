# Contributing to log-assert

Thank you for your interest in contributing! This document covers everything you need to get started.

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Java | 21 |
| Maven | 3.9 |

No other tools required. GPG is only needed for publishing releases (maintainers only).

---

## Build & test

```bash
# Clone
git clone https://github.com/usmanakram232/log-assert.git
cd log-assert

# Compile and run all tests
mvn test

# Full build (includes sources + javadoc jars)
mvn package

# Auto-format source code (Google Java Format)
mvn fmt:format

# Check formatting without modifying files
mvn fmt:check
```

Tests must pass before every commit:
```
Tests run: 90, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Code style

This project uses **Google Java Format** enforced by `fmt-maven-plugin`.

- Run `mvn fmt:format` before committing — CI will fail on format violations.
- Java 21: use `switch` expressions, records, `var` where they improve clarity.
- No Lombok. No code generation.
- `Optional` in return positions instead of `null` (except `LogCaptorStore` internals).
- Every public method must have a Javadoc comment.

---

## Branch & commit conventions

- Branch from `main` with a prefix: `feat/`, `fix/`, `chore/`, `docs/`
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
  ```
  fix(assertj): correct hasSize singular grammar
  feat(assertj): add atLevelAtLeast filter method
  chore: bump assertj 3.26.3 → 3.27.7
  ```

---

## Pull request checklist

Before opening a PR, verify:

- [ ] `mvn test` → `BUILD SUCCESS`, 90 tests passing
- [ ] `mvn fmt:check` → `BUILD SUCCESS`
- [ ] New public methods have Javadoc
- [ ] New behaviour has corresponding tests (both happy path and failure path with exact message assertions)
- [ ] No new `compile`-scope non-optional dependencies without discussion

---

## What to work on

See [open issues](https://github.com/usmanakram232/log-assert/issues) — issues labelled `good first issue` are a good starting point.

Some known enhancements tracked for future versions:
- `@EchoLogs` integration test coverage
- Annotation inheritance tests for all four annotations
- `module-info.java` for JPMS support
- Logback adapter (`logback-classic` backend)

---

## Project structure

```
src/main/java/io/github/logassert/
├── core/          Pure-Java API — no framework dependencies
├── jboss/         JBoss Log Manager adapter
└── junit5/        JUnit 5 extension + annotations

src/test/java/     Mirror packages — one test class per production class
```

See `AGENTS.md` for the full package contract and domain language glossary.

---

## Reporting bugs

Please open a [GitHub issue](https://github.com/usmanakram232/log-assert/issues/new) with:
1. Minimal reproducible test case
2. Expected vs actual behaviour
3. Java version (`java -version`), JBoss Log Manager version, Quarkus version (if applicable)
