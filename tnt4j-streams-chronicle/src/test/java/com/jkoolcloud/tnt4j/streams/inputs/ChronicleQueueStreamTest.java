package com.jkoolcloud.tnt4j.streams.inputs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.ChronicleQueueProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.outputs.NullActivityOutput;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJavaObjectParser;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;

public class ChronicleQueueStreamTest {

	private EntryDefinition expect;
	private int expectCount;
	private boolean fail;

	@Test
	public void testStartFromEnd() throws IOException, InterruptedException {
		System.setProperty("log4j2.configurationFile", "file:../config/log4j2.xml");
		ChronicleQueueStream stream = new ChronicleQueueStream() {
			@Override
			public Object getNextItem() throws Exception {
				Object item = super.getNextItem();
				System.out.println("####################################################################" + item);
				try {
					assertEquals(item, expect);
				} catch (AssertionError e) {
					fail = true;
				}

				return item;
			}
		};

		Path testQueue = Files.createTempDirectory("testQueue");
		ChronicleQueue queue = ChronicleQueue.single(testQueue.toFile().getAbsolutePath());
		ExcerptAppender appender = queue.acquireAppender();

		stream.setProperty(StreamProperties.PROP_FILENAME, testQueue.toFile().getAbsolutePath());
		stream.setProperty(ChronicleQueueProperties.PROP_MARSHALL_CLASS,
				"com.jkoolcloud.tnt4j.streams.inputs.ChronicleQueueStreamTest$EntryDefinition");
		stream.setProperty(ChronicleQueueProperties.PROP_START_FROM_LATEST, "true");

		stream.addReference(new NullActivityOutput());
		stream.addParser(new ActivityJavaObjectParser());

		stream.addStreamItemAccountingListener(new StreamItemAccountingListener() {
			@Override
			public void onItemLost() {
			}

			@Override
			public void onItemFiltered() {
			}

			@Override
			public void onItemSkipped() {
			}

			@Override
			public void onItemProcessed() {
				expectCount--;
			}

			@Override
			public void onBytesStreamed(long bytes) {
			}

			@Override
			public void updateTotal(TNTInputStream<?, ?> stream, long bytes, int activities) {
			}
		});

		expect = new EntryDefinition();
		expect.setName("BBB");
		// expectCount++;
		appender.writeDocument(expect);

		StreamThread streamThread = new StreamThread(stream);

		streamThread.start();

		expect = new EntryDefinition();
		expect.setName("AAA");
		expectCount++;
		appender.writeDocument(expect);

		while (expectCount != 0) {
			Thread.sleep(500);
			expectCount--; // Should not halt anyway
		}
		if (fail) {
			fail();
		}
	}

	public static class EntryDefinition extends BytesInBinaryMarshallable {
		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}
			EntryDefinition that = (EntryDefinition) o;
			return Objects.equals(name, that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), name);
		}
	}
}