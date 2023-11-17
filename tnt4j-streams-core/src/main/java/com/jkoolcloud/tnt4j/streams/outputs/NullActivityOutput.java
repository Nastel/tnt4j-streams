package com.jkoolcloud.tnt4j.streams.outputs;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Class for TNT4J-Streams output logging {@link com.jkoolcloud.tnt4j.streams.fields.ActivityInfo} to nowhere.
 * 
 * @version $Revision: 1 $
 */
public class NullActivityOutput extends AbstractTNTStreamOutput<ActivityInfo> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JKCloudActivityOutput.class);

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void logItem(ActivityInfo item) throws Exception {
		super.logItem(item);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"NullActivityOutput.log.item", item);
		notifyLoggingFinish(item);
	}

	@Override
	public void initialize() throws Exception {
	}

	@Override
	public void handleConsumerThread(Thread t) throws IllegalStateException {
	}

	@Override
	public void setProperty(String name, Object value) {
	}

	@Override
	public boolean hasSink(String sinkId) {
		return true;
	}
}
