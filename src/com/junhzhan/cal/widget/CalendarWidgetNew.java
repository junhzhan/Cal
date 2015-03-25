package com.junhzhan.cal.widget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cal.R;
import com.junhzhan.cal.data.CalendarItem;
import com.junhzhan.cal.data.CustomEvent;
import com.junhzhan.cal.data.EventItem;
import com.junhzhan.cal.data.SyncEvent;

public class CalendarWidgetNew extends LinearLayout {
    
    private static final String TAG = "CalendarWidgetNew";
    
    private static final String[] DAY_OF_WEEK_TITLE = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
    
    private static final String FONT_NAME = "Helvetica.ttf";
    
    /**
     * It should be a value between 0 and 1.0f
     */
    private static final float AUTO_SCROLL_PERCENTAGE = 0.2f;
    
    private View mResizableContainer;
    private ScrollContainer mScrollableContainer;
    
    
    private int mActualMonth;
    private int mActualYear;
    private int mActualDate;
    
    private int mRowHeight;
    private int mViewPagerScrollState;
    private int mFocusedRow;
    
    private OnCalendarDateSelectedListener mOutListener;
    
    private ViewPager mCalendarPager;
    
    private Handler mHandler = new Handler();
    
    private CalendarPagerAdapter mAdapter;
    
    private boolean mExtraNeedLayout = true;
    
    
    private HashMap<CalendarItem, EventItem> mEvents = new HashMap<CalendarItem, EventItem>();
    
    private boolean mViewPagerScrolling = false;
    
    private View mExtraView;
    private FrameLayout mExtraViewContainer;

    public CalendarWidgetNew(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CalendarWidgetNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalendarWidgetNew(Context context) {
        super(context);
        init();
    }
    
    private void init() {
        
        setOrientation(VERTICAL);
        
        GregorianCalendar cal = new GregorianCalendar();
        mActualYear = cal.get(Calendar.YEAR);
        mActualMonth = cal.get(Calendar.MONTH);
        mActualDate = cal.get(Calendar.DAY_OF_MONTH);
        
        /*week day title part*/
        LinearLayout dayTitleContainer = new LinearLayout(getContext());
        dayTitleContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
        Typeface typeface = Typeface.createFromAsset(getResources().getAssets(), FONT_NAME);
        String[] titles = getResources().getStringArray(R.array.week_day_titles);
        for (int i = 0;i < DAY_OF_WEEK_TITLE.length;i++) {
            TextView dayTitleView = new TextView(getContext());
            dayTitleView.setTypeface(typeface);
            dayTitleView.setText(titles[i]);
            dayTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.calendar_day_of_week_title_text_size));
            dayTitleView.setTextColor(0xFF000000);
            dayTitleView.setGravity(Gravity.CENTER);
            LayoutParams dayTitleLayout = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            dayTitleContainer.addView(dayTitleView, dayTitleLayout);
        }
        
