package com.junhzhan.cal.widget;

public class CalendarDayItem {
    public final int year;
    public final int month;
    public final int date;
    
    public CalendarDayItem(int year, int month, int date) {
        this.year = year;
        this.month = month;
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        CalendarDayItem other = (CalendarDayItem)o;
        return other.year == year && other.month == month && other.date == date;
    }

    @Override
    public int hashCode() {
        return year * 10000 + month * 100 + date;
    }
    
    
    
}
