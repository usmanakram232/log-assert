package io.github.logassert.junit5;

/**
 * Thrown by {@link LogCaptorExtension} when the log capture infrastructure cannot be verified
 * after installation.
 *
 * <p>The most common cause is another component resetting the root logger's handler list after
 * {@link LogCaptorExtension} has installed its handler. In Quarkus environments this can happen
 * when the Quarkus test machinery re-initialises the log manager; use {@code
 * LogCaptorQuarkusResource} in those cases instead of {@code @RegisterExtension static}.
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
}
