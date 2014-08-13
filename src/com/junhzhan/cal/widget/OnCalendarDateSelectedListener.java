package com.junhzhan.cal.widget;

import java.util.List;


public interface OnCalendarDateSelectedListener<T> {
    void onCalendarDateSelected(CalendarDayItem item, List<T> events);
}