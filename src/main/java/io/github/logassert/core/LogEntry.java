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
 *
 * <p><strong>Note on {@code formattedMessage} vs {@code rawTemplate}</strong>: when using the SLF4J
 * → JBoss bridge ({@code slf4j-jboss-logmanager}), both fields will contain the fully resolved
 * string because the raw SLF4J template is not preserved through that bridge. Use {@link
 * #formattedMessage()} for assertions in all cases; only rely on {@link #rawTemplate()} when you
 * are using an SLF4J implementation that does preserve the template (e.g. Logback).
 *
 * @since 1.0.0
 */
public record LogEntry(
    /** Wall-clock time of the log event. */
    Instant timestamp,
    /** SLF4J event level (TRACE, DEBUG, INFO, WARN, ERROR). */
    Level level,
    /** Logger name — typically the fully-qualified class name. */
    String loggerName,
    /**
     * The fully resolved log message. SLF4J {@code {}} placeholders have been substituted.
     *
     * <p>Example: {@code "User alice logged in"}.
     *
     * <p><strong>Bridge note:</strong> when using the SLF4J → JBoss bridge ({@code
     * slf4j-jboss-logmanager}), both {@code formattedMessage} and {@code rawTemplate} will contain
     * the resolved string; the raw SLF4J template is not available through this bridge.
     */
    String formattedMessage,
    /**
     * The original log template passed to SLF4J before argument substitution.
     *
     * <p>Example: {@code "User {} logged in"}.
     *
     * <p>May equal {@link #formattedMessage()} depending on the SLF4J bridge in use. When using
     * {@code slf4j-jboss-logmanager}, both fields will contain the fully resolved string because
     * the JBoss bridge does not preserve the raw template. Use {@link #formattedMessage()} for
     * assertions in all environments.
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
