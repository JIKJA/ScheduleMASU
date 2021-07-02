package com.example.schedule;

import android.os.Debug;
import android.util.Log;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLhandler implements SheetContentsHandler {
	private ArrayList<Day> result = null;
	private ArrayList<String> teachersRef;
	private ArrayList<Day> daysRow = null;
	private String group;
	private int currentRow = -1;
	private int currentCol = -1;
	private int parseType = 0; // 0 - dates, 1 - lesson names, 2 - teachers and places
	private ArrayList<Integer> rangeIdx = new ArrayList<>();
	private ArrayList<Integer> teachers = new ArrayList<>();
	private int minColumns = 87;

	public XMLhandler(ArrayList<Day> list, ArrayList<String> teachers, String sheet_name) {
		this.result = list;
		this.teachersRef = teachers;
		this.group = sheet_name;
	}

	@Override
	public void startRow(int i) {
		//Log.e("ROW", String.valueOf(i));
		currentRow = i;
		currentCol = 0;
		if (((currentRow - 9) % 15) == 0) {
			daysRow = new ArrayList<>();
			parseType = 0;
		}
	}

	@Override
	public void endRow() {
		if (currentRow == 84) { // bug if NY in saturday
			// add sunday
			SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
			Calendar cal = Calendar.getInstance();
			Day sunday = new Day();
			sunday.addNewLesson();
			sunday.addLastLessonTimeInfo("8.30", "10.05");
			sunday.addLastLessonNameInfo("Выходной день", "");
			sunday.setGroup(group);
			for(int j = 0; j < daysRow.size(); j++){
				try {
					cal.setTime(format.parse(daysRow.get(j).getDate()));
					cal.add(Calendar.DATE, 1);
					sunday.setDate(format.format(cal.getTime()));
					result.add(sunday.copy());
				} catch (ParseException e){}
			}
		}
		if (((currentRow - 8) % 15) == 0) {
			if (rangeIdx.isEmpty() == false){
				// parse days range
				for(int j = 0; j < rangeIdx.size(); j++){
					String[] rangeSubstr = daysRow.get(rangeIdx.get(j)).getDate().split("-");
					int start = Integer.parseInt(rangeSubstr[0].trim());
					int finish = Integer.parseInt(rangeSubstr[1].trim().split(" ")[0]);
					SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy");
					SimpleDateFormat inputFormat = new SimpleDateFormat("d MMMMM yyyy", new Locale("ru", "RU"));
					for(int dayiter = start; dayiter <= finish; dayiter++){
						try{
							Date date = inputFormat.parse(String.valueOf(dayiter) + rangeSubstr[1].substring(rangeSubstr[1].indexOf(" ", 1)));
							daysRow.add(daysRow.get(rangeIdx.get(j)).copy());
							daysRow.get(daysRow.size() - 1).setDate(outputFormat.format(date));
						} catch (ParseException e){}
					}
					daysRow.remove((int) rangeIdx.get(j));
				}
				rangeIdx.clear();
			}
			// copy days to result
			if (daysRow != null){
				// delete empty lessons and days
				for(int j = daysRow.size() - 1; j >= 0; j--){
					daysRow.get(j).deleteEmptyLessons();
					if (daysRow.get(j).getLessonsNumber() == 0){
						daysRow.remove(j);
					}
				}
				for (int j = 0; j < daysRow.size() - 1; j++){
					result.add(daysRow.get(j).copy());
				}
			}
		}
		if (parseType == 0) {
			parseType = 1;
		} else if (parseType == 1) {
			parseType = 2;
		} else {
			parseType = 1;
		}
	}

	@Override
	public void cell(String cellReference, String formattedValue) {
		int thisCol = (new CellReference(cellReference)).getCol();
		int missedCols = thisCol - currentCol - 1;
		if (missedCols > 0) {
			if (currentRow > 8) {
				for (int i = currentCol + 1; i < thisCol; i++) {
					if (parseType == 0) {
						if (i % 2 == 0 && i > 0) {
							daysRow.add(new Day());
							daysRow.get(daysRow.size() - 1).setDate("missing date"); // try repair date
							daysRow.get(daysRow.size() - 1).setGroup(group);
						}
					} else if (parseType == 1) {
						if (i % 2 == 1 && i > 0) {
							daysRow.get((i - 1) / 2).addNewLesson();
						} else if (i % 2 == 0 && i > 0) {
							// if empty lesson name cell do nothing
						}
					} else {
						// if empty teacher/place cell do nothing
					}
				}
			}
		}

		if (currentRow >= 9){
			if (parseType == 0) {
				// parse date cells
				// 0 col - Перерывы, odd col - day of week, even col - dates
				if (thisCol % 2 == 0 && thisCol > 0) {
					daysRow.add(new Day());
					// parse dates
					SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy");
					SimpleDateFormat inputFormat = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy");
					try{
						Date date = inputFormat.parse(formattedValue);
						daysRow.get(daysRow.size() - 1).setDate(outputFormat.format(date));
					} catch (ParseException e){
						// check if date match range
						Pattern p = Pattern.compile("\\d+\\s-\\s\\d+\\s[а-я]+\\s\\d{4}\\sг\\.");
						Matcher m = p.matcher(formattedValue);
						if (m.matches() == true){
							rangeIdx.add(0, (thisCol - 2)/2); // insert to 0 to delete elements from daysRow in decrement way(without problems)
							daysRow.get(daysRow.size() - 1).setDate(formattedValue);
						} else {
							daysRow.get(daysRow.size() - 1).setDate("missing date");
						}
					}
					daysRow.get(daysRow.size() - 1).setGroup(group);
				}
			} else if (parseType == 1){
				// parse lesson time and name cells
				// 0 col - breaks, odd col - lesson time, even col - lesson names
				if (thisCol % 2 == 1 && thisCol > 0){
					// lesson time info
					daysRow.get((thisCol - 1) / 2).addNewLesson();
					String[] timeInfo = formattedValue.split("-");
					daysRow.get((thisCol - 1) / 2).addLastLessonTimeInfo(timeInfo[0], timeInfo[1]);
				} else if (thisCol % 2 == 0 && thisCol > 0){
					// lesson name info
					Pattern p = Pattern.compile("(\\(((пр|ПР)|(лк|ЛК)|(лб|ЛБ))\\))| ((пр|ПР)|(лк|ЛК)|(лб|ЛБ)) |(\\(((пр|ПР)|(лк|ЛК)|(лб|ЛБ))(\\/|\\\\)((пр|ПР)|(лк|ЛК)|(лб|ЛБ))\\))"); // regex to find lesson type
					Matcher m = p.matcher(formattedValue);
					String lesson_name, lesson_type;
					if (m.find() == true){
						lesson_type = formattedValue.substring(m.start(), m.end()).trim().replace("(","").replace(")","").replace("\\", "/").toLowerCase();
						lesson_name = formattedValue.substring(0, m.start()) + formattedValue.substring(m.end());
					} else {
						lesson_type = "";
						lesson_name = formattedValue;
					}
					daysRow.get((thisCol - 2)/2).addLastLessonNameInfo(lesson_name, lesson_type);
				}
			} else {
				// parse lesson meta cells
				String teacher = "";
				String place = "";
				if (formattedValue.indexOf("//")==-1){
					String[] teacherAndPlace = formattedValue.split("/");
					if (teacherAndPlace.length == 1) { // meta cell may contain only teacher name without "/" and place
						teacher = parseTeacher(teacherAndPlace[0]);
					} else {
						teacher = parseTeacher(teacherAndPlace[0]);
						place = teacherAndPlace[1].trim();
					}
				} else {
					String[] teacherAndPlace = formattedValue.split("//");
					String[] firstGroup = teacherAndPlace[0].trim().split("/");
					String[] secondGroup = teacherAndPlace[1].trim().split("/");
					teacher = parseTeacher(firstGroup[0]) + ", " + parseTeacher(secondGroup[0]);
					place = firstGroup[1].trim() + ", " + secondGroup[1].trim();
				}
				daysRow.get((thisCol - 2)/2).addLastLessonMetaInfo(teacher, place);

			}
		}
		currentCol = thisCol;
	}

	@Override
	public void headerFooter(String s, boolean b, String s1) {
	}

	private String parseTeacher(String teacher){
		String teacherFinal = "";
		// try to parse simple pattern N.N.Surname or Surname N.N.
		Pattern p = Pattern.compile("([А-Я][^а-яА-Я]{1,2}){2}[А-Я][а-я]{1,}(-[А-Я][а-я]{1,})?|[А-Я][а-я]{1,}(-[А-Я][а-я]{1,})?[^а-яА-Я][А-Я][^а-яА-Я]{1,2}[А-Я][^а-яА-Я]{0,1}");
		Matcher m = p.matcher(teacher);
		while (m.find()){
			String[] teacherParts = m.group().replaceAll("[^а-яА-Я-]","").split("(?<!-)(?=[А-Я])");
			if(teacherParts[0].equals("")){
				teacherParts = Arrays.copyOfRange(teacherParts, 1, teacherParts.length);
			}
			if(teacherParts.length == 3){
				if(teacherParts[0].length()==1){
					if (!teacherFinal.equals("")){
						teacherFinal += ", ";
					}
					teacherFinal += teacherParts[2]+" "+teacherParts[0]+"."+teacherParts[1];
				} else if(teacherParts[2].length()==1){
					if (!teacherFinal.equals("")){
						teacherFinal += ", ";
					}
					teacherFinal += teacherParts[0]+" "+teacherParts[1]+"."+teacherParts[2];
				}
			}
		}
		if (teacherFinal.equals("")){
			// if unsuccessful then try to parse hard pattern Name Name2 Surname or Surname Name Name2
			p = Pattern.compile("([А-Я][а-я]{1,}[^а-яА-Я-]?){2}[А-Я][а-я]{1,}(-[А-Я][а-я]{1,})?|[А-Я][а-я]{1,}(-[А-Я][а-я]{1,})?([^а-яА-Я-]?[А-Я][а-я]{1,}){2}");
			m = p.matcher(teacher);
			while (m.find()){
				String[] teacherParts = m.group().replaceAll("[^а-яА-Я-]","").split("(?<!-)(?=[А-Я])");
				if(teacherParts[0].equals("")){
					teacherParts = Arrays.copyOfRange(teacherParts, 1, teacherParts.length);
				}
				for (int i = 0; i < teachersRef.size()-1; i++){
					String surname = teachersRef.get(i).substring(0, teachersRef.get(i).indexOf(' '));
					if (teacherParts[0].equals(surname)){
						String maybeName = teacherParts[0]+" "+teacherParts[1].substring(0,1)+"."+teacherParts[2].substring(0,1);
						if (maybeName.equals(teachersRef.get(i))){
							if (!teacherFinal.equals("")){
								teacherFinal += ", ";
							}
							teacherFinal += maybeName;
						}
					} else {
						if (teacherParts[2].equals(surname)){
							String maybeName = teacherParts[2]+" "+teacherParts[0].substring(0,1)+"."+teacherParts[1].substring(0,1);
							if (maybeName.equals(teachersRef.get(i))){
								if (!teacherFinal.equals("")){
									teacherFinal += ", ";
								}
								teacherFinal = maybeName;
							}
						}
					}
				}
				// if pattern passed and surname does't contains in teachers list from MASU site then add that teacher (hard to understand where surname)
				if (teacherFinal.equals("")){
					if (!teacherFinal.equals("")){
						teacherFinal += ", ";
					}
					teacherFinal += teacherParts[0]+" "+teacherParts[1]+" "+teacherParts[2];
				}
			}
		}
		// if teacherFinal is empty then teacher cell does't contain teacher name (may contain, but there are hard to parse)
		return teacherFinal;
	}
}
