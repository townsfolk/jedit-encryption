package mochi.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * This class is a {@link Map} decorator, with a default implementation. It adds value expiration durations to any Map.
 * <p>
 * Internally this class uses a {@link ScheduledExecutorService} which polls the values every second and expires them.
 * The expiry service, does not remove items from the map, but does mark them as being expired. Any expired items will
 * be removed after the next get is called, allowing the user to get the value one last time before it's gone. Note that
 * the {@link #get(Object)} method is the only method that removes items based on expiration. If it is never called
 * after the item has expired it will sit there forever.
 * <p>
 * As such, there is an {@link ExpirationListener} interface which you can implement to get notified when a value was
 * expired.
 *
 * @author Eric Berry
 */
public class ExpiringMap<K, V> implements Map<K, V> {

	/**
	 * Allows you to listen for when items become expired.
	 *
	 * @param <K> The type of the Key.
	 */
	public interface ExpirationListener<K> {
		/**
		 * Notified when the given key has expired.
		 *
		 * @param key The expired key.
		 */
		void itemExpired(K key);
	}

	private class ExpiringEntry {
		private final long durationMillis;
		private boolean expired;
		private long init;

		public ExpiringEntry(long duration, TimeUnit timeUnit) {
			durationMillis = timeUnit.toMillis(duration);
			init = System.currentTimeMillis();
		}

		/**
		 * Checks the current entry's duration against the current time. If the time since it was initialized
		 * (or was last touched) is greater than the current time, the expired flag will be set to true. Use
		 * {@link #check(long)} to avoid multiple calls to {@link System#currentTimeMillis()}.
		 *
		 * @return True if the duration since this entry was initialized or last touched is greater than the current time.
		 */
		private boolean check() {
			return check(System.currentTimeMillis());
		}

		/**
		 * Checks the current entry's duration against the given current time. If the time since it was initialized
		 * (or was last touched) is greater than the given current time, the expired flag will be set to true.
		 *
		 * @param currentTime
		 * @return True if the duration since this entry was initialized or last touched is greater than the given time.
		 */
		private boolean check(long currentTime) {
			if (currentTime > init + durationMillis) {
				expired = true;
			}
			return expired;
		}

		/**
		 * Resets the initialization time to the current time. Can be used if a "web-session" like behavior is required
		 * for expiration - meaning, the value can stay in memory longer if it's used often.
		 */
		private void touch() {
			init = System.currentTimeMillis();
		}
	}

	private final long defaultDuration;
	private final TimeUnit defaultTimeUnit;
	private final Map<K, V> delegate;
	private final Map<K, ExpiringEntry> expiringEntries;
	private final ScheduledExecutorService expiryService;
	private final Set<ExpirationListener<K>> listeners;

	/**
	 * Creates a new ExpiringMap with a default expiration duration of 30 minutes. The underlying map is a
	 * {@link ConcurrentHashMap}.
	 */
	public ExpiringMap() {
		this(new ConcurrentHashMap<>(), 30, TimeUnit.MINUTES);
	}

	/**
	 * Creates a new ExpiringMap with a default expiration duration of the given time. The underlying map is a
	 * {@link ConcurrentHashMap}.
	 *
	 * @param defaultDuration The default duration
	 * @param defaultTimeUnit The default {@link TimeUnit}
	 */
	public ExpiringMap(long defaultDuration, TimeUnit defaultTimeUnit) {
		this(new ConcurrentHashMap<>(), defaultDuration, defaultTimeUnit);
	}

	/**
	 * Creates a new ExpiringMap that decorates the given delegate map with the given default duration.
	 *
	 * @param delegate
	 * @param defaultDuration
	 * @param defaultTimeUnit
	 */
	public ExpiringMap(Map<K, V> delegate, long defaultDuration, TimeUnit defaultTimeUnit) {
		this.delegate = delegate;
		this.defaultDuration = defaultDuration;
		this.defaultTimeUnit = defaultTimeUnit;
		expiringEntries = new ConcurrentHashMap<>();
		expiryService = new ScheduledThreadPoolExecutor(1);
		expiryService.schedule(this::expireValues, 1, TimeUnit.SECONDS);
		listeners = new CopyOnWriteArraySet<>();
	}

	public void addListener(ExpirationListener<K> listener) {
		listeners.add(listener);
	}

	@Override
	public void clear() {
		expiringEntries.clear();
		delegate.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public V get(Object key) {
		if (expiringEntries.containsKey(key)) {
			V value = delegate.get(key);
			ExpiringEntry expiringEntry = expiringEntries.get(key);
			if (expiringEntry.expired || expiringEntry.check()) {
				delegate.remove(key);
				expiringEntries.remove(key);
			}
			return value;
		}
		return null;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public V put(K key, V value) {
		return put(key, value, defaultDuration, defaultTimeUnit);
	}

	/**
	 * Does the same as {@link Map#put(Object, Object)}, but associates the non-default given duration with the value.
	 *
	 * @param key
	 * @param value
	 * @param duration
	 * @param timeUnit
	 * @return The previous value associated with the given key if it existed, otherwise null.
	 * @see Map#put(Object, Object)
	 */
	public V put(K key, V value, long duration, TimeUnit timeUnit) {
		expiringEntries.put(key, newEntry(duration, timeUnit));
		return delegate.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.forEach((k, v) -> expiringEntries.put(k, newEntry()));
		delegate.putAll(m);
	}

	/**
	 * Does the same as {@link Map#putAll(Map)}, but associates the non-default given duration with all the values.
	 *
	 * @param m
	 * @param duration
	 * @param timeUnit
	 * @see Map#putAll(Map)
	 */
	public void putAll(Map<? extends K, ? extends V> m, final long duration, final TimeUnit timeUnit) {
		m.forEach((k, v) -> expiringEntries.put(k, newEntry(duration, timeUnit)));
		delegate.putAll(m);
	}

	@Override
	public V remove(Object key) {
		expiringEntries.remove(key);
		return delegate.remove(key);
	}

	public boolean removeListener(ExpirationListener<K> listener) {
		return listeners.remove(listener);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public Collection<V> values() {
		return delegate.values();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	private void expireValues() {
		final long currentTime = System.currentTimeMillis();
		expiringEntries.forEach((k, v) -> {
			if (v.check(currentTime)) {
				listeners.forEach((l) -> l.itemExpired(k));
			}
		});
	}

	private ExpiringEntry newEntry(long duration, TimeUnit timeUnit) {
		return new ExpiringEntry(duration, timeUnit);
	}

	private ExpiringEntry newEntry() {
		return newEntry(defaultDuration, defaultTimeUnit);
	}
}