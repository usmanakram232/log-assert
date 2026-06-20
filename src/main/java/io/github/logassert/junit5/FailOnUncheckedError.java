package io.github.logassert.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When present on a test class or method, fails the test after all assertions if any {@code ERROR}
 * level log entries remain unchecked (i.e. still present in the captor at {@code afterEach} time).
 *
 * <p>This is useful to detect unexpected error logging that tests might otherwise silently ignore.
 * If you intentionally log at ERROR and assert on it, the assertion itself consumes the entry; the
 * check only trips if un-asserted ERROR entries remain.
 *
 * <p><strong>Note:</strong> the check fires in {@code afterEach}, <em>after</em> the test body has
 * run. If the test body already throws an assertion error, this annotation's failure is reported
 * separately.
 *
 * <pre>{@code
 * @ExtendWith(LogCaptorExtension.class)
 * @FailOnUncheckedError
 * class MyTest { ... }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FailOnUncheckedError {}
