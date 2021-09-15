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

import java.util.EnumSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.poi.ss.usermodel.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.MsOfficeParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract activity data parser that assumes each activity data item can MS Excel
 * {@link org.apache.poi.ss.usermodel.Workbook} contained data structure (e.g.,
 * {@link org.apache.poi.ss.usermodel.Sheet} or {@link org.apache.poi.ss.usermodel.Row}), where each field is
 * represented by a {@link org.apache.poi.ss.usermodel.Cell} and the sheet name and cell identifier (row number and
 * column letter) is used to map cell(s) contained data into its corresponding activity field.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>UseFormattedCellValue - indicator flag stating to use formatted cell value (always {@link java.lang.String}) as
 * field/locator RAW data. When this flag is set to {@code true} - original cell value provided by Apache POI API is
 * used e.g., making all numeric cells values as decimals ({@code double}) what is not very comfortable when entered
 * cell value is integer. Default value - {@code false}. (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#ParserProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#SystemProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#EnvVariable}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractExcelParser<T> extends GenericActivityParser<T> {

	private boolean useFormattedCellValue = false;

	private DataFormatter formatter;
	protected final Lock formatLock = new ReentrantLock();
	private FormulaEvaluator evaluator;
	protected final Lock evaluationLock = new ReentrantLock();

	/**
	 * Constructs a new AbstractExcelParser.
	 */
	protected AbstractExcelParser() {
		super(ActivityFieldDataType.AsInput);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (MsOfficeParserProperties.PROP_USE_FORMATTED_VALUE.equalsIgnoreCase(name)) {
			useFormattedCellValue = Utils.toBoolean(value);

			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"), name,
					value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (MsOfficeParserProperties.PROP_USE_FORMATTED_VALUE.equalsIgnoreCase(name)) {
			return useFormattedCellValue;
		}

		return super.getProperty(name);
	}

	/**
	 * Evaluates and returns cell contained value.
	 *
	 * @param cell
	 *            cell instance to evaluate value
	 * @return evaluated cell value
	 */
	protected Object getCellValue(Cell cell) {
		if (useFormattedCellValue) {
			return getFormattedCellValue(cell);
		} else {
			return getOriginalCellValue(cell);
		}
	}

	private String getFormattedCellValue(Cell cell) {
		formatLock.lock();
		try {
			if (formatter == null) {
				formatter = new DataFormatter();
			}
			return formatter.formatCellValue(cell, getEvaluator(cell));
		} finally {
			formatLock.unlock();
		}
	}

	private Object getOriginalCellValue(Cell cell) {
		switch (cell.getCellType()) {
		case BOOLEAN:
			return cell.getBooleanCellValue();
		case NUMERIC:
			return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
		case FORMULA:
			return evaluateCellFormula(cell);
		case STRING:
			return cell.getRichStringCellValue().toString();
		case BLANK:
			return null;
		default:
			return cell.toString();
		}
	}

	private FormulaEvaluator getEvaluator(Cell cell) {
		evaluationLock.lock();
		try {
			if (evaluator == null) {
				Workbook workbook = cell.getSheet().getWorkbook();
				evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			}

			return evaluator;
		} finally {
			evaluationLock.unlock();
		}
	}

	private Object evaluateCellFormula(Cell cell) {
		CellValue cellValue;
		evaluationLock.lock();
		try {
			cellValue = getEvaluator(cell).evaluate(cell);
		} finally {
			evaluationLock.unlock();
		}

		return getCellValue(cell, cellValue);
	}

	private static Object getCellValue(Cell cell, CellValue cellValue) {
		if (cellValue == null) {
			return cell.toString();
		}

		switch (cellValue.getCellType()) {
		case BOOLEAN:
			return cellValue.getBooleanValue();
		case NUMERIC:
			return DateUtil.isCellDateFormatted(cell) ? DateUtil.getJavaDate(cellValue.getNumberValue())
					: cellValue.getNumberValue();
		case STRING:
			return cellValue.getStringValue();
		default:
			return cellValue.formatAsString();
		}
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
