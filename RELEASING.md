# Releasing log-assert

Step-by-step checklist for cutting a release. Run each step in order.

---

## Pre-release checklist

- [ ] All tests pass: `mvn test` → `BUILD SUCCESS`
- [ ] Formatting clean: `mvn fmt:check` → `BUILD SUCCESS`
- [ ] No SNAPSHOT dependencies: `mvn dependency:tree | grep SNAPSHOT` → empty
- [ ] `CHANGELOG.md` updated with the new version section
- [ ] `README.md` dependency snippet shows the new version
- [ ] `docs/index.html` version pill updated
- [ ] `settings-central.xml` has a **fresh, valid** Central Portal token (rotate before each release)

---

## Bump the version

```bash
# Edit pom.xml: change <version>X.Y.Z-old</version> to <version>X.Y.Z</version>
# Then verify:
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

---

## Build and verify locally

```bash
# Full build with sources + javadoc (no signing, no upload)
mvn clean package

# Verify javadoc has no warnings
mvn javadoc:jar 2>&1 | grep -i warning
```

---

## Deploy to Maven Central

```bash
# Sign + upload to Central Portal staging (requires GPG key + settings-central.xml)
mvn clean deploy -s settings-central.xml -Prelease
```

Then go to **[central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)**:
1. Find the new deployment (it will show as "Validated")
2. Click **Publish**
3. Wait ~10–30 minutes for Maven Central propagation

---

## Tag the release

```bash
# Tag the exact commit that was deployed
git tag vX.Y.Z
git push origin vX.Y.Z
```

Convention:
- `v1.0.0` → initial release
- `v1.0.1` → patch (bug fixes, no API changes)
- `v1.1.0` → minor (new backwards-compatible API additions)
- `v2.0.0` → major (breaking API changes)

---

## Post-release

- [ ] Create a GitHub Release at `github.com/usmanakram232/log-assert/releases/new`
  - Tag: `vX.Y.Z`
  - Title: `log-assert X.Y.Z`
  - Body: paste the relevant `CHANGELOG.md` section
- [ ] Bump `pom.xml` to the next `-SNAPSHOT` version on `main`
- [ ] Announce (if major/minor): Reddit r/java, Quarkus Discord, mastodon.social #java

---

## GPG key management

The signing key is **not** stored in this repository. To set up on a new machine:

```bash
# List available keys
gpg --list-secret-keys --keyid-format=long

# If no key — generate one (RSA 4096, no expiry or 2y)
gpg --full-generate-key

# Upload public key to keyserver (Central Portal checks this)
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

The GPG passphrase is entered interactively during `mvn deploy -Prelease`.
