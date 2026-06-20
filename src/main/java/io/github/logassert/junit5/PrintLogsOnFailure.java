package io.github.logassert.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When present on a test class or method, dumps all captured log entries to {@code System.err}
 * whenever that test fails.
 *
 * <p>Applies to the annotated scope only:
 *
 * <ul>
 *   <li>On a class — all methods in the class print logs on failure.
 *   <li>On a method — only that method prints logs on failure.
 * </ul>
 *
 * <pre>{@code
 * @ExtendWith(LogCaptorExtension.class)
 * @PrintLogsOnFailure
 * class MyTest { ... }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PrintLogsOnFailure {}
