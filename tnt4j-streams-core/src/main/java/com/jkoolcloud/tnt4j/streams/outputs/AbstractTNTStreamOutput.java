package com.jkoolcloud.tnt4j.streams.outputs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

public abstract class AbstractTNTStreamOutput<T> implements TNTStreamOutput<T> {

	private String name;
	private TNTInputStream<?, ?> stream;

	private boolean closed = false;
	private Collection<OutputStreamListener> outputListeners;

	protected AbstractTNTStreamOutput() {
	}

	protected AbstractTNTStreamOutput(String name) {
		this.name = name;
	}

	/**
	 * Returns logger used by this stream output handler.
	 *
	 * @return parser logger
	 */
	protected abstract EventSink logger();

	@Override
	public void setStream(TNTInputStream<?, ?> inputStream) {
		this.stream = inputStream;
	}

	@Override
	public TNTInputStream<?, ?> getStream() {
		return stream;
	}

	/**
	 * Returns output name value.
	 *
	 * @return output name value
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets output name value.
	 *
	 * @param name
	 *            output name value
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	protected void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Override
	public void cleanup() {
		closed = true;

		if (outputListeners != null) {
			outputListeners.clear();
		}
	}

	@Override
	public void logItem(T item) throws Exception {
		notifyLoggingStart(item);
	}

	@Override
	public void addOutputListener(OutputStreamListener listener) {
		if (outputListeners == null) {
			outputListeners = new ArrayList<>(3);
		}

		outputListeners.add(listener);
	}

	/**
	 * Notifies activity item has been entered logging procedure.
	 *
	 * @param item
	 *            logged activity item
	 */
	protected void notifyLoggingStart(T item) {
		if (outputListeners != null) {
			for (OutputStreamListener listener : outputListeners) {
				listener.onItemLogStart(stream, item);
			}
		}
	}

	/**
	 * Notifies activity item logging procedure has been completed.
	 *
	 * @param item
	 *            logged activity item
	 */
	protected void notifyLoggingFinish(T item) {
		if (outputListeners != null) {
			for (OutputStreamListener listener : outputListeners) {
				listener.onItemLogFinish(item);
			}
		}
	}

	/**
	 * Notifies activity item entity (itself or child) has been sent.
	 *
	 * @param item
	 *            recorded activity item
	 * @param trackable
	 *            recorded trackable instance
	 */
	protected void notifyEntityRecorded(T item, Trackable trackable) {
		if (outputListeners != null) {
			for (OutputStreamListener listener : outputListeners) {
				listener.onItemRecorded(item, trackable);
			}
		}
	}
}
