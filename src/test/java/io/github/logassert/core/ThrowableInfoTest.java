package io.github.logassert.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class ThrowableInfoTest {

  // ── from(null) ────────────────────────────────────────────────────────────

  @Test
  void from_null_returnsNull() {
    assertThat(ThrowableInfo.from(null)).isNull();
  }

  // ── from(exception) — basic capture ──────────────────────────────────────

  @Test
  void from_exception_capturesClassName() {
    RuntimeException ex = new RuntimeException("test message");
    ThrowableInfo info = ThrowableInfo.from(ex);

    assertThat(info).isNotNull();
    assertThat(info.className()).isEqualTo("java.lang.RuntimeException");
  }

  @Test
  void from_exception_capturesMessage() {
    RuntimeException ex = new RuntimeException("something went wrong");
    ThrowableInfo info = ThrowableInfo.from(ex);

    assertThat(info.message()).isEqualTo("something went wrong");
  }

  @Test
  void from_exception_capturesNullMessage() {
    RuntimeException ex = new RuntimeException((String) null);
    ThrowableInfo info = ThrowableInfo.from(ex);

    assertThat(info.message()).isNull();
  }

  @Test
  void from_exception_capturesNonEmptyStackFrames() {
    RuntimeException ex = new RuntimeException("test");
    ThrowableInfo info = ThrowableInfo.from(ex);

    assertThat(info.stackFrames()).isNotEmpty();
    // Each frame should be a StackTraceElement.toString() — contains '(' and ')'
    assertThat(info.stackFrames().get(0)).contains("(");
  }

  @Test
  void from_exception_stackFrames_areUnmodifiable() {
    ThrowableInfo info = ThrowableInfo.from(new RuntimeException("test"));

    assertThat(info.stackFrames()).isUnmodifiable();
  }

  @Test
  void from_exception_noCause_causeIsNull() {
    ThrowableInfo info = ThrowableInfo.from(new RuntimeException("no cause"));

    assertThat(info.cause()).isNull();
  }

  // ── from(exception with cause) — recursive capture ───────────────────────

  @Test
  void from_exceptionWithCause_capturesCauseChainRecursively() {
    IllegalArgumentException rootCause = new IllegalArgumentException("root cause");
    RuntimeException wrapper = new RuntimeException("wrapper", rootCause);

    ThrowableInfo info = ThrowableInfo.from(wrapper);

    assertThat(info.cause()).isNotNull();
    assertThat(info.cause().className()).isEqualTo("java.lang.IllegalArgumentException");
    assertThat(info.cause().message()).isEqualTo("root cause");
    assertThat(info.cause().cause()).isNull();
  }

  @Test
  void from_deepCauseChain_capturesMultipleLevels() {
    Throwable level3 = new RuntimeException("level3");
    Throwable level2 = new RuntimeException("level2", level3);
    Throwable level1 = new RuntimeException("level1", level2);

    ThrowableInfo info = ThrowableInfo.from(level1);

    assertThat(info.message()).isEqualTo("level1");
    assertThat(info.cause().message()).isEqualTo("level2");
    assertThat(info.cause().cause().message()).isEqualTo("level3");
    assertThat(info.cause().cause().cause()).isNull();
  }

  // ── isOfType ─────────────────────────────────────────────────────────────

  @Test
  void isOfType_matchesByExactClassName() {
    ThrowableInfo info = ThrowableInfo.from(new SocketTimeoutException("timed out"));

    assertThat(info.isOfType(SocketTimeoutException.class)).isTrue();
  }

  @Test
  void isOfType_doesNotMatchSuperclass() {
    ThrowableInfo info = ThrowableInfo.from(new SocketTimeoutException("timed out"));

    assertThat(info.isOfType(RuntimeException.class)).isFalse();
    assertThat(info.isOfType(Exception.class)).isFalse();
  }

  // ── simpleClassName ───────────────────────────────────────────────────────

  @Test
  void simpleClassName_returnsUnqualifiedName() {
    ThrowableInfo info = ThrowableInfo.from(new SocketTimeoutException("timed out"));

    assertThat(info.simpleClassName()).isEqualTo("SocketTimeoutException");
  }

  @Test
  void simpleClassName_forDefaultPackage_returnsFullName() {
    // Simulate a className with no dots by constructing directly
    ThrowableInfo info = new ThrowableInfo("NoPackageException", null, java.util.List.of(), null);

    assertThat(info.simpleClassName()).isEqualTo("NoPackageException");
  }

  // ── Circular cause chain protection ──────────────────────────────────────

  /**
   * Verifies that a cause chain longer than {@value ThrowableInfo#MAX_CAUSE_DEPTH} is truncated
   * rather than overflowing the stack. A true circular chain (A→B→A) is simulated via a custom
   * Throwable subclass that returns a fixed cause from {@code getCause()}.
   */
  @Test
  void circularCauseChain_doesNotStackOverflow() {
    // Build a circular chain: A.getCause() == B, B.getCause() == A
    CircularThrowable a = new CircularThrowable("A");
    CircularThrowable b = new CircularThrowable("B");
    a.setPartner(b);
    b.setPartner(a);

    // Must not throw StackOverflowError
    assertThatCode(() -> ThrowableInfo.from(a)).doesNotThrowAnyException();

    ThrowableInfo info = ThrowableInfo.from(a);
    assertThat(info).isNotNull();

    // Walk chain and verify depth is capped at MAX_CAUSE_DEPTH
    int depth = 0;
    ThrowableInfo node = info;
    while (node != null) {
      depth++;
      node = node.cause();
    }
    assertThat(depth).isLessThanOrEqualTo(ThrowableInfo.MAX_CAUSE_DEPTH);
  }

  @Test
  void deepCauseChain_exceedingMaxDepth_isTruncated() {
    // Chain of 25 exceptions — more than MAX_CAUSE_DEPTH (20)
    Throwable innermost = new RuntimeException("depth-25");
    Throwable current = innermost;
    for (int i = 24; i > 0; i--) {
      current = new RuntimeException("depth-" + i, current);
    }

    ThrowableInfo info = ThrowableInfo.from(current);
    assertThat(info).isNotNull();

    int depth = 0;
    ThrowableInfo node = info;
    while (node != null) {
      depth++;
      node = node.cause();
    }
    assertThat(depth).isLessThanOrEqualTo(ThrowableInfo.MAX_CAUSE_DEPTH);
  }

  // ── Helper — circular throwable ───────────────────────────────────────────

  /**
   * Throwable subclass that returns a caller-set partner from {@code getCause()} to form a loop.
   */
  private static class CircularThrowable extends Throwable {
    private Throwable partner;

    CircularThrowable(String message) {
      super(message, null, true, false); // suppress stack trace filling for speed
    }

    void setPartner(Throwable partner) {
      this.partner = partner;
    }

    @Override
    public synchronized Throwable getCause() {
      return partner;
    }
  }
}
