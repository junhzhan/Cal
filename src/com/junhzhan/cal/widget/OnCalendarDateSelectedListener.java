package com.junhzhan.cal.widget;

import java.util.List;

import com.junhzhan.cal.data.CalendarItem;


public interface OnCalendarDateSelectedListener {
    void onCalendarDateSelected(CalendarItem item);
    void onCalendarDateLongPressed(CalendarItem item);
}