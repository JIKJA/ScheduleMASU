package com.example.schedule;

import android.util.Log;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SAXparser {
	File file;
	ArrayList<String> teachers;

	SAXparser(File file, ArrayList<String> teachers){
		this.file = file;
		this.teachers = teachers;
	}

	public void getGroups(ArrayList<String> parsingData) throws IOException{
		// parses file and write group names to parsingData as Day
		OPCPackage container;
		try {
			container = OPCPackage.open(file.getAbsolutePath());
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(container);
			XSSFReader xssfReader = new XSSFReader(container);
			StylesTable styles = xssfReader.getStylesTable();
			XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator)xssfReader.getSheetsData();
			while (iter.hasNext()) {
				InputStream stream = iter.next();
				String sheetName = iter.getSheetName();
				Pattern p = Pattern.compile("Лист\\d");
				Matcher m = p.matcher(sheetName);
				if (!m.matches()) {
					parsingData.add(sheetName);
				}
				stream.close();
			}
			container.close();
		} catch(InvalidFormatException e) {
			e.printStackTrace();
		} catch(SAXException e) {
			e.printStackTrace();
		} catch(OpenXML4JException e) {
			e.printStackTrace();
		}
	}

	public void parseExcel(ArrayList<Day> parsingData, String group) throws IOException {
		// parses file and write schedule data to parsingData as Day
		OPCPackage container;
		try {
			container = OPCPackage.open(file.getAbsolutePath());
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(container);
			XSSFReader xssfReader = new XSSFReader(container);
			StylesTable styles = xssfReader.getStylesTable();
			XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator)xssfReader.getSheetsData();
			int index = 0;
			while (iter.hasNext()) {
				InputStream stream = iter.next();
				String sheetName = iter.getSheetName();
				if (!sheetName.toLowerCase().contains("лист")) {
					Log.e("INFO", sheetName + " [index=" + index + "]");
					if (group==null || sheetName.equals(group)) {
						processSheet(styles, strings, new XMLhandler(parsingData, teachers, group), stream);
					}
				}
				stream.close();
				index++;
			}
			container.close();
		} catch(InvalidFormatException e) {
			e.printStackTrace();
		} catch(SAXException e) {
			e.printStackTrace();
		} catch(OpenXML4JException e) {
			e.printStackTrace();
		}

	}

	protected void processSheet(StylesTable styles, ReadOnlySharedStringsTable strings, XMLhandler xmlhandler, InputStream sheetInputStream) throws IOException, SAXException {

		InputSource sheetSource = new InputSource(sheetInputStream);
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxFactory.newSAXParser();
			XMLReader sheetParser = saxParser.getXMLReader();
			ContentHandler handler = new XSSFSheetXMLHandler(styles, strings, xmlhandler, false );
			sheetParser.setContentHandler(handler);
			sheetParser.parse(sheetSource);
		} catch(ParserConfigurationException e) {
			throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
		}
	}
}


