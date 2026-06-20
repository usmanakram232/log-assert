package io.github.logassert.jboss;

import io.github.logassert.core.LogEntry;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory storage for captured {@link LogEntry} instances.
 *
 * <p>Implements ring-buffer semantics: when the store is at capacity, the oldest entry is evicted
 * before the new entry is added. All public methods are safe to call from multiple threads
 * concurrently.
 *
 * <p>Used internally by {@link LogCaptorHandler} (appends) and {@link LogCaptorImpl} (reads /
 * clears).
 */
public final class LogCaptorStore {

  private static final int DEFAULT_MAX_ENTRIES = 10_000;

  private final ConcurrentLinkedDeque<LogEntry> entries = new ConcurrentLinkedDeque<>();
  private final AtomicInteger size = new AtomicInteger(0);
  private final int maxEntries;

  /** Creates a store with the default capacity of {@value #DEFAULT_MAX_ENTRIES} entries. */
  public LogCaptorStore() {
    this(DEFAULT_MAX_ENTRIES);
  }

  /**
   * Creates a store with the given maximum capacity.
   *
   * @param maxEntries maximum number of entries to retain; once reached, oldest entries are evicted
   */
  public LogCaptorStore(int maxEntries) {
    this.maxEntries = maxEntries;
  }

  /**
   * Appends {@code entry} to the store.
   *
   * <p>If the store is already at capacity, the oldest entry is removed (ring-buffer eviction)
   * before the new entry is added, keeping the size bounded.
   *
   * @param entry the log entry to store; must not be {@code null}
   */
  public synchronized void append(LogEntry entry) {
    entries.addLast(entry);
    if (size.incrementAndGet() > maxEntries) {
      entries.pollFirst();
      size.decrementAndGet();
    }
  }

  /**
   * Returns an unmodifiable point-in-time snapshot of all stored entries, oldest first.
   *
   * <p>Synchronized against {@link #append} and {@link #clear} to ensure the returned list
   * reflects a consistent state and never contains entries that were concurrently evicted by a
   * {@code clear()} call in progress.
   *
   * @return immutable copy of current entries; never {@code null}
   */
  public synchronized List<LogEntry> snapshot() {
    return List.copyOf(entries);
  }

  /** Removes all stored entries and resets the size counter to zero. */
  public synchronized void clear() {
    entries.clear();
    size.set(0);
  }

  /** Returns the current number of stored entries. */
  public int size() {
    return size.get();
  }

  /** Returns {@code true} if no entries are stored. */
  public boolean isEmpty() {
    return size.get() == 0;
  }
}
