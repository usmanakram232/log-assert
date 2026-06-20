package io.github.logassert.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.slf4j.event.Level;

/**
 * When present on a test class or method, re-enables console log output for that scope.
 *
 * <p>By default {@link LogCaptorExtension} detaches console/stream handlers from the root logger to
 * prevent test noise. Annotating with {@code @EchoLogs} signals that log output should still be
 * forwarded to the console for debugging purposes.
 *
 * <p>The {@link #minimumLevel()} attribute controls which levels are echoed (defaults to {@link
 * Level#TRACE}, i.e. everything).
 *
 * <pre>{@code
 * @ExtendWith(LogCaptorExtension.class)
 * @EchoLogs(minimumLevel = Level.DEBUG)
 * class MyTest { ... }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EchoLogs {
  /** Minimum level of log entries to echo to the console. Defaults to {@link Level#TRACE}. */
  Level minimumLevel() default Level.TRACE;
}
