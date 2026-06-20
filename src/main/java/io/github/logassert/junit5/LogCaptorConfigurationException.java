package io.github.logassert.junit5;

/**
 * Thrown by {@link LogCaptorExtension} when the log capture infrastructure cannot be verified after
 * installation, or when a field injection fails due to module system access restrictions.
 *
 * <p>The most common cause is another component resetting the root logger's handler list after
 * {@link LogCaptorExtension} has installed its handler. In Quarkus environments this can happen
 * when the Quarkus test machinery re-initialises the log manager; use {@code
 * LogCaptorQuarkusResource} in those cases instead of {@code @RegisterExtension static}.
 *
 * <p>A second cause is Java module system encapsulation: if the test class lives in a named module
 * that does not {@code opens} its package to {@code io.github.logassert.junit5}, field injection
 * via {@link java.lang.reflect.Field#setAccessible} will throw {@link
 * java.lang.reflect.InaccessibleObjectException}. The exception message contains the exact
 * {@code opens} directive or {@code --add-opens} JVM flag needed to resolve it.
 */
public class LogCaptorConfigurationException extends RuntimeException {

  /**
   * Creates a new exception with the given diagnostic message.
   *
   * @param message human-readable description of the misconfiguration
   */
  public LogCaptorConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with a diagnostic message and an underlying cause.
   *
   * @param message human-readable description of the misconfiguration
   * @param cause the underlying exception (e.g. {@link java.lang.reflect.InaccessibleObjectException})
   */
  public LogCaptorConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
