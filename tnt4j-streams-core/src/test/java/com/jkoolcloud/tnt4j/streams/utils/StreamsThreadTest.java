/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.utils;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsThreadTest {

	@Test
	public void testStreamsThread() {
		StreamsThread thread = new StreamsThread();
		assertNotNull(thread);
	}

	@Test
	public void testStreamsThreadRunnable() {
		StreamsThread thread = new StreamsThread(simpleRunnable);
		assertNotNull(thread);
	}

	@Test
	public void testStreamsThreadRunnableString() {
		String name = "TEST-NAME"; // NON-NLS
		StreamsThread thread = new StreamsThread(simpleRunnable, name);
		assertNotNull(thread);
		assertThreadNameCorrect(name, thread);
	}

	private void assertThreadNameCorrect(String name, StreamsThread thread) {
		String actualName = thread.getName();
		int indexOfNameWithoutIdStart = String.valueOf(thread.getId()).length() + 1;
		assertEquals(name, actualName.substring(indexOfNameWithoutIdStart));
	}

	@Test
	public void testStreamsThreadString() {
		String name = "TEST-NAME"; // NON-NLS
		StreamsThread thread = new StreamsThread(name);
		assertNotNull(thread);
		assertThreadNameCorrect(name, thread);
	}

	@Test
	public void testStreamsThreadThreadGroupRunnable() {
		ThreadGroup tGroup = new ThreadGroup("TEST"); // NON-NLS
		StreamsThread thread = new StreamsThread(tGroup, simpleRunnable);
		StreamsThread thread2 = new StreamsThread(tGroup, simpleRunnable);
		thread.start();
		thread2.start();
		assertEquals(2, tGroup.activeCount());
	}

	@Test
	public void testStreamsThreadThreadGroupRunnableString() {
		String name = "TEST-NAME"; // NON-NLS
		String name2 = "TEST-NAME2"; // NON-NLS
		ThreadGroup tGroup = new ThreadGroup("TEST"); // NON-NLS
		StreamsThread thread = new StreamsThread(tGroup, simpleRunnable, name);
		StreamsThread thread2 = new StreamsThread(tGroup, simpleRunnable, name2);
		// assertEquals(2, tGroup.);
		assertThreadNameCorrect(name, thread);
		assertThreadNameCorrect(name2, thread2);
	}

	@Test
	public void testStreamsThreadThreadGroupString() {
		String name = "TEST-NAME"; // NON-NLS
		String name2 = "TEST-NAME2"; // NON-NLS
		ThreadGroup tGroup = new ThreadGroup("TEST"); // NON-NLS
		StreamsThread thread = new StreamsThread(tGroup, name);
		StreamsThread thread2 = new StreamsThread(tGroup, name2);
		// assertEquals(2, tGroup.);
		assertThreadNameCorrect(name, thread);
		assertThreadNameCorrect(name2, thread2);
	}

	@Test
	public void testIsStopRunning() {
		StreamsThread thread = new StreamsThread();
		thread.start();

		thread.halt(true);
		assertTrue(thread.isStopRunning());
	}

	@Test
	public void testSleep() {
		StreamsThread thread = new StreamsThread(new Runnable() {
			@Override
			public void run() {
				StreamsThread.sleep(500);
			}
		});
		thread.start();
		thread.interrupt();
	}

	@Test
	public void testSleepFully() {
		int sleepTime = 5000;
		long testStartTime = System.currentTimeMillis();

		StreamsThread thread = new StreamsThread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
				Thread.currentThread().interrupt();
			}
		});

		thread.start();

		thread.sleepFully(sleepTime);
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		thread.getState();
		assertTrue(System.currentTimeMillis() - testStartTime >= sleepTime);
	}

	@Test
	public void testWaitFor() {
		int sleepTime = 5000;
		long testStartTime = System.currentTimeMillis();

		StreamsThread thread = new StreamsThread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
			}
		});
		thread.start();
		thread.waitFor(sleepTime / 2);

		assertTrue(System.currentTimeMillis() - testStartTime >= sleepTime / 2);

	}

	final Runnable simpleRunnable = new Runnable() {
		@Override
		public void run() {
			for (int i = 0; i <= 500000; i++) {
			}
		}
	};
}
