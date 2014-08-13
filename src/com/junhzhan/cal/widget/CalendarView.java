package com.junhzhan.cal.widget;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.example.cal.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * @author junhao.zhang
 *
 */
public abstract class CalendarView extends LinearLayout {
    private static final String TAG = "CalendarView";
    
    public static final int ROW_COUNT = 6;
    public static final int COLUMN_COUNT = 7;
    
    private static final int INVALID_WEEK_ROW = -1;
    
    private int mYear;
    private int mMonth;
    private int mDate;
    private int mActualYear;
    private int mActualMonth;
    private int mActualDate;
    private CalendarDayItem[] mData;
    
    
    private View[] mDateViews = new View[ROW_COUNT * COLUMN_COUNT];
    
    
    private CalendarViewClickListener[] mClickListeners = new CalendarViewClickListener[ROW_COUNT * COLUMN_COUNT];
    

    
    private int mWeekRow;
    
    private boolean mIsWeekView;
    private Drawable mSelectionDrawable;
    
    
    
    
    CalendarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        for (int i = 0;i < ROW_COUNT;i++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LayoutParams layout = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1);
            addView(row, layout);
        }
        
    }
    
    /**
     * 
     * @param item
     */
    protected abstract View onCreateDateView(CalendarDayItem date, CalendarDayItem selectedDate, boolean weekOrMonthView);
    
    protected abstract void onDateViewInvalidate(View dateView, CalendarDayItem date, CalendarDayItem selectedDate, boolean weekOrMonthView);
    
    protected void onDateSelected(CalendarDayItem item) {
        
    }
    
    public void resetData() {
        mYear = 0;
        mMonth = 0;
        mDate = 0;
        mData = null;
    }
    
    
    
    public void setMonthView(int year, int month, int date) {
        Log.e(TAG, "setMonthView");
        mIsWeekView = false;
        mYear = year;
        mMonth = month;
        mDate = date;
        mData = createCalendarData(year, month);
        inflateDateView();
    }
    
    private void inflateDateView() {
        if (mData == null || mData.length != ROW_COUNT * COLUMN_COUNT) {
            throw new IllegalStateException("data size not correct");
        }
        CalendarDayItem currentItem = new CalendarDayItem(mYear, mMonth, mDate);
        if (mIsWeekView) {
            for (int i = 0;i < COLUMN_COUNT;i++) {
                LinearLayout row = (LinearLayout)getChildAt(mWeekRow);
                int index = i + mWeekRow * COLUMN_COUNT;
                View dateView = mDateViews[index];
                if (dateView == null) {
                    dateView = onCreateDateView(mData[index], currentItem, true);
                    if (dateView == null) {
                        throw new IllegalStateException("create date view return null");
                    }
                    CalendarViewClickListener clickListener = new CalendarViewClickListener();
                    mClickListeners[index] = clickListener;
                    dateView.setOnClickListener(clickListener);
                    View item = row.getChildAt(i);
                    LayoutParams itemLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT
                            , 1);
                    if (item == null) {
                        row.addView(dateView, itemLayout);
                    } else {
                        row.removeViewAt(i);
                        row.addView(dateView, i, itemLayout);
                    }
                    mDateViews[index] = dateView;
                }
                setTextPart(dateView, mData[index], currentItem);
                onDateViewInvalidate(dateView, mData[index], currentItem, true);
                mClickListeners[index].setDate(mData[index]);
            }
        } else {
            for (int i = 0; i < mData.length;i++) {
                View dateView = mDateViews[i];
                LinearLayout row = (LinearLayout)getChildAt(i / COLUMN_COUNT);
                int column = i % COLUMN_COUNT;
                if (dateView == null) {
                    dateView = onCreateDateView(mData[i], currentItem, false);
                    if (dateView == null) {
                        throw new IllegalStateException("create date view return null");
                    }
                    CalendarViewClickListener clickListener = new CalendarViewClickListener();
                    dateView.setOnClickListener(clickListener);
                    mClickListeners[i] = clickListener;
                    View item = row.getChildAt(column);
                    LayoutParams itemLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT
                            , 1);
                    if (item == null) {
                        row.addView(dateView, itemLayout);
                    } else {
                        row.removeViewAt(column);
                        row.addView(dateView, column, itemLayout);
                    }
                    mDateViews[i] = dateView;
                }
                setTextPart(dateView, mData[i], currentItem);
                onDateViewInvalidate(dateView, mData[i], currentItem, false);
                mClickListeners[i].setDate(mData[i]);
            }    
        }
        
    }
    
    private void setTextPart(View dateView, CalendarDayItem date, CalendarDayItem currentDate) {
        View view = dateView.findViewById(R.id.date_view_text_part_id);
        if (view == null) {
            return;
        }
        TextView textPart = (TextView)view;
        if (date.equals(currentDate)) {
            textPart.setTextColor(0xff000000);
        } else if (date.year == currentDate.year && date.month == currentDate.month) {
            textPart.setTextColor(0xffdbdbdb);
        } else {
            textPart.setTextColor(0xfff4f4f4);
        }
        textPart.setText(String.valueOf(date.date));
    }
    
    private class CalendarViewClickListener implements OnClickListener {
        
        private CalendarDayItem mDate;
        CalendarViewClickListener() {
            
        }
        
        void setDate(CalendarDayItem date) {
            mDate = date;
        }

        @Override
        public void onClick(View v) {
            onDateSelected(mDate);
        }
        
    }
    
    public void setWeekView(int year, int month, int date, int weekRow) {
        Log.e(TAG, "setWeekView");
        if (weekRow < 0 || weekRow >= ROW_COUNT) {
            throw new IllegalArgumentException("target row must be in [0, ROW_CONUT)");
        }
        mIsWeekView = true;
        mYear = year;
        mMonth = month;
        mDate = date;
        mWeekRow = weekRow;
        mData = createCalendarData(year, month, date, weekRow);
        inflateDateView();
        
    }
    
    public void setActualCalendar(int actualYear, int actualMonth, int actualDate) {
        mActualYear = actualYear;
        mActualMonth = actualMonth;
        mActualDate = actualDate;
        invalidate();
    }
    
    
    private CalendarDayItem[] createCalendarData(int year, int month, int date, int targetRow) {
        CalendarDayItem[] result = new CalendarDayItem[COLUMN_COUNT * ROW_COUNT];
        Calendar cal = new GregorianCalendar(year, month, date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int beforeCount = targetRow * COLUMN_COUNT + dayOfWeek - 1;
        int i = 0;
        for (; i < beforeCount;i++) {
            Calendar calendar = new GregorianCalendar(year, month, date);
            calendar.add(Calendar.DATE, i - beforeCount);
            result[i] = new CalendarDayItem(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }
        for (int j = i;j < result.length;j++) {
            CalendarDayItem item = new CalendarDayItem(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            result[j] = item;
            cal.add(Calendar.DATE, 1);
        }
        return result;
    }
    
    /**
     * create calendar data for month of year.
     * @param year 
     * @param month should be in range between {@link Calendar#JANUARY} and {@link Calendar#DECEMBER} 
     * @return
     */
    private CalendarDayItem[] createCalendarData(int year, int month) {
        CalendarDayItem[] result = new CalendarDayItem[COLUMN_COUNT * ROW_COUNT];
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DATE, 1);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int i = 1;
        for (i = 1;i < dayOfWeek;i++) {
            Calendar calendar = new GregorianCalendar(year, month, 1);
            calendar.add(Calendar.DATE, i - dayOfWeek);
            result[i - 1] = new CalendarDayItem(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }
        
        for (int j = i - 1; j < result.length;j++) {
            CalendarDayItem item = new CalendarDayItem(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            result[j] = item;
            cal.add(Calendar.DATE, 1);
        }
        return result;
    }

    
    public int getRowIndexOfDay() {
        if (mYear == 0) {
            return -1;
        }
        if (mIsWeekView) {
            return mWeekRow;
        } else {
            Calendar cal = new GregorianCalendar(mYear, mMonth, mDate);
            return cal.get(Calendar.WEEK_OF_MONTH) - 1;
        }
    }
    
    public CalendarDayItem getDay() {
        return new CalendarDayItem(mYear, mMonth, mDate);
    }
    
    /**
     * set background drawable for selected date
     * @param d
     */
    public void setSelectionDrawable(Drawable d) {
        mSelectionDrawable = d;
    }
    
}
