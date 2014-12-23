package com.junhzhan.cal.widget;

import com.junhzhan.cal.data.CalendarItem;

public interface OnEventListScrollListener {
    public void onScrolled(CalendarItem firstVisibleDate, CalendarItem lastVisibleDate);
}
