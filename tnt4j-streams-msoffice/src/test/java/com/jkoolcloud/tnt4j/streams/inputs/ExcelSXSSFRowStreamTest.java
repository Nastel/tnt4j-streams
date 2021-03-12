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

package com.jkoolcloud.tnt4j.streams.inputs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.MsOfficeStreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class ExcelSXSSFRowStreamTest {

	@Test
	public void testBigXSSFFile() throws Exception {
		ExcelSXSSFRowStream stream = spy(new ExcelSXSSFRowStream());

		Map<String, String> props = new HashMap<>(2);
		props.put(MsOfficeStreamProperties.PROP_SHEETS, "WA_Fn-UseC_-IT-Help-Desk");

		stream.setProperties(props.entrySet());
		stream.applyProperties();

		doReturn(true).when(stream).addInputToBuffer(any(Row.class));
		// when(stream.addInputToBuffer(any(Row.class))).thenReturn(true);
		stream.readXLXS(new File("./samples/xlsx-large/WA_Fn-UseC_-IT-Help-Desk.xlsx"));
		verify(stream, times(100001)).addInputToBuffer(any(Row.class));
	}

	@Test
	public void testBigXSSFFileLimit10Records() throws Exception {
		ExcelSXSSFRowStream stream = spy(new ExcelSXSSFRowStream());

		Map<String, String> props = new HashMap<>(3);
		props.put(StreamProperties.PROP_FILENAME, "./samples/xlsx-large/WA_Fn-UseC_-IT-Help-Desk.xlsx");
		props.put(MsOfficeStreamProperties.PROP_SHEETS, "WA_Fn-UseC_-IT-Help-Desk");
		props.put(StreamProperties.PROP_RANGE_TO_STREAM, "2:11");

		stream.setProperties(props.entrySet());
		stream.applyProperties();

		doReturn(true).when(stream).addInputToBuffer(any(Row.class));
		// when(stream.addInputToBuffer(any(Row.class))).thenReturn(true);
		stream.readXLXS(new File("./samples/xlsx-large/WA_Fn-UseC_-IT-Help-Desk.xlsx"));
		verify(stream, times(10)).addInputToBuffer(any(Row.class));
	}

	@Test
	public void testHSSFFile() throws Exception {
		ExcelSXSSFRowStream stream = spy(new ExcelSXSSFRowStream() {
			@Override
			protected boolean addInputToBuffer(Row inputData) throws IllegalStateException {
				printRow(inputData);
				return true;
			}

		});

		Map<String, String> props = new HashMap<>(2);
		props.put(MsOfficeStreamProperties.PROP_SHEETS, "WA_Fn-UseC_-IT-Help-Desk");

		stream.setProperties(props.entrySet());
		stream.applyProperties();

		// doReturn(true).when(stream).addInputToBuffer(any(Row.class));
		// when(stream.addInputToBuffer(any(Row.class))).thenReturn(true);
		stream.readXLS(new File("./samples/xlsx-large/SimpleTest.xls"));
		verify(stream, times(2)).addInputToBuffer(any(Row.class));
	}

	public static void printRow(Row row) {
		Iterator<Cell> cellIterator = row.cellIterator();
		System.out.print("|");
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			System.out.print(cell);
			System.out.print("(");
			System.out.print(cell.getCellType());
			if (cell.getCellType().equals(CellType.NUMERIC) && DateUtil.isCellDateFormatted(cell)) {
				System.out.print("Date");
			}
			System.out.print(")");
			System.out.print("\t |");
		}
		System.out.println("|");
	}

}