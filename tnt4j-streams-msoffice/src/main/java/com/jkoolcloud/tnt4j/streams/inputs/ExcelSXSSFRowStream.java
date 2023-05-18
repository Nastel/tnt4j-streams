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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.*;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.MsOfficeStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements a MS Excel {@link org.apache.poi.xssf.streaming.SXSSFWorkbook} stored activity stream, where each workbook
 * sheet {@link SXSSFRow} is assumed to represent a single activity or event which should be recorded.
 * <p>
 * NOTE: to read MS Excel file Apache POI SXSSF API is used to optimize memory consumption. But in some cases cell value
 * formatting and formula value evaluation may be limited! If you notice any critical inaccuracies, use
 * {@link com.jkoolcloud.tnt4j.streams.inputs.ExcelRowStream} instead.
 * <p>
 * This activity stream requires parsers that can support {@link org.apache.poi.ss.usermodel.Row} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>FileName - the system-dependent file name of MS Excel document. (Required)</li>
 * <li>SheetsToProcess - defines workbook sheets name filter mask (wildcard or RegEx) to process only sheets which names
 * matches this mask. (Optional)</li>
 * <li>WorkbookPassword - excel workbook password. (Optional)</li>
 * <li>RangeToStream - defines streamed data rows index range. Default value - {@code 1:}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ExcelSXSSFRowStream extends AbstractBufferedStream<Row> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ExcelSXSSFRowStream.class);

	/**
	 * Stream attribute defining file name.
	 */
	private String fileName = null;

	private String sheetName = null;
	private Pattern sheetNameMatcher = null;

	private String wbPass = null;

	private String rangeValue = "1:"; // NON-NLS
	private IntRange rowRange;

	private int totalRows = 0;
	private long totalBytes = 0;

	private boolean ended = false;

	/**
	 * Constructs a new ExcelSXSSFRowStream. Requires configuration settings to set input stream source.
	 */
	public ExcelSXSSFRowStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			fileName = value;
		} else if (MsOfficeStreamProperties.PROP_SHEETS.equalsIgnoreCase(name)) {
			sheetName = value;

			if (StringUtils.isNotEmpty(sheetName)) {
				sheetNameMatcher = Pattern.compile(Utils.wildcardToRegex2(sheetName));
			}
		} else if (StreamProperties.PROP_RANGE_TO_STREAM.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				rangeValue = value;
			}
		} else if (MsOfficeStreamProperties.PROP_WORKBOOK_PASS.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				wbPass = decPassword(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (MsOfficeStreamProperties.PROP_SHEETS.equalsIgnoreCase(name)) {
			return sheetName;
		}
		if (StreamProperties.PROP_RANGE_TO_STREAM.equalsIgnoreCase(name)) {
			return rangeValue;
		}
		if (MsOfficeStreamProperties.PROP_WORKBOOK_PASS.equalsIgnoreCase(name)) {
			return encPassword(wbPass);
		}

		return super.getProperty(name);
	}

	@Override
	protected long getActivityItemByteSize(Row activityItem) {
		return activityItem == null ? 0 : getRowByteSize(activityItem);
	}

	@Override
	public int getTotalActivities() {
		return totalRows;
	}

	@Override
	public long getTotalBytes() {
		return totalBytes;
	}

	private static long getRowByteSize(Row row) {
		int bCount = 0;

		if (row != null) {
			Iterator<Cell> cells = row.cellIterator();
			while (cells.hasNext()) {
				Cell c = cells.next();

				CellType ct = c.getCellType();
				switch (ct) {
				case BOOLEAN:
				case ERROR:
					bCount += 1;
					break;
				case NUMERIC:
					bCount += 8; // double
					break;
				case STRING:
				case FORMULA:
					bCount += c.getStringCellValue().getBytes().length;
					break;
				case BLANK:
				case _NONE:
				default:
					break;
				}
			}
		}

		return bCount;
	}

	@Override
	protected boolean isInputEnded() {
		return ended;
	}

	/**
	 * Checks if row is within stream defined range and adds it to stream input buffer.
	 *
	 * @param row
	 *            row instance to add to buffer
	 * @param rowNumber
	 *            row number to check if it is in range
	 * @return {@code true} if row was added to input buffer, {@code false} = otherwise
	 *
	 * @throws IllegalStateException
	 *             if buffer queue is not initialized
	 */
	protected boolean checkAndAddToInputToBuffer(Row row, int rowNumber) throws IllegalStateException {
		// NOTE: adding 1, since rowNumber is 0 based while for user convenience range starts from 1.
		int rRowNum = rowNumber + 1;
		// must pass rowNumber cause inputData.getRowNum() iterates whole sheet
		if (IntRange.inRange(rowRange, rRowNum)) {
			return addInputToBuffer(row);
		} else {
			skipFilteredActivities();
		}

		// if (rowRange.getTo().compareTo(rRowNum) < 0) {
		// //TODO jump to next available sheet
		// }

		return false;
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(fileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_FILENAME));
		}

		rowRange = IntRange.getRange(rangeValue);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		Thread excelFileReader = new Thread(() -> {
			try {
				File inputFile = new File(fileName);
				totalBytes = inputFile.length();
				InputStream is = null;
				FileMagic fm;

				try {
					is = Files.newInputStream(inputFile.toPath());
					is = FileMagic.prepareToCheckMagic(is);
					fm = FileMagic.valueOf(is);
				} finally {
					Utils.close(is);
				}

				if (fm == FileMagic.OOXML) {
					readXLXS(inputFile);
				} else if (fm == FileMagic.OLE2) {
					readXLS(inputFile);
				} else {
					throw new IOException(
							StreamsResources.getStringFormatted(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
									"ExcelSXSSFRowStream.unsupported.format", fileName));
				}
			} catch (Exception e) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.file.read.failed", fileName, e);
			}
			ended = true;
			offerDieMarker();
		}, getName() + "FileReaderThread");
		excelFileReader.start();
	}

	/**
	 * Reads HSSF (XLS) format excel file using Apache POI streaming SXSSF API.
	 *
	 * @param xlsFile
	 *            excel HSSF format file to read
	 *
	 * @throws IOException
	 *             if excel file or workbook can't be read
	 */
	protected void readXLS(File xlsFile) throws IOException {
		InputStream dis = null;
		boolean passwordSet = false;

		try (POIFSFileSystem fs = new POIFSFileSystem(xlsFile, true)) {
			DirectoryNode root = fs.getRoot();
			if (root.hasEntry("EncryptedPackage")) { // NON-NLS
				dis = DocumentFactoryHelper.getDecryptedStream(fs, wbPass);
			} else {
				if (wbPass != null) {
					Biff8EncryptionKey.setCurrentUserPassword(wbPass);
					passwordSet = true;
				}
				dis = fs.createDocumentInputStream("Workbook"); // NON-NLS
			}
			HSSFRequest req = new HSSFRequest();

			XLSEventListener listener = new XLSEventListener(this);
			FormatTrackingHSSFListener formatsListener = new FormatTrackingHSSFListener(listener, Locale.getDefault());
			listener.setFormatListener(formatsListener);
			req.addListenerForAllRecords(formatsListener);
			HSSFEventFactory factory = new HSSFEventFactory();
			factory.processEvents(req, dis);
		} finally {
			if (passwordSet) {
				Biff8EncryptionKey.setCurrentUserPassword(null);
			}

			Utils.close(dis);
		}
	}

	/**
	 * Reads XSSF (XLXS) format excel file using Apache POI streaming SXSSF API.
	 *
	 * @param xlsxFile
	 *            excel XSSF format file to read
	 *
	 * @throws IOException
	 *             if excel file or workbook can't be read
	 * @throws SAXException
	 *             if file contained XML reading fails
	 * @throws OpenXML4JException
	 *             if file contained XML reading fails
	 */
	protected void readXLXS(File xlsxFile) throws IOException, SAXException, OpenXML4JException {
		try (OPCPackage xlsxPackage = OPCPackage.open(xlsxFile, PackageAccess.READ)) {
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
			XSSFReader xssfReader = new XSSFReader(xlsxPackage);
			StylesTable styles = xssfReader.getStylesTable();
			SXSSFWorkbook workbook = new SXSSFWorkbook();
			XSSFReader.SheetIterator sIter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
			while (sIter.hasNext()) {
				try (InputStream sStream = sIter.next()) {
					String sheetName = sIter.getSheetName();
					boolean match = sheetNameMatcher == null || sheetNameMatcher.matcher(sheetName).matches();
					if (!match) {
						continue;
					}

					SXSSFSheet sheet = workbook.createSheet();
					processSXSSFSheet(styles, strings, new XLSXSheetContentHandler(this, sheet), sStream);
				}
			}
		}
	}

	/**
	 * Parses the content of one sheet using the specified styles and shared-strings tables.
	 *
	 * @param styles
	 *            the table of styles that may be referenced by cells in the sheet
	 * @param strings
	 *            the table of strings that may be referenced by cells in the sheet
	 * @param sheetHandler
	 *            the sheet handler
	 * @param sheetInputStream
	 *            the input stream to read the sheet-data from
	 *
	 * @throws IOException
	 *             if sheet input stream read fails
	 * @throws SAXException
	 *             if sheet input stream provided XML can't be parsed
	 */
	public static void processSXSSFSheet(StylesTable styles, ReadOnlySharedStringsTable strings,
			SheetContentsHandler sheetHandler, InputStream sheetInputStream) throws IOException, SAXException {
		DataFormatter formatter = new DataFormatter();
		InputSource sheetSource = new InputSource(sheetInputStream);
		try {
			XMLReader sheetParser = XMLHelper.newXMLReader();
			ContentHandler handler = new XSSFSheetXMLHandler(styles, null, strings, sheetHandler, formatter, false);
			sheetParser.setContentHandler(handler);
			sheetParser.parse(sheetSource);
		} catch (ParserConfigurationException exc) {
			throw new RuntimeException(StreamsResources.getStringFormatted(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
					"ExcelSXSSFRowStream.sax.cfg.error", Utils.getExceptionMessages(exc)));
		}
	}

	private static class XLSXSheetContentHandler implements SheetContentsHandler {
		private ExcelSXSSFRowStream stream;

		private SXSSFSheet sheet;
		private SXSSFRow currentRow = null;

		private int currentRowNum = -1;
		private int currentColNum = -1;

		/**
		 * Constructs a new XLSXSheetContentHandler.
		 *
		 * @param stream
		 *            referenced stream instance
		 * @param sheet
		 *            sheet instance to process
		 */
		XLSXSheetContentHandler(ExcelSXSSFRowStream stream, SXSSFSheet sheet) {
			this.stream = stream;
			this.sheet = sheet;
		}

		@Override
		public void startRow(int rowNum) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
					"ExcelSXSSFRowStream.starting.xssf.row", rowNum);

			this.currentRowNum = rowNum;
			currentColNum = -1;
			currentRow = new SXSSFRow(sheet);
			stream.totalRows = sheet.getLastRowNum();
		}

		@Override
		public void endRow(int rowNum) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
					"ExcelSXSSFRowStream.ending.xssf.row", rowNum);

			stream.checkAndAddToInputToBuffer(currentRow, rowNum);
			currentRow = null;
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
					"ExcelSXSSFRowStream.got.xssf.cell", cellReference, formattedValue,
					comment == null ? null : comment.getString());

			// gracefully handle missing CellRef here in a similar way as XSSFCell does
			if (cellReference == null) {
				cellReference = new CellAddress(currentRowNum, currentColNum).formatAsString();
			}

			currentColNum = new CellReference(cellReference).getCol();

			try {
				double v = Double.parseDouble(formattedValue);
				Cell cell = currentRow.createCell(currentColNum, CellType.NUMERIC);
				cell.setCellValue(v);
			} catch (NumberFormatException e) {
				Cell cell = currentRow.createCell(currentColNum, CellType.STRING);
				cell.setCellValue(formattedValue);
			}
		}

		@Override
		public void headerFooter(String text, boolean isHeader, String tagName) {
		}
	}

	private static class XLSEventListener implements HSSFListener {
		private ExcelSXSSFRowStream stream;

		private SXSSFSheet sheet;
		private SXSSFRow currentRow = null;

		private int currentRowNum = -1;
		private int currentColNum = -1;

		private Queue<String> sheets = new LinkedList<>();
		private String currentSheet;
		private SSTRecord sstRec;

		private FormatTrackingHSSFListener formatListener;

		/**
		 * Constructs a new XLSEventListener.
		 *
		 * @param stream
		 *            referenced stream instance
		 */
		public XLSEventListener(ExcelSXSSFRowStream stream) throws IOException {
			this.stream = stream;
			this.sheet = new SXSSFWorkbook().createSheet();
		}

		/**
		 * Sets format tracking listener to format cell values.
		 *
		 * @param formatListener
		 *            format tracking listener instance
		 */
		void setFormatListener(FormatTrackingHSSFListener formatListener) {
			this.formatListener = formatListener;
		}

		/**
		 * Handles processing of provided HSSF records.
		 *
		 * @param record
		 *            the record to be processed
		 */
		@Override
		public void processRecord(org.apache.poi.hssf.record.Record record) {
			boolean currentSheetMatches = currentSheet == null || stream.sheetNameMatcher == null
					|| stream.sheetNameMatcher.matcher(currentSheet).matches();

			switch (record.getSid()) {
			case BOFRecord.sid:
				BOFRecord bof = (BOFRecord) record;
				if (bof.getType() == bof.TYPE_WORKBOOK) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.workbook");
				} else if (bof.getType() == bof.TYPE_WORKSHEET) {
					currentSheet = sheets.poll();
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.sheet", currentSheet);

				}
				break;
			case SSTRecord.sid:
				sstRec = (SSTRecord) record;
				for (int i = 0; i < sstRec.getNumUniqueStrings(); i++) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.sstRecord", i, sstRec.getString(i));
				}
				break;
			case BoundSheetRecord.sid:
				BoundSheetRecord bsr = (BoundSheetRecord) record;
				sheets.add(bsr.getSheetname());
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.bsRecord", bsr.getSheetname(), bsr.getPositionOfBof());
				break;
			case EOFRecord.sid:
				if (currentRow != null && currentSheetMatches) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.sheets.end");
					stream.checkAndAddToInputToBuffer(currentRow, currentRowNum);
				}
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.workbook.end");
				break;
			case RowRecord.sid:
				RowRecord rowRec = (RowRecord) record;
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.row", currentSheet, rowRec.getRowNumber(), rowRec.getFirstCol(),
						rowRec.getLastCol());
				break;
			case LabelSSTRecord.sid:
				LabelSSTRecord lRec = (LabelSSTRecord) record;
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.string.cell", sstRec.getString(lRec.getSSTIndex()), lRec.getRow(),
						lRec.getColumn());
				if (!currentSheetMatches) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.record.skip", currentSheet);
					return;
				}
				processCell(lRec);
				SXSSFCell cell = currentRow.createCell(lRec.getColumn(), CellType.STRING);
				cell.setCellValue(String.valueOf(sstRec.getString(lRec.getSSTIndex())));
				break;
			case NumberRecord.sid:
				NumberRecord numRec = (NumberRecord) record;
				if (!currentSheetMatches) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.record.skip", currentSheet);
					return;
				}
				processCell(numRec);
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.number.cell", numRec.getValue(), numRec.getRow(), numRec.getColumn());

				creteCell(CellType.NUMERIC, formatNumberValue(numRec, numRec.getValue()), numRec);
				break;
			case FormulaRecord.sid:
				FormulaRecord fr = (FormulaRecord) record;
				if (!currentSheetMatches) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.record.skip", currentSheet);
					return;
				}
				processCell(fr);
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.formula.cell", fr.getValue(), fr.getRow(), fr.getColumn());
				creteCell(CellType.NUMERIC, formatNumberValue(fr, fr.getValue()), fr);
				break;
			case BoolErrRecord.sid:
				BoolErrRecord bErrRec = (BoolErrRecord) record;
				if (!currentSheetMatches) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.record.skip", currentSheet);
					return;
				}
				processCell(bErrRec);
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.hssf.error.cell", bErrRec.getErrorValue(), bErrRec.getRow(),
						bErrRec.getColumn());
				cell = currentRow.createCell(bErrRec.getColumn(), CellType.ERROR);
				cell.setCellErrorValue(bErrRec.getErrorValue());
				break;
			default:
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
						"ExcelSXSSFRowStream.unhandled.hssf.record", record);
			}
		}

		private void creteCell(CellType type, Object value, CellRecord cellRec) {
			SXSSFCell cell = currentRow.createCell(cellRec.getColumn(), type);
			if (value instanceof Date) {
				cell.setCellValue((Date) value);
				CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
				String formatString = formatListener.getFormatString(cellRec);
				CreationHelper createHelper = sheet.getWorkbook().getCreationHelper();
				cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(formatString));
				cell.setCellStyle(cellStyle);
			} else if (value instanceof Double) {
				cell.setCellValue((Double) value);
			} else {
				cell.setCellValue(value.toString());
			}
		}

		private void processCell(CellRecord cellRec) {
			if (currentRowNum != cellRec.getRow()) {
				if (currentRowNum != -1) {
					LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
							"ExcelSXSSFRowStream.hssf.rows.end");
					stream.checkAndAddToInputToBuffer(currentRow, cellRec.getRow());
				}
				currentRow = new SXSSFRow(sheet);
				currentRowNum = cellRec.getRow();
				currentColNum = cellRec.getColumn();
				stream.totalRows = sheet.getLastRowNum();
			}
		}

		/**
		 * Formats provided {@code value} as date when {@code cell} has date formatting. If {@code cell} has number
		 * formatting, formats {@code value} as decimal number. In all other cases returns {@code value} without
		 * changes.
		 *
		 * @param cell
		 *            cell instance containing value
		 * @param value
		 *            number/date value
		 * @return date string built from {@code value} if {@code cell} has date formatting, formatted {@code value}
		 *         number string if {@code cell} has number formatting, plain {@code value} - otherwise.
		 */
		private Object formatNumberValue(CellValueRecordInterface cell, double value) {
			if (formatListener == null) {
				return value;
			}
			int formatIndex = formatListener.getFormatIndex(cell);
			String formatString = formatListener.getFormatString(cell);

			if (formatString == null) {
				return value;
			}

			if (DateUtil.isADateFormat(formatIndex, formatString) && DateUtil.isValidExcelDate(value)) {
				// Java wants M not m for month
				formatString = formatString.replace('m', 'M');
				// Change \- into -, if it's there
				formatString = formatString.replaceAll("\\\\-", "-"); // NON-NLS

				Date d = DateUtil.getJavaDate(value, false);
				DateFormat df = new SimpleDateFormat(formatString);
				return df.format(d);
			}

			if (formatString == "General") { // NON-NLS
				return value;
			}

			DecimalFormat df = new DecimalFormat(formatString);
			return df.format(value);
		}
	}
}
