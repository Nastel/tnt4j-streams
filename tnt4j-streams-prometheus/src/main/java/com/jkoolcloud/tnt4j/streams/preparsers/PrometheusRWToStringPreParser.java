/*
 * Copyright 2014-2022 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;

import prometheus.Remote;
import prometheus.Types;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from binary data {@code byte[]} of
 * Prometheus Remote-Write protobuf message to list of selected format strings: JSON or TEXT.
 * <p>
 * Default string print format is
 * {@link com.jkoolcloud.tnt4j.streams.preparsers.ProtoMessageToStringPreParser.PrintFormatType#JSON}.
 *
 * @version $Revision: 1 $
 */
public class PrometheusRWToStringPreParser extends AbstractPreParser<byte[], Collection<String>> {

	private static final JsonFormat.Printer jsonPrinter = JsonFormat.printer();
	private static final TextFormat.Printer textPrinter = TextFormat.printer();

	/**
	 * Protobuf message print format type.
	 */
	protected final ProtoMessageToStringPreParser.PrintFormatType printFormatType;

	/**
	 * Constructs a new PrometheusRWToStringPreParser.
	 */
	protected PrometheusRWToStringPreParser() {
		this(ProtoMessageToStringPreParser.PrintFormatType.JSON);
	}

	/**
	 * Constructs a new PrometheusRWToStringPreParser.
	 *
	 * @param printFormatType
	 *            protobuf message print format type
	 */
	protected PrometheusRWToStringPreParser(ProtoMessageToStringPreParser.PrintFormatType printFormatType) {
		this.printFormatType = printFormatType;
	}

	/**
	 * Constructs a new PrometheusRWToStringPreParser.
	 *
	 * @param printFormatTypeName
	 *            protobuf message print format type name
	 */
	protected PrometheusRWToStringPreParser(String printFormatTypeName) {
		printFormatType = StringUtils.isEmpty(printFormatTypeName) ? ProtoMessageToStringPreParser.PrintFormatType.JSON
				: ProtoMessageToStringPreParser.PrintFormatType.valueOf(printFormatTypeName.toUpperCase());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@code byte[]}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof byte[];
	}

	@Override
	public Collection<String> preParse(byte[] data) throws Exception {
		Remote.WriteRequest writeRequest = Remote.WriteRequest.parseFrom(data);
		List<Types.MetricMetadata> metadataList = writeRequest.getMetadataList();
		List<Types.TimeSeries> timeSeriesList = writeRequest.getTimeseriesList();

		// String reqJson = jsonPrinter.print(writeRequest);

		if (CollectionUtils.isNotEmpty(timeSeriesList)) {
			Collection<String> timeSeriesStrList = new ArrayList<>(timeSeriesList.size());
			for (Types.TimeSeries ts : timeSeriesList) {
				String msgString;
				switch (printFormatType) {
				case TEXT:
					msgString = textPrinter.printToString(ts);
					break;
				case JSON:
				default:
					msgString = jsonPrinter.print(ts);
					break;
				}
				timeSeriesStrList.add(msgString);
			}

			return timeSeriesStrList;
		}

		return null;
	}

	@Override
	public String dataTypeReturned() {
		switch (printFormatType) {
		case TEXT:
			return "TEXT"; // NON-NLS
		case JSON:
		default:
			return "JSON"; // NON-NLS
		}
	}
}