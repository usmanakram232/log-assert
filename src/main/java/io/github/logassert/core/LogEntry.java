package io.github.logassert.core;

import java.time.Instant;
import java.util.Map;
import org.slf4j.event.Level;

/**
 * Immutable snapshot of a single log event, captured at the moment the event was emitted.
 *
 * <p>All fields reflect the state at capture time; no mutable references are retained.
 *
 * <p><strong>Note on {@code Level}</strong>: uses {@code org.slf4j.event.Level} — never a
 * JBoss/Logback Level type — so the assertion API stays framework-agnostic.
 */
public record LogEntry(
    /** Wall-clock time of the log event. */
    Instant timestamp,
    /** SLF4J event level (TRACE, DEBUG, INFO, WARN, ERROR). */
    Level level,
    /** Logger name — typically the fully-qualified class name. */
    String loggerName,
    /**
     * The fully formatted message with all SLF4J {@code {}} placeholders already resolved (e.g.
     * {@code "User 42 logged in"}).
     */
    String formattedMessage,
    /**
     * The original SLF4J message template before argument substitution (e.g. {@code "User {} logged
     * in"}).
     */
    String rawTemplate,
    /**
     * Serialized exception snapshot, or {@code null} if no throwable was passed to the logging
     * call.
     */
    ThrowableInfo throwable,
    /**
     * Defensive copy of the MDC context at the time of the log event; unmodifiable and never {@code
     * null}.
     */
    Map<String, String> mdcContext,
    /** Name of the thread that produced this log event. */
    String threadName,
    /** ID of the thread that produced this log event. */
    long threadId,
    /**
     * SLF4J marker name, or {@code null}. Placeholder field for v1 — always {@code null} until a
     * later phase adds marker support.
     */
    String markerName) {

  /**
   * Canonical constructor — defensively copies {@code mdcContext} so that mutations to the caller's
   * map after construction do not affect this record.
   *
   * @throws NullPointerException if {@code mdcContext} is {@code null} or contains {@code null}
   *     keys/values (callers must pass an empty map, never {@code null})
   */
  public LogEntry {
    mdcContext = Map.copyOf(mdcContext);
  }
}
