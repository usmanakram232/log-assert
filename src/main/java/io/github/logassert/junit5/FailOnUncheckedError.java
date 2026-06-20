package io.github.logassert.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When present on a test class or method, fails the test after all assertions if any {@code ERROR}
 * level log entries remain unchecked (i.e. still present in the captor at {@code afterEach} time).
 *
 * <p>This is useful to detect unexpected error logging that tests might otherwise silently ignore.
 * If you intentionally log at ERROR and assert on it, call {@code logCaptor.clearLogs()} after your
 * assertions to prevent a spurious failure here.
 *
 * <p><strong>Note:</strong> the check fires in {@code afterEach} <em>only when the test body
 * passed</em>. If the test body already threw an exception or {@code AssertionError}, this check is
 * suppressed to avoid masking the original failure.
 *
 * <p>This annotation is {@link java.lang.annotation.Inherited} — placing it on a superclass applies
 * it to all subclass test methods.
 *
 * <pre>{@code
 * @ExtendWith(LogCaptorExtension.class)
 * @FailOnUncheckedError
 * class MyTest { ... }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FailOnUncheckedError {}
