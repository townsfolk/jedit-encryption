package mochi.util

import org.junit.Test

import java.util.concurrent.TimeUnit

/**
 * Created by elberry on 11/16/16.
 */
class ExpiringMapTests {

	@Test
	public void "Can be used with normal groovy map methods"() {
		ExpiringMap<String, String> data = [:]
		data["foo"] = "bar"
		assert data.foo == 'bar'
		data.each { k, v ->
			assert k == 'foo'
			assert v == 'bar'
		}

		data.clear()
		data.baz = 'boo'
		assert data['baz'] == 'boo'
		data.each { k, v ->
			assert k == 'baz'
			assert v == 'boo'
		}
	}

	@Test
	public void "Expired items can only be retrieved once more"() {
		ExpiringMap<String, String> data = [:]
		data.put("foo", "bar", 0, TimeUnit.MILLISECONDS)
		sleep(1)
		assert data.foo == 'bar' && data.foo == null
	}

	@Test
	public void "Listeners get notified when an item expires"() {
		ExpiringMap<String, String> data = [:]
		data.put("foo", "bar", 900, TimeUnit.MILLISECONDS)
		boolean called = false
		data.addListener({k ->
			assert data[k] == 'bar' && data[k] == null
			called = true
		})
		sleep(1001)
		assert called == true
	}
}
