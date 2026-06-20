package io.github.logassert.assertj;

import io.github.logassert.core.LogCaptor;
import io.github.logassert.core.LogEntry;
import java.util.List;

/**
 * Static entry point for log-assert AssertJ assertions.
 *
 * <p>Named {@code LogCaptorAssertions} (not {@code Assertions}) to avoid conflicts with AssertJ's
 * own {@code org.assertj.core.api.Assertions} when both are statically imported.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * import static io.github.logassert.assertj.LogCaptorAssertions.assertThat;
 *
 * assertThat(captor)
 *     .atLevel(Level.ERROR)
 *     .single()
 *     .hasFormattedMessageContaining("payment failed");
 * }</pre>
 */
public final class LogCaptorAssertions {

  private LogCaptorAssertions() {}

  /**
   * Creates a {@link LogsAssert} over a snapshot of the entries currently held by {@code captor}.
   *
   * <p>The snapshot is taken at call time; entries logged after this call are not included.
   *
   * @param captor the log captor to assert against
   * @return a new {@code LogsAssert}
   */
  public static LogsAssert assertThat(LogCaptor captor) {
    return new LogsAssert(captor.getLogs());
  }

  /**
   * Creates a {@link LogsAssert} directly over the provided list of entries.
   *
   * <p>Useful when you have already retrieved entries via {@link LogCaptor#getLogs()} and want to
   * perform multiple independent assertion chains over the same snapshot.
   *
   * @param entries the entries to assert against
   * @return a new {@code LogsAssert}
   */
  public static LogsAssert assertThat(List<LogEntry> entries) {
    return new LogsAssert(entries);
  }
}
