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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.MsOfficeStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements activity data parser that assumes each activity data item is an MS Excel
 * {@link org.apache.poi.ss.usermodel.Workbook} {@link Sheet} data structure, where each field is represented by a sheet
 * cell reference (e.g., "B12", "H12", "AA1" where letters identifies column and number identifies row) and the name is
 * used to map each field into its corresponding activity field.
 * <p>
 * This activity parser supports configuration properties from {@link AbstractExcelParser} (and higher hierarchy
 * parsers).
 *
 * @version $Revision: 2 $
 */
public class ActivityExcelSheetParser extends AbstractExcelParser<Sheet> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityExcelSheetParser.class);

	/**
	 * Constructs a new ExcelSheetParser.
	 */
	public ActivityExcelSheetParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.apache.poi.ss.usermodel.Sheet}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return Sheet.class.isInstance(data);
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity context data package having MS Excel document sheet as activity data object
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		Sheet sheet = cData.getData();

		if (StringUtils.isNotEmpty(locStr)) {
			CellReference ref = new CellReference(locStr);
			if (ref.getRow() < 0 || ref.getCol() < 0) {
				throw new ParseException(
						StreamsResources.getStringFormatted(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityExcelRowParser.unresolved.cell.reference", locStr),
						sheet.getWorkbook().getSheetIndex(sheet));
			}
			Row row = sheet.getRow(ref.getRow());
			if (row != null) {
				Cell cell = row.getCell(ref.getCol());
				if (cell != null) {
					val = getCellValue(cell);
				} else {
					val = row;
				}
			}

			logger().log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityExcelRowParser.resolved.cell.value", locStr, sheet.getWorkbook().getMissingCellPolicy(),
					toString(val));
		}

		return val;
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "EXCEL SHEET" }; // NON-NLS

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - {@code "EXCEL SHEET"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}
}