        super.addView(dayTitleContainer, LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.calendar_day_of_week_title_height));
        
        /* main part */
        ScrollContainer scrollContainer = new ScrollContainer(getContext());
        LinearLayout resizeContainer = new LinearLayout(getContext());
        resizeContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
        scrollContainer.addView(resizeContainer, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        super.addView(scrollContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        
        /* ViewPager for calendar */
        mCalendarPager = new ViewPager(getContext());
        mAdapter = new CalendarPagerAdapter();
        mCalendarPager.setAdapter(mAdapter);
        mCalendarPager.setCurrentItem(1);
        mCalendarPager.setOnPageChangeListener(mPageChangeListener);
        
        resizeContainer.addView(mCalendarPager, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mResizableContainer = resizeContainer;
        mScrollableContainer = scrollContainer;
        mExtraViewContainer = new FrameLayout(getContext());
        mExtraViewContainer.setBackgroundColor(0xFFFF0000);
        mScrollableContainer.addView(mExtraViewContainer, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                mScrollableContainer.scroll(-15, true);
            }
        });
        
    }
    
    public void setExtraView(View view) {
        mExtraView = view;
        mExtraViewContainer.addView(view, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
    }
    
    private float mLastPointYInCalendarWidget;
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (mViewPagerScrolling) {
                return false;
            }
            mScrollableContainer.mTargetRow = mFocusedRow;
            break;
        case MotionEvent.ACTION_MOVE:
            float deltaY = event.getY() - mLastPointYInCalendarWidget;
            mScrollableContainer.scroll(deltaY, false);
            break;
        case MotionEvent.ACTION_UP:
            if (mResizableContainer.getHeight() - mScrollableContainer.getScrollY() > mRowHeight * CalendarViewEfficient.ROW_COUNT / 2) {
                mScrollableContainer.mAutoScrollDelta = 15;
            } else {
                mScrollableContainer.mAutoScrollDelta = -15;
            }
            mScrollableContainer.scroll(mScrollableContainer.mAutoScrollDelta, true);
            break;
            
        }
        mLastPointYInCalendarWidget = event.getY();
        return true;
    }

    public void setOnCalendarDateSelectedListener(OnCalendarDateSelectedListener l) {
        mOutListener = l;
    }
    
    public void addCustomEvent(int year, int month, int date, List<CustomEvent> events) {
        if (events == null) {
            return;
        }
        CalendarItem item = new CalendarItem(year, month, date);
        EventItem eventItem = mEvents.get(item);
        if (eventItem == null) {
            eventItem = new EventItem();
            mEvents.put(item, eventItem);
        }
        eventItem.addCustomEvent(events);
    }
    
    public void addSyncEvent(int year, int month, int date, List<SyncEvent> events) {
        if (events == null) {
            return;
        }
        CalendarItem item = new CalendarItem(year, month, date);
        EventItem eventItem = mEvents.get(item);
        if (eventItem == null) {
            eventItem = new EventItem();
            mEvents.put(item, eventItem);
        }
        eventItem.addSyncEvent(events);
    }
    
    public void notifyEventDataChanged() {
        mAdapter.mDataChanged = true;
        mAdapter.finishUpdate(null);
    }
    
    
    public void setDate(int year, int month, int date) {
        mAdapter.setDate(year, month, date);
    }
    
    
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalArgumentException();
        }
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalArgumentException();
        }
        if (mCalendarPager.getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            int height = MeasureSpec.getSize(widthMeasureSpec);
            mCalendarPager.getLayoutParams().height = height / CalendarViewEfficient.COLUMN_COUNT * CalendarViewEfficient.ROW_COUNT;
            mRowHeight = height / CalendarViewEfficient.COLUMN_COUNT;
            requestLayout();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void addView(View child, int index, LayoutParams params) {
        throw new UnsupportedOperationException();
    }

    
    public void adjustHeight(boolean fillParent) {
        int scrollContainerHeight = mScrollableContainer.getHeight() == 0 ? mScrollableContainer
                .getMeasuredHeight() : mScrollableContainer.getHeight();
        int resizeContainerHeight = mResizableContainer.getHeight() == 0 ? mResizableContainer
                .getMeasuredHeight() : mResizableContainer.getHeight();
        int extraContainerVisibleHeight = scrollContainerHeight - resizeContainerHeight
                - mScrollableContainer.getScrollY();
        if (fillParent) {
            Log.d(TAG,
                    String.format(
                            "listview fill scroll container %d resize container %d scroll y %d",
                            scrollContainerHeight, resizeContainerHeight,
                            getScrollY()));
            mExtraViewContainer.getLayoutParams().height = scrollContainerHeight
                    - (resizeContainerHeight - mScrollableContainer
                            .getScrollY());
            mExtraViewContainer.requestLayout();
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getTop() < extraContainerVisibleHeight
                        && child.getBottom() > extraContainerVisibleHeight) {
                    getLayoutParams().height = child.getBottom();
                    requestLayout();
                    break;
                }
            }
        }

    }

    private class ScrollContainer extends LinearLayout {

        
        private float mDownPointY;
        private float mDownPointX;
        private float mLastPointY;
        
        private boolean mProcessActionDown;
        
        private float mAutoScrollDelta;
        private int mTargetRow;
        
        private boolean mDownActionInList;
        
        private float mDeltaY;
        
        public ScrollContainer(Context context) {
            super(context);
            setOrientation(VERTICAL);
        }
        

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            Log.e(TAG, String.format("view pager scrolling %s, action %d", mViewPagerScrolling, ev.getAction()));
            if (mViewPagerScrolling && ev.getAction() == MotionEvent.ACTION_DOWN) {
                return false;
            }
            float scrolledPositionX = ev.getX() + getScrollX();
            float scrolledPositionY = ev.getY() + getScrollY();
            
            return super.dispatchTouchEvent(ev);
        }




        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.d(TAG, String.format("scroll onIntercept action %d", ev.getAction()));
            boolean intercept = false;
            switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownPointY = ev.getY();
                mDownPointX = ev.getX();
                mProcessActionDown = false;
                float scrolledPositionX = ev.getX() + getScrollX();
                float scrolledPositionY = ev.getY() + getScrollY();
                if (scrolledPositionX >= mExtraViewContainer.getLeft() && scrolledPositionX <= mExtraViewContainer.getRight()
                        && scrolledPositionY >= mExtraViewContainer.getTop() && scrolledPositionY <= mExtraViewContainer.getBottom()) {
                    mDownActionInList = true;
                } else {
                    mDownActionInList = false;
                }
                Log.d(TAG, "down action in list " + mDownActionInList);
                return false;
                
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, String.format("scroll onIntercept x %f, y %f", ev.getX(), ev.getY()));
                float totalDeltaY = Math.abs(ev.getY() - mDownPointY);
                float totalDeltaX = Math.abs(ev.getX() - mDownPointX);
                float deltaY = ev.getY() - mDownPointY;
                if (mAdapter.isWeekOrMonthView()) {
                    if (!mDownActionInList) {
                        if (mViewPagerScrollState == ViewPager.SCROLL_STATE_IDLE && totalDeltaY > 50 && totalDeltaX < totalDeltaY) {
                            mTargetRow = mFocusedRow;
                            intercept = true;
                        }
                    }
                } else {
                    if (!mDownActionInList) {
                        if (mViewPagerScrollState == ViewPager.SCROLL_STATE_IDLE && totalDeltaY > 50 && totalDeltaX < totalDeltaY) {
                            mTargetRow = mFocusedRow;
                            intercept = true;
                        }
                    } else {
                        if (deltaY < -10) {
                            mTargetRow = mFocusedRow;
                            intercept = true;
                        }
                    }
                }
                
            }
            mLastPointY = ev.getY();
            Log.d(TAG, "scroll onIntercept return " + intercept);
            return intercept;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            String log = "";
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                log = "ACTION_DOWN";
                mProcessActionDown = true;
                break;
            case MotionEvent.ACTION_MOVE:
                log = "ACTION_MOVE";
                if (mProcessActionDown) {
                    break;
                }
                
                if (event.getY() - mLastPointY != 0.0) {
                    mDeltaY = event.getY() - mLastPointY;
                }
                scroll(mDeltaY, false);
                break;
            case MotionEvent.ACTION_UP:
                log = "ACTION_UP";
                if (mProcessActionDown) {
                    break;
                }
                if (mDeltaY > 0) {
                    if (mResizableContainer.getHeight() - getScrollY() > mRowHeight * CalendarViewEfficient.ROW_COUNT * AUTO_SCROLL_PERCENTAGE) {
                        mAutoScrollDelta = 15;
                    } else {
                        mAutoScrollDelta = -15;
                    }
                } else {
                    if (mResizableContainer.getHeight() - getScrollY() < mRowHeight * CalendarViewEfficient.ROW_COUNT * 
                            (1 - AUTO_SCROLL_PERCENTAGE)) {
                        mAutoScrollDelta = -15;
                    } else {
                        mAutoScrollDelta = 15;
                    }
                }
                scroll(mAutoScrollDelta, true);
                break;
            }
            Log.d(TAG, String.format("scroll container action %d, x %f, y %f", event.getAction(), x, y));
            mLastPointY = event.getY();
            return true;
        }
        
        private Runnable mRunnable = new Runnable() {
            
            @Override
            public void run() {
                scroll(mAutoScrollDelta, true);
            }
        };
        
        private void scroll(float deltaY, boolean auto) {
            if (auto) {
                mExtraNeedLayout = false;
            } else {
                mHandler.removeCallbacks(mRunnable);
            }
            if (deltaY > 0) {
                if (getScrollY() > 0) {
                    if ((getScrollY() - deltaY) <= 0) {
                        scrollTo(0, 0);
                        if (auto) {
                            mHandler.post(mRunnable);
                        }
                    } else {
                        scrollBy(0, (int) (-deltaY));
                        if (auto) {
                            mHandler.postDelayed(mRunnable, 10);
                        }
                    }
                } else {
                    if (mResizableContainer.getHeight() <= CalendarViewEfficient.ROW_COUNT * mRowHeight) {
                        if ((mResizableContainer.getHeight() + deltaY) >= CalendarViewEfficient.ROW_COUNT * mRowHeight) {
                            mResizableContainer.getLayoutParams().height = CalendarViewEfficient.ROW_COUNT * mRowHeight;
                            mResizableContainer.requestLayout();
                            onExpanded();
                        } else {
                            mResizableContainer.getLayoutParams().height = mResizableContainer.getHeight() + (int)deltaY;
                            mResizableContainer.requestLayout();
                            if (auto) {
                                mHandler.post(mRunnable);
                            }
                        }
                    }
                }
            } else if (deltaY < 0) {
                if (getScrollY() < mTargetRow * mRowHeight) {
                    if ((getScrollY() - deltaY) > mTargetRow * mRowHeight) {
                        scrollTo(0, mTargetRow * mRowHeight);
                        if (auto) {
                            mHandler.post(mRunnable);
                        }
                    } else {
                        scrollBy(0, (int)(-deltaY));
                        if (auto) {
                            mHandler.postDelayed(mRunnable, 10);
                        }
                    }
                } else {
                    if (mResizableContainer.getHeight() >= (mTargetRow + 1) * mRowHeight) {
                        if ((mResizableContainer.getHeight() + (int)deltaY) <= (mTargetRow + 1) * mRowHeight) {
                            mResizableContainer.getLayoutParams().height = (mTargetRow + 1) * mRowHeight;
                            mResizableContainer.requestLayout();
                            onUnExpanded();
                        } else {
                            mResizableContainer.getLayoutParams().height = mResizableContainer.getHeight() + (int)deltaY;
                            mResizableContainer.requestLayout();
                            if (auto) {
                                mHandler.post(mRunnable);
                            }
                        }
                    }
                }
            } else {
                
            }
        }
    }
    
    private void onExpanded() {
        mExtraNeedLayout = true;
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                mAdapter.setMonthView();
            }
        });
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                adjustHeight(true);
            }
        });
    }
    
    private void onUnExpanded() {
        mExtraNeedLayout = true;
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                mAdapter.setWeekView();
            }
        });
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                adjustHeight(true);
            }
        });
    }
    
    public CalendarItem getCurrentDate() {
        return mAdapter.getCurrentDate();
    }
    
    private class CalendarPagerAdapter extends PagerAdapter {

        private HashMap<Object, CalendarViewEfficient> mViewMap = new HashMap<Object, CalendarViewEfficient>();
        private ArrayList<CalendarViewEfficient> mRecycleView = new ArrayList<CalendarViewEfficient>();
        private Data mCurrent;
        private Data mPre;
        private Data mNext;
        
        private boolean mIsWeekView;
        
        private WeekViewData mCurrentWeekViewDate;
        private WeekViewData mPreWeekViewDate;
        private WeekViewData mNextWeekViewDate;
        
        boolean mDataChanged;
        
        
        private OnCalendarDateSelectedListener mDateSelectedListener = new OnCalendarDateSelectedListener() {
            
            @Override
            public void onCalendarDateSelected(CalendarItem item) {
                setDate(item.year, item.month, item.date);
            }

            @Override
            public void onCalendarDateLongPressed(CalendarItem item) {
                
            }
            
        };
        
        
        public CalendarPagerAdapter() {
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, mActualYear);
            cal.set(Calendar.MONTH, mActualMonth);
            cal.set(Calendar.DATE, 1);
            mCurrent = new Data(mActualYear, mActualMonth, mActualDate);
            cal.add(Calendar.MONTH, -1);
            mPre = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            cal.add(Calendar.MONTH, 2);
            mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            mIsWeekView = false;
        }
        
        public void setDate(int year, int month, int date) {
            if (mOutListener != null) {
                mOutListener.onCalendarDateSelected(new CalendarItem(year, month, date));
            }
            if (mIsWeekView) {
                WeekViewData weekDate = new WeekViewData(year, month, date);
                if (mCurrentWeekViewDate != null && mCurrentWeekViewDate.getKey() == weekDate.getKey()) {
                    mCurrentWeekViewDate = weekDate;
                    Calendar cal = new GregorianCalendar(year, month, date);
                    cal.add(Calendar.DATE, -7);
                    mPreWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                    cal.add(Calendar.DATE, 14);
                    mNextWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                    mDataChanged = true;
                    finishUpdate(null);
                } else {
                    mCurrentWeekViewDate = weekDate;
                    Calendar cal = new GregorianCalendar(year, month, date);
                    cal.add(Calendar.DATE, -7);
                    mPreWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                    cal.add(Calendar.DATE, 14);
                    mNextWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                    notifyDataSetChanged();
                    mCalendarPager.setCurrentItem(1, false);
                }
            } else {
                if (year != mCurrent.year || month != mCurrent.month) {
                    mCurrent = new Data(year, month, date);
                    Calendar cal = new GregorianCalendar();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DATE, 1);
                    cal.add(Calendar.MONTH, -1);
                    mPre = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                    cal.add(Calendar.MONTH, 2);
                    mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                    notifyDataSetChanged();
                    mCalendarPager.setCurrentItem(1, false);
                } else {
                    mCurrent = new Data(mCurrent.year, mCurrent.month, date);
                    mDataChanged = true;
                    finishUpdate(null);
                }    
            }
        }
        
        /**
         * indicate if it is in month state or week state
         * @return true if it's in week state. Otherwise false
         */
        public boolean isWeekOrMonthView() {
            return mIsWeekView;
        }
        
        CalendarItem getCurrentDate() {
            if (mIsWeekView) {
                return new CalendarItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
            } else {
                return new CalendarItem(mCurrent.year, mCurrent.month, mCurrent.date);
            }
        }
        

        private class WeekViewData {
            final int year;
            final int month;
            final int date;
            final int weekOfMonth;
            
            WeekViewData(int year, int month, int date) {
                this.year = year;
                this.month = month;
                this.date = date;
                Calendar cal = new GregorianCalendar(year, month, date);
                weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
            }
            
            int getKey() {
                return year * 10000 + month * 100 + weekOfMonth;
            }
            
        }

        private class Data {
            final int year;
            final int month;
            final int date;

            Data(int year, int month, int date) {
                this.year = year;
                this.month = month;
                this.date = date;
            }
            
            int getKey() {
                return year * 100 + month;
            }
        }
        
        public void setWeekView() {
            if (mIsWeekView) {
                return;
            }
            Integer currentKey = mCurrent.getKey();
            CalendarViewEfficient currentView = mViewMap.get(currentKey);
            if (currentView != null) {
                CalendarItem currentDay = currentView.getDay();
                mCurrentWeekViewDate = new WeekViewData(currentDay.year, currentDay.month, currentDay.date);
                Calendar cal = new GregorianCalendar(currentDay.year, currentDay.month, currentDay.date);
                cal.add(Calendar.DATE, -7);
                mPreWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                cal.add(Calendar.DATE, 14);
                mNextWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                mIsWeekView = true;
                notifyDataSetChanged();
            }
            
        }
        
        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            mDataChanged = true;
        }


        public void setMonthView() {
            if (!mIsWeekView) {
                return;
            }
            mIsWeekView = false;
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, mCurrentWeekViewDate.year);
            cal.set(Calendar.MONTH, mCurrentWeekViewDate.month);
            cal.set(Calendar.DATE, 1);
            cal.add(Calendar.MONTH, -1);
            mCurrent = new Data(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
            mPre = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            cal.add(Calendar.MONTH, 2);
            mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            notifyDataSetChanged();
        }

        /**
         * set current month to next month
         */
        public void increment() {
            if (mIsWeekView) {
                mPreWeekViewDate = new WeekViewData(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                Calendar cal = new GregorianCalendar(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                cal.add(Calendar.DATE, 7);
                mCurrentWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                if (mOutListener != null) {
                    CalendarItem item = new CalendarItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    mOutListener.onCalendarDateSelected(item);
                }
                cal.add(Calendar.DATE, 7);
                mNextWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            } else {
                mPre = new Data(mCurrent.year, mCurrent.month, 1);
                Calendar cal = new GregorianCalendar();
                cal.set(Calendar.YEAR, mCurrent.year);
                cal.set(Calendar.MONTH, mCurrent.month);
                cal.set(Calendar.DATE, 1);
                cal.add(Calendar.MONTH, 1);
                if (cal.getActualMaximum(Calendar.DATE) < mCurrent.date) {
                    mCurrent = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                } else {
                    mCurrent = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), mCurrent.date);
                }
                if (mOutListener != null) {
                    CalendarItem item = new CalendarItem(mCurrent.year, mCurrent.month, mCurrent.date);
                    mOutListener.onCalendarDateSelected(item);
                }
                cal.add(Calendar.MONTH, 1);
                mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            }
            
            notifyDataSetChanged();
        }

        /**
         * set current month to previous month
         */
        public void decrement() {
            if (mIsWeekView) {
                mNextWeekViewDate = new WeekViewData(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month
                        , mCurrentWeekViewDate.date);
                Calendar cal = new GregorianCalendar(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, 
                        mCurrentWeekViewDate.date);
                cal.add(Calendar.DATE, -7);
                mCurrentWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 
                        cal.get(Calendar.DATE));
                if (mOutListener != null) {
                    CalendarItem item = new CalendarItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    mOutListener.onCalendarDateSelected(item);
                }
                cal.add(Calendar.DATE, -7);
                mPreWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 
                        cal.get(Calendar.DATE));
            } else {
                mNext = new Data(mCurrent.year, mCurrent.month, 1);
                Calendar cal = new GregorianCalendar();
                cal.set(Calendar.YEAR, mCurrent.year);
                cal.set(Calendar.MONTH, mCurrent.month);
                cal.set(Calendar.DATE, 1);
                cal.add(Calendar.MONTH, -1);
                if (cal.getActualMaximum(Calendar.DATE) < mCurrent.date) {
                    mCurrent = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                } else {
                    mCurrent = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), mCurrent.date);
                }
                if (mOutListener != null) {
                    CalendarItem item = new CalendarItem(mCurrent.year, mCurrent.month, mCurrent.date);
                    mOutListener.onCalendarDateSelected(item);
                }
                cal.add(Calendar.MONTH, -1);
                mPre = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1); 
            }
            
            notifyDataSetChanged();
        }
        
        

        @Override
        public int getCount() {
            return 3;
        }
        
        

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return mViewMap.get(arg1) == arg0;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            CalendarViewEfficient view = mViewMap.get(object);
            view.resetData();
            container.removeView(mViewMap.get(object));
            mViewMap.remove(object);
        }

        @Override
        public int getItemPosition(Object object) {
            Integer value = (Integer) object;
            int yearAndMonth = value.intValue();
            if (mIsWeekView) {
                if (yearAndMonth == mCurrentWeekViewDate.getKey()) {
                    return 1;
                } else if (yearAndMonth == mPreWeekViewDate.getKey()) {
                    return 0;
                } else if (yearAndMonth == mNextWeekViewDate.getKey()) {
                    return 2;
                } else {
                    return POSITION_NONE;
                }
            } else {
                if (yearAndMonth == mCurrent.getKey()) {
                    return 1;
                } else if (yearAndMonth == mPre.getKey()) {
                    return 0;
                } else if (yearAndMonth == mNext.getKey()) {
                    return 2;
                } else {
                    return POSITION_NONE;
                }
            }
            
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Integer key = null;
            if (mIsWeekView) {
                switch (position) {
                case 0:
                    key = mPreWeekViewDate.getKey();
                    break;
                case 1:
                    key = mCurrentWeekViewDate.getKey();
                    break;
                case 2:
                    key = mNextWeekViewDate.getKey();
                    break;
                default:
                    throw new IllegalArgumentException(
                            "position should be in [0, getCount)");
                }
            } else {
                switch (position) {
                case 0:
                    key = mPre.getKey();
                    break;
                case 1:
                    key = mCurrent.getKey();
                    break;
                case 2:
                    key = mNext.getKey();
                    break;
                default:
                    throw new IllegalArgumentException(
                            "position should be in [0, getCount)");
                }
            }
            
            boolean notUsedFound = false;
            for (int i = 0; i < mRecycleView.size(); i++) {
                CalendarViewEfficient item = mRecycleView.get(i);
                int index = container.indexOfChild(item);
                if (index == -1) {
                    notUsedFound = true;
                    container.addView(item, LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT);
                    mViewMap.put(key, item);
                    break;
                }
            }
            
            if (!notUsedFound) {
                CalendarViewEfficient item = new CalendarViewEfficient(container.getContext());
                item.setOnDateSelectedListener(mDateSelectedListener);
                container.addView(item, LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT);
                mViewMap.put(key, item);
                mRecycleView.add(item);
            }
            mDataChanged = true;
            return key;
        }
        
        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);
            if (!mDataChanged) {
                return;
            }
            mDataChanged = false;
            if (mIsWeekView) {
                Integer middleKey = mCurrentWeekViewDate.getKey();
                CalendarViewEfficient middelView = mViewMap.get(middleKey);
                if (middelView != null) {
                    int weekRow = 0;
                    int oldRow = middelView.getRowIndexOfDay();
                    middelView.setMonthView(mCurrentWeekViewDate.year,
                            mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    middelView.setEvents(mEvents);
                    weekRow = middelView.getRowIndexOfDay();
                    if (oldRow == -1 || (oldRow != -1 && weekRow != oldRow)) {
                        mResizableContainer.getLayoutParams().height = (weekRow + 1) * mRowHeight;
                        mResizableContainer.requestLayout();
                        mScrollableContainer.scrollTo(0, weekRow * mRowHeight);
                        mFocusedRow = weekRow;
                    }
                    Integer leftKey = mPreWeekViewDate.getKey();
                    CalendarViewEfficient leftView = mViewMap.get(leftKey);
                    if (leftView != null) {
                        leftView.setWeekView(mPreWeekViewDate.year, 
                                mPreWeekViewDate.month, mPreWeekViewDate.date, weekRow);
                        leftView.setEvents(mEvents);
                    }
                        
                    Integer rightKey = mNextWeekViewDate.getKey();
                    CalendarViewEfficient rightView = mViewMap.get(rightKey);
                    if (rightView != null) {
                        rightView.setWeekView(mNextWeekViewDate.year, mNextWeekViewDate.month, mNextWeekViewDate.date, weekRow);  
                        rightView.setEvents(mEvents);
                    }
                }
                
            } else {
                Integer currentKey = mCurrent.getKey();
                CalendarViewEfficient currentView = mViewMap.get(currentKey);
                if (currentView != null) {
                    currentView.setMonthView(mCurrent.year, mCurrent.month, mCurrent.date);
                    currentView.setEvents(mEvents);
                    mFocusedRow = currentView.getRowIndexOfDay();
                }
                Integer preKey = mPre.getKey();
                CalendarViewEfficient preView = mViewMap.get(preKey);
                if (preView != null) {
                    preView.setMonthView(mPre.year, mPre.month, 0);
                    preView.setEvents(mEvents);
                }
                Integer nextKey = mNext.getKey();
                CalendarViewEfficient nextView = mViewMap.get(nextKey);
                if (nextView != null) {
                    nextView.setMonthView(mNext.year, mNext.month, 0);
                    nextView.setEvents(mEvents);
                }    
            }
            
        }

    }
    
    
    
    private OnPageChangeListener mPageChangeListener = new OnPageChangeListener() {
        
        @Override
        public void onPageSelected(int position) {
            
        }
        
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            
        }
        
        @Override
        public void onPageScrollStateChanged(int state) {
            mViewPagerScrollState = state;
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                mViewPagerScrolling = false;
                int current = mCalendarPager.getCurrentItem();
                if (current == 0) {
                    mHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mAdapter.decrement();
                        }
                    });
                    
                } else if (current == 2) {
                    mHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mAdapter.increment();
                        }
                    });
                }
            } else {
                mViewPagerScrolling = true;
            }
        }
    };
    
}
