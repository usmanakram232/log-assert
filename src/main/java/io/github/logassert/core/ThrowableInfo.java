package io.github.logassert.core;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable serialized snapshot of a {@link Throwable} captured at log-event time.
 *
 * <p>Never holds a reference to the live {@code Throwable}. The entire cause chain is serialized
 * recursively up to a maximum depth of {@value #MAX_CAUSE_DEPTH} levels, protecting against
 * circular cause chains.
 *
 * @since 1.0.0
 */
public record ThrowableInfo(
    /** Fully-qualified class name, e.g. {@code "java.net.SocketTimeoutException"}. */
    String className,
    /** Exception message — may be {@code null}. */
    String message,
    /** Stack frames formatted by {@link StackTraceElement#toString()}; immutable. */
    List<String> stackFrames,
    /** Serialized cause — {@code null} if the exception had no cause or depth limit reached. */
    ThrowableInfo cause) {

  /**
   * Maximum cause-chain depth to capture before truncating (prevents circular-chain overflow).
   *
   * @since 1.0.0
   */
  public static final int MAX_CAUSE_DEPTH = 20;

  /** Canonical constructor — makes {@code stackFrames} unmodifiable. */
  public ThrowableInfo {
    stackFrames = List.copyOf(stackFrames);
  }

  /**
   * Serializes {@code t} into a {@code ThrowableInfo}.
   *
   * @param t the throwable to capture; may be {@code null}
   * @return a snapshot of {@code t}, or {@code null} if {@code t} is {@code null}
   * @since 1.0.0
   */
  public static ThrowableInfo from(Throwable t) {
    return from(t, 0);
  }

  private static ThrowableInfo from(Throwable t, int depth) {
    if (t == null) {
      return null;
    }
    if (depth >= MAX_CAUSE_DEPTH) {
      return null;
    }
    String className = t.getClass().getName();
    String message = t.getMessage();
    List<String> stackFrames =
        Arrays.stream(t.getStackTrace()).map(StackTraceElement::toString).toList();
    ThrowableInfo cause = from(t.getCause(), depth + 1);
    return new ThrowableInfo(className, message, stackFrames, cause);
  }

  /**
   * Returns {@code true} if this exception's class name equals {@code type.getName()}.
   *
   * @param type the class to check against
   * @since 1.0.0
   */
  public boolean isOfType(Class<? extends Throwable> type) {
    return className.equals(type.getName());
  }

  /**
   * Returns the simple (unqualified) class name.
   *
   * <p>For example, {@code "java.net.SocketTimeoutException"} → {@code "SocketTimeoutException"}.
   *
   * @since 1.0.0
   */
  public String simpleClassName() {
    int lastDot = className.lastIndexOf('.');
    return lastDot >= 0 ? className.substring(lastDot + 1) : className;
  }
}
