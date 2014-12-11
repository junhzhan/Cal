package com.junhzhan.cal.widget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.cal.R;
import com.junhzhan.cal.data.CalendarItem;
import com.junhzhan.cal.data.CustomEvent;
import com.junhzhan.cal.data.EventItem;
import com.junhzhan.cal.data.SyncEvent;

/**
 * 
 * @author junhao.zhang
 *
 */
public class CalendarViewEfficient extends View {
    private static final String TAG = "CalendarView";
    
    public static final int ROW_COUNT = 6;
    public static final int COLUMN_COUNT = 7;
    
    private TextPaint mPaint;
    private Rect mRect;
    private int mYear;
    private int mMonth;
    private int mDate;
    private CalendarItem[] mData;
    
    private int mOldDate;
    
    private int mMainColor;
    private int mAltColor;
    
    private int mWeekRow;
    
    private boolean mIsWeekView;
    
    private OnCalendarDateSelectedListener mListener;
    
    private CalendarItem mDownActionTarget;
    
    
    private HashMap<CalendarItem, EventItem> mEvents;
    
    private static final int MAX_EVENT_COUNT = 8;
    private static final int MOVE_STEP = 10;
    
    private Drawable mSelectionBg;
    private int mDividerColor;
    private int mDividerWidth;
    private int mCustomEventColor;
    private int mSyncEventColor;
    private int mEventLineMargin;
    private float mEventLineWidth;
    
