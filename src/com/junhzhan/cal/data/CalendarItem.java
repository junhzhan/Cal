package com.junhzhan.cal.data;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarItem {
    public final int year;
    public final int month;
    public final int date;
    
    public CalendarItem(int year, int month, int date) {
        this.year = year;
        this.month = month;
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        CalendarItem other = (CalendarItem)o;
        return other.year == year && other.month == month && other.date == date;
    }

    @Override
    public int hashCode() {
        return year * 10000 + month * 100 + date;
    }
    
    
    /**
     * calculate difference between this one and the other CalendarDayItem by days.
     * @param item the other CalendayDayItem
     * @return Positive number of days if item is before this one. Negative number of days if item is after this one.
     * It will return 0 if item is the same day as this.
     */
    public int diff(CalendarItem item) throws IllegalArgumentException {
        Calendar cal = new GregorianCalendar(year, month, date);
        Calendar itemCal = new GregorianCalendar(item.year, item.month, item.date);
        int result = cal.compareTo(itemCal);
        Calendar start = null;
        Calendar end = null;
        if (result == 0) {
            return 0;
        }
        if (result == 1) {
            start = itemCal;
            end = cal;
        } else {
            start = cal;
            end = itemCal;
        }
        int diff = 0;
        while (start.before(end)) {
            start.add(Calendar.DATE, 1);
            diff++;
        }
        if (result == 1) {
            return diff;
        } else {
            return -diff;
        }
    }

    @Override
    public String toString() {
        return String.format("year : %d month : %d date : %d", year, month, date);
    }
    
    
    
    
}
