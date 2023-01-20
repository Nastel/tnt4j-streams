/*
 * Copyright 2014-2023 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0
 */
public class AbstractBufferedStreamTest {
	public boolean inputShouldEnd = false;
	AbstractBufferedStream<String> abs = new AbstractBufferedStreamTestStub();

	@Test
	public void getNextItemTest() throws Exception {
		abs.startStream();
		abs.setOwnerThread(mock(StreamThread.class));
		abs.addInputToBuffer("TEST"); // NON-NLS
		assertEquals("TEST", abs.getNextItem());
	}

	@Test(expected = IllegalStateException.class)
	public void getNextItemFailOnNullTest() throws Exception {
		abs.addInputToBuffer(null);
		assertNull(abs.getNextItem());
	}

	@Test
	public void getNextItemNullOnEmptyTest() throws Exception {
		abs.startStream();
		inputShouldEnd = true;
		assertNull(abs.getNextItem());
	}

	@Test(timeout = 4000)
	public void getNextItemExpectedToWaitTest() throws Exception {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					abs.startStream();
					abs.addInputToBuffer("TEST"); // NON-NLS
					// abs.setProperties(ActivityParserTestBase.makeProperty(StreamProperties.PROP_HALT_ON_PARSER,
					// "false"));
					assertNotNull(abs.getNextItem());
					assertNull(abs.getNextItem());
				} catch (Exception e) {
				}
			}
		});
		thread.start();
		TimeUnit.SECONDS.sleep(1);
		assertTrue(thread.getState() == Thread.State.WAITING || thread.getState() == Thread.State.TIMED_WAITING);
		thread.interrupt();
	}

	private class AbstractBufferedStreamTestStub extends AbstractBufferedStream<String> {
		private EventSink es = mock(EventSink.class);

		AbstractBufferedStreamTestStub() {
			super();
		}

		@Override
		protected EventSink logger() {
			return es;
		}

		@Override
		protected boolean isInputEnded() {
			return inputShouldEnd;
		}

		@Override
		protected long getActivityItemByteSize(String item) {
			return item == null ? 0 : item.getBytes().length;
		}

		@Override
		public boolean isHalted() {
			return false;
		}

	}

	private int overflowRecordCount = 1024 * 10 + 1;

	@Test(timeout = 5000)
	public void addInputToBufferOverflowTest() throws Exception {
		abs.startStream();

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (; overflowRecordCount >= 0; overflowRecordCount--) {
					abs.addInputToBuffer("T"); // NON-NLS
					System.out.println(overflowRecordCount);
				}
			}
		});
		thread.start();
		TimeUnit.SECONDS.sleep(1);
		if (overflowRecordCount >= 2) {
			Thread.sleep(30);
			assertEquals(Thread.State.WAITING, thread.getState());
			// TODO check assertEquals(Thread.State.TIMED_WAITING, thread.getState());
		}
	}
}