    private int mHighlightTop;
    private int mHighlightLeft;
    
    
    public CalendarViewEfficient(Context context) {
        super(context);
        mPaint = new TextPaint();
        mPaint.setTextSize(getResources().getDimension(R.dimen.calendar_date_text_size));
        mPaint.setColor(0xFFDBDBDB);
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Align.CENTER);
        mRect = new Rect();
        mPaint.getTextBounds("1", 0, 1, mRect);
        mMainColor = getResources().getColor(R.color.calendar_view_main_textcolor);
        mAltColor = getResources().getColor(R.color.calendar_view_alt_textcolor);
        mSelectionBg = getResources().getDrawable(R.drawable.selection_date_bg);
        mDividerColor = getResources().getColor(R.color.calendar_view_divider_color);
        mDividerWidth = getResources().getDimensionPixelOffset(R.dimen.calendar_view_divider_width);
        mCustomEventColor = getResources().getColor(R.color.calendar_view_custom_event_line_color);
        mSyncEventColor = getResources().getColor(R.color.calendar_view_sync_event_line_color);
        mEventLineMargin = getResources().getDimensionPixelOffset(R.dimen.calendar_view_event_line_margin);
        mEventLineWidth = getResources().getDimension(R.dimen.calendar_view_event_line_width);
        mEvents = new HashMap<CalendarItem, EventItem>();
        EventItem item = new EventItem();
        ArrayList<CustomEvent> customEvents = new ArrayList<CustomEvent>();
        customEvents.add(new CustomEvent());
        customEvents.add(new CustomEvent());
        item.addCustomEvent(customEvents);
        ArrayList<SyncEvent> syncEvents = new ArrayList<SyncEvent>();
        syncEvents.add(new SyncEvent());
        syncEvents.add(new SyncEvent());
        item.addSyncEvent(syncEvents);
        mEvents.put(new CalendarItem(2014, 10, 22), item);
    }
    
    
    public void setEvents(HashMap<CalendarItem, EventItem> events) {
        mEvents = events;
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mDownActionTarget = resolveTouchedDate(event.getX(), event.getY());
            break;
        case MotionEvent.ACTION_MOVE:
            break;
        case MotionEvent.ACTION_UP:
            CalendarItem target = resolveTouchedDate(event.getX(), event.getY());
            if (mDownActionTarget != null && target != null && mDownActionTarget.equals(target)
                    && mListener != null) {
                mListener.onCalendarDateSelected(target);
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            break;
        default:
            break;
        }
        return true;
    }
    
    public void resetData() {
        mYear = 0;
        mMonth = 0;
        mDate = 0;
        mData = null;
    }
    
    public void setOnDateSelectedListener(OnCalendarDateSelectedListener l) {
        mListener = l;
    }
    
    private CalendarItem resolveTouchedDate(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        int horizontalPadding = (width % COLUMN_COUNT) / 2;
        int cellWidth = width / COLUMN_COUNT;
        int cellHeight = height / ROW_COUNT;
        for (int row = 0;row < ROW_COUNT;row++) {
            for (int column = 0;column < COLUMN_COUNT;column++) {
                int cellLeft = horizontalPadding + column * cellWidth;
                int cellTop = row * cellHeight;
                if (x >= cellLeft && x <= cellLeft + cellWidth && y > cellTop && y < cellTop + cellHeight) {
                    return mData[row * COLUMN_COUNT + column];
                }
            }
        }
        return null;
    }



    public void setMonthView(int year, int month, int date) {
        mOldDate = mDate;
        mHighlightLeft = 0;
        mHighlightTop = 0;
        mIsWeekView = false;
        mYear = year;
        mMonth = month;
        mDate = date;
        mData = createCalendarData(year, month);
        invalidate();
    }
    
    public void setWeekView(int year, int month, int date, int weekRow) {
        if (weekRow < 0 || weekRow >= ROW_COUNT) {
            throw new IllegalArgumentException("target row must be in [0, ROW_CONUT)");
        }
        mOldDate = mDate;
        mHighlightLeft = 0;
        mHighlightTop = 0;
        mIsWeekView = true;
        mYear = year;
        mMonth = month;
        mDate = date;
        mWeekRow = weekRow;
        mData = createCalendarData(year, month, date, weekRow);
        invalidate();
        
    }
    
    
    private CalendarItem[] createCalendarData(int year, int month, int date, int targetRow) {
        CalendarItem[] result = new CalendarItem[COLUMN_COUNT * ROW_COUNT];
        Calendar cal = new GregorianCalendar(year, month, date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int beforeCount = targetRow * COLUMN_COUNT + dayOfWeek - 1;
        int i = 0;
        for (; i < beforeCount;i++) {
            Calendar calendar = new GregorianCalendar(year, month, date);
            calendar.add(Calendar.DATE, i - beforeCount);
            result[i] = new CalendarItem(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }
        for (int j = i;j < result.length;j++) {
            CalendarItem item = new CalendarItem(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
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
    private CalendarItem[] createCalendarData(int year, int month) {
        CalendarItem[] result = new CalendarItem[COLUMN_COUNT * ROW_COUNT];
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DATE, 1);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int i = 1;
        for (i = 1;i < dayOfWeek;i++) {
            Calendar calendar = new GregorianCalendar(year, month, 1);
            calendar.add(Calendar.DATE, i - dayOfWeek);
            result[i - 1] = new CalendarItem(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }
        
        for (int j = i - 1; j < result.length;j++) {
            CalendarItem item = new CalendarItem(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            result[j] = item;
            cal.add(Calendar.DATE, 1);
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int horizontalPadding = ((width - (COLUMN_COUNT - 1) * mDividerWidth) % COLUMN_COUNT) / 2;
        int cellWidth = (width - (COLUMN_COUNT - 1) * mDividerWidth) / COLUMN_COUNT;
        int cellHeight = (height - ROW_COUNT * mDividerWidth) / ROW_COUNT;
        for (int row = 0; row < ROW_COUNT;row++) {
            mPaint.setStrokeWidth(mDividerWidth);
            mPaint.setColor(mDividerColor);
            canvas.drawLine(0, row * (mDividerWidth + cellHeight), width, row * (mDividerWidth + cellHeight), mPaint);
        }
        
        for (int column = 1; column < COLUMN_COUNT; column++) {
            float x = horizontalPadding + (column - 1) * mDividerWidth + column * cellWidth;
            canvas.drawLine(x, 0, x, height, mPaint);
        }
        int currentLeft = 0;
        int currentTop = 0;
        int oldLeft = 0;
        int oldTop = 0;
        for (int row = 0;row < ROW_COUNT;row++) {
            for (int column = 0;column < COLUMN_COUNT;column++) {
                CalendarItem item = mData[row * COLUMN_COUNT + column];
                int cellTop = row * cellHeight + (row + 1) * mDividerWidth;
                int cellLeft = horizontalPadding + column * (cellWidth + mDividerWidth);
                float centerX = (float)(cellLeft + cellWidth / 2.0);
                float centerY = (float)(cellTop + cellHeight / 2.0);
                int customEventColor = mCustomEventColor;
                int syncEventColor = mSyncEventColor;
                if (item.month != mMonth) {
                    mPaint.setColor(mAltColor);
                } else {
                    /**
                     * Below code should be more flexible
                     */
                    if (item.date == mDate) {
                        currentLeft = cellLeft;
                        currentTop = cellTop;
                    } else {
                        if (item.date == mOldDate) {
                            oldLeft = cellLeft;
                            oldTop = cellTop;
                        }
                    }
                    mPaint.setColor(mMainColor);
                    
                }
                canvas.drawText(String.valueOf(item.date), centerX, centerY + mRect.height() / 2.0f, mPaint);
                EventItem eventItem = mEvents.get(item);
                float yPos = cellTop + cellHeight * 0.9f;
                mPaint.setStrokeWidth(mEventLineWidth);
                mPaint.setStrokeCap(Cap.ROUND);
                if (eventItem != null) {
                    int customCount = eventItem.getCustomEventCount();
                    int syncConunt = eventItem.getSyncEventCount();
                    if ((customCount + syncConunt) > MAX_EVENT_COUNT) {
                        if (customCount == 0) {
                            mPaint.setColor(syncEventColor);
                            canvas.drawLine(cellLeft + mEventLineMargin, yPos, cellLeft + cellWidth - mEventLineMargin, yPos, mPaint);
                        } else if (syncConunt == 0) {
                            mPaint.setColor(customEventColor);
                            canvas.drawLine(cellLeft + mEventLineMargin, yPos, cellLeft + cellWidth - mEventLineMargin, yPos, mPaint);
                        } else {
                            float customWidth = (cellWidth - 3 * mEventLineMargin) * ((float)customCount) / (customCount + syncConunt);
                            mPaint.setColor(customEventColor);
                            canvas.drawLine(cellLeft + mEventLineMargin, yPos, cellLeft + mEventLineMargin + customWidth, 
                                    yPos, mPaint);
                            mPaint.setColor(syncEventColor);
                            canvas.drawLine(cellLeft + mEventLineMargin * 2 + customWidth, yPos, 
                                    cellLeft + cellWidth - mEventLineMargin, yPos, mPaint);    
                        }
                        
                    } else {
                        float customWidth = (cellWidth - 3 * mEventLineMargin) / ((float)MAX_EVENT_COUNT) * customCount;
                        float syncWidth = (cellWidth - 3 * mEventLineMargin) / ((float)MAX_EVENT_COUNT) * syncConunt;
                        mPaint.setColor(customEventColor);
                        canvas.drawLine(cellLeft + mEventLineMargin, yPos, cellLeft + mEventLineMargin + customWidth, 
                                yPos, mPaint);
                        mPaint.setColor(syncEventColor);
                        canvas.drawLine(cellLeft + mEventLineMargin * 2 + customWidth, yPos, 
                                cellLeft + mEventLineMargin * 2 + customWidth + syncWidth, yPos, mPaint);
                    }
                }
            }
        }
        
        if (currentLeft != 0 && currentTop != 0) {
            Log.e(TAG, "current left and top not 0");
            if (oldLeft != 0 || oldTop != 0) {
                if (mHighlightLeft == 0) {
                    mHighlightLeft = oldLeft;
                }
                if (mHighlightTop == 0) {
                    mHighlightTop = oldTop;
                }
                float deltaX = (currentLeft - oldLeft) * 1.0f / MOVE_STEP;
                float deltaY = (currentTop - oldTop) * 1.0f / MOVE_STEP;
                if (mHighlightLeft != currentLeft || mHighlightTop != currentTop) {
                    if (Math.abs(mHighlightLeft - oldLeft + deltaX) > Math.abs(currentLeft - oldLeft) || 
                            Math.abs(mHighlightTop - oldTop + deltaY) > Math.abs(currentTop - oldTop)) {
                        mHighlightLeft = currentLeft;
                        mHighlightTop = currentTop;
                    } else {
                        mHighlightLeft += deltaX;
                        mHighlightTop += deltaY;
                    }
                    mSelectionBg.setBounds(mHighlightLeft, mHighlightTop, mHighlightLeft + cellWidth, mHighlightTop + cellHeight);
                    mSelectionBg.draw(canvas);
                    invalidate();
                } else {
                    mSelectionBg.setBounds(mHighlightLeft, mHighlightTop, mHighlightLeft + cellWidth, mHighlightTop + cellHeight);
                    mSelectionBg.draw(canvas);
                }
            } else {
                mSelectionBg.setBounds(currentLeft, currentTop, currentLeft + cellWidth, currentTop + cellHeight);
                mSelectionBg.draw(canvas);
            }
        }
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
    
    public CalendarItem getDay() {
        return new CalendarItem(mYear, mMonth, mDate);
    }
    
    
}
