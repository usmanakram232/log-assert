# Releasing log-assert

This project uses `maven-release-plugin` to automate version bumping, tagging, and deployment.
The full release is a two-command flow.

---

## Prerequisites

| Requirement | Check |
|---|---|
| GPG key configured | `gpg --list-secret-keys` |
| GPG key on keyserver | `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>` |
| `settings-central.xml` present with a valid Central Portal token | `cat settings-central.xml` |
| On `main` branch, working tree clean | `git status` |

---

## The release flow

### Step 1 — Prepare

`release:prepare` does the following automatically:

1. Checks for SNAPSHOT dependencies (via `maven-enforcer`)
2. Runs `clean verify fmt:check` to confirm the build is green
3. Strips `-SNAPSHOT` from the version (e.g. `1.0.3-SNAPSHOT` → `1.0.3`)
4. Commits `chore: release v1.0.3`
5. Creates git tag `v1.0.3`
6. Bumps to next development version (`1.0.4-SNAPSHOT`)
7. Commits `chore: prepare for next development iteration`
8. Pushes both commits + the tag to `origin`

```bash
mvn release:prepare -s settings-central.xml
```

The plugin will prompt for:
- **Release version** (default: strips `-SNAPSHOT` — accept with Enter)
- **SCM tag** (default: `v1.0.3` — accept with Enter)
- **Next development version** (default: `1.0.4-SNAPSHOT` — accept with Enter)

To run non-interactively (CI-friendly):

```bash
mvn release:prepare -s settings-central.xml -B \
  -DreleaseVersion=1.0.3 \
  -DdevelopmentVersion=1.0.4-SNAPSHOT \
  -Dtag=v1.0.3
```

---

### Step 2 — Perform

`release:perform` does the following automatically:

1. Checks out the tagged commit into `target/checkout/`
2. Runs `mvn deploy -Prelease` on the checkout
3. Signs all artifacts with GPG
4. Uploads the bundle to Sonatype Central Portal

```bash
mvn release:perform -s settings-central.xml
```

Then go to **[central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)**:
1. Find the deployment (status: "Validated")
2. Click **Publish**
3. Wait ~10–30 min for Maven Central propagation

---

### One-shot (prepare + perform together)

```bash
mvn release:prepare release:perform -s settings-central.xml
```

---

## Post-release

After Maven Central publishes the artifact:

```bash
# Create the GitHub Release (the tag was already pushed by release:prepare)
gh release create v1.0.3 \
  --title "log-assert 1.0.3" \
  --notes "$(sed -n '/## \[1\.0\.3\]/,/## \[1\.0\./{ /## \[1\.0\./!p }' CHANGELOG.md)" \
  --latest
```

Or create it manually at [github.com/usmanakram232/log-assert/releases/new](https://github.com/usmanakram232/log-assert/releases/new).

---

## If something goes wrong

### Rollback a failed prepare

```bash
mvn release:rollback -s settings-central.xml
# Then delete the tag if it was already pushed:
git tag -d v1.0.3
git push origin :refs/tags/v1.0.3
```

### Clean up stale release files

```bash
mvn release:clean
# Removes release.properties and backup POMs
```

### Start over

```bash
mvn release:rollback -s settings-central.xml
mvn release:clean
```

---

## Versioning policy

This project follows [Semantic Versioning](https://semver.org/):

| Change | Version bump | Example |
|---|---|---|
| Bug fixes, internal refactoring (no API change) | Patch | `1.0.2` → `1.0.3` |
| New backwards-compatible API methods | Minor | `1.0.3` → `1.1.0` |
| Breaking API changes | Major | `1.x.x` → `2.0.0` |

---

## GPG key management

```bash
# List keys
gpg --list-secret-keys --keyid-format=long

# Generate a new key (RSA 4096, no expiry or 2y)
gpg --full-generate-key

# Upload public key to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Export for backup
gpg --export-secret-keys YOUR_KEY_ID > private-key.asc
```

The GPG passphrase is entered interactively during `mvn release:perform`.
