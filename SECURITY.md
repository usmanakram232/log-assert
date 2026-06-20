# Security Policy

## Supported versions

| Version | Supported |
|---------|-----------|
| 1.x     | ✅ Active |

## Scope

`log-assert` is a **test-scope library** — it is intended to run only inside test suites, not in production deployments. Its attack surface is limited by design:

- No network I/O of any kind
- No file-system writes
- No `Runtime.exec()` or `ProcessBuilder`
- No deserialization of untrusted data
- Compiled artifacts are GPG-signed and published via Sonatype Central Portal

Despite the test-only nature, supply-chain integrity matters. A compromised artifact in your test classpath could still exfiltrate secrets available at build time (environment variables, credentials in `~/.m2/settings.xml`, source code).

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Report privately by emailing: **usman.akram@ginmon.de**

Include:
- Description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept (do not exploit beyond demonstrating the issue)
- Affected versions
- Any suggested remediation

You will receive an acknowledgement within 48 hours. We aim to release a fix within 14 days for critical issues.

## Verifying artifact integrity

All releases are GPG-signed. To verify:

```bash
# Download the artifact and its .asc signature from Maven Central, then:
gpg --verify log-assert-1.0.0.jar.asc log-assert-1.0.0.jar
```

The signing key fingerprint is published on the [releases page](https://github.com/usmanakram232/log-assert/releases).
