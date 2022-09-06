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

package com.jkoolcloud.tnt4j.streams.inputs;

import org.apache.poi.ss.usermodel.Sheet;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements a MS Excel {@link org.apache.poi.ss.usermodel.Workbook} stored activity stream, where each workbook
 * {@link Sheet} is assumed to represent a single activity or event which should be recorded.
 * <p>
 * NOTE: since this stream uses DOM based access of MS Excel file contents, memory stream used consumption may be
 * significant. But it provides all features of cell value formatting and formula value evaluation.
 * <p>
 * This activity stream requires parsers that can support {@link Sheet} data.
 * <p>
 * This activity stream supports configuration properties from {@link AbstractExcelStream} (and higher hierarchy
 * streams).
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ExcelSheetStream extends AbstractExcelStream<Sheet> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ExcelSheetStream.class);

	/**
	 * Constructs a new ExcelSheetStream. Requires configuration settings to set input stream source.
	 */
	public ExcelSheetStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a excel workbook {@link Sheet} containing the contents of the next raw activity data item.
	 */
	@Override
	public Sheet getNextItem() throws Exception {
		Sheet sheet = getNextNameMatchingSheet(true);

		if (sheet != null) {
			addStreamedBytesCount(getSheetBytesCount(sheet));
		}

		return sheet;
	}
}
