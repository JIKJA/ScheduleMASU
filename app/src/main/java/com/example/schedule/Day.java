package com.example.schedule;

import org.apache.poi.ss.formula.functions.Today;

import java.util.ArrayList;

import schemasMicrosoftComVml.STTrueFalse;

public class Day {
    private String date;
    private String group;
    private ArrayList<Lesson> lessons;

    public Day(){
        date = "";
        group = "";
        lessons = new ArrayList<>();
    }

    public Day copy(){
        Day day = new Day();
        day.setDate(date);
        day.setGroup(group);
        for(int i = 0; i < lessons.size(); i++){
            day.addLesson(lessons.get(i).copy());
        }
        return day;
    }

    public boolean isEmpty(){
        if(date == "" && group == "" && lessons.isEmpty()){
            return true;
        } else {
            return false;
        }
    }

    public void deleteEmptyLessons(){
        for(int i = lessons.size() - 1; i >= 0; i--){
            if (lessons.get(i).isEmpty() == true){
                lessons.remove(i);
            }
        }
    }

    public void addNewLesson(){
        lessons.add(new Lesson());
    }

    public void addLesson(Lesson lesson){
        lessons.add(lesson);
    }

    public void addLastLessonTimeInfo(String start, String finish){
        lessons.get(lessons.size() - 1).start_time = start;
        lessons.get(lessons.size() - 1).finish_time = finish;
    }

    public void addLastLessonNameInfo(String name, String type){
        lessons.get(lessons.size() - 1).lesson_name = name;
        lessons.get(lessons.size() - 1).lesson_type = type;
    }

    public void addLastLessonMetaInfo(String teacher, String place){
        lessons.get(lessons.size() - 1).teacher = teacher;
        lessons.get(lessons.size() - 1).place = place;
    }

    public void setDate(String date){
        this.date = date;
    }

    public String getDate(){
        return date;
    }

    public void setGroup(String group){
        this.group = group;
    }

    public String getGroup(){
        return group;
    }

    public Integer getLessonsNumber(){
        return lessons.size();
    }

    public Lesson getLesson(int i){
        return lessons.get(i);
    }

    public boolean equals(Day day){
        if ( !(date.equals(day.getDate()) && group.equals(day.getGroup())) ) {return false;}
        if (this.getLessonsNumber() != day.getLessonsNumber()) {return false;}
        for (int i = 0; i < this.getLessonsNumber(); i++){
            if (!this.getLesson(i).equals(day.getLesson(i))) {return false;}
        }
        return true;
    }

    class Lesson{
        public String start_time = "";
        public String finish_time = "";
        public String lesson_name = "";
        public String lesson_type = "";
        public String teacher = "";
        public String place = "";

        public Lesson copy(){
            Lesson lesson = new Lesson();
            lesson.start_time = start_time;
            lesson.finish_time = finish_time;
            lesson.lesson_name = lesson_name;
            lesson.lesson_type = lesson_type;
            lesson.teacher = teacher;
            lesson.place = place;
            return lesson;
        }

        public boolean isEmpty(){
            if (lesson_name.equals("") && lesson_type.equals("") && teacher.equals("") && place.equals("")){
                return true;
            } else {
                return false;
            }
        }

        public boolean equals(Lesson lesson){
            return this.lesson_name.equals(lesson.lesson_name) && this.lesson_type.equals(lesson.lesson_type) && this.teacher.equals(lesson.teacher) &&
                    this.place.equals(lesson.place) && this.start_time.equals(lesson.start_time) && this.finish_time.equals(lesson.finish_time);
        }
    }
}
