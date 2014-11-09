package com.junhzhan.cal.widget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cal.R;

/**
 * class of calendar widget which contains title bar and calendar view
 * @author junhao.zhang
 *
 */
public abstract class CalendarWidget<T> extends LinearLayout {
    
    private static final String TAG = "CalendarWidget";
    
    private static final String FONT_NAME = "Helvetica.ttf";
    
    private int mActualYear;
    private int mActualMonth;
    private int mActualDate;
    
    
    private ScrollContainer mScrollableContainer;
    private View mResizableContainer;
    private ViewPager mCalendarPager;
    
    private TextView mTitle;
    
    private CalendarPagerAdapter mAdapter;
    
    private Handler mHandler = new Handler();
    private int mRowHeight;
    private int mViewPagerScrollState;
    
    private int mFocusedRow;
    
    private FrameLayout mTitleContainer;
    private int mTitleYear;
    private int mTitleMonth;
    private int mTitleDate;
    
    private Drawable mSelectionDrawable;
    
    private OnCalendarDateSelectedListener<T> mOutListener;
    
    
    private HashMap<CalendarDayItem, List<T>> mEventsMap = new HashMap<CalendarDayItem, List<T>>();
    
    /**
     * 一、二、三、四、五、六、七、八、九、十，十一，十二
     */
    public static final String[] MONTH_CHARACTERS = {"\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d",
        "\u4e03", "\u516b", "\u4e5d", "\u5341", "\u5341\u4e00", "\u5341\u4e8c"};
    
    private static final String[] DAY_OF_WEEK_TITLE = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
    
    /**
     * constructor of CalendarView
     * @param ctx context
     * @param year initial year
     * @param month initial month, it should be in range between {@link Calendar#JANUARY} and {@link Calendar#DECEMBER} 
     * @param date initial date of month
     */
    public CalendarWidget(Context ctx) {
        super(ctx);
        init(ctx);
    }
    
    
    
    
    
    
    public CalendarWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }






    public CalendarWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }




    private void init(Context ctx) {
        setOrientation(VERTICAL);
        Calendar cal = Calendar.getInstance();
        mActualYear = cal.get(Calendar.YEAR);
        mActualMonth = cal.get(Calendar.MONTH);
        mActualDate = cal.get(Calendar.DATE);
        DisplayMetrics metric = ctx.getResources().getDisplayMetrics();
        int calViewHeight = metric.widthPixels / CalendarView.COLUMN_COUNT * CalendarView.ROW_COUNT;
        mRowHeight = metric.widthPixels / CalendarView.COLUMN_COUNT;
        RelativeLayout bar = new RelativeLayout(ctx);
        bar.setBackgroundColor(getResources().getColor(android.R.color.white));
        bar.setPadding(getResources().getDimensionPixelOffset(R.dimen.calendar_title_padding_left), 0, 0, 0);
        mTitle = new TextView(ctx);
        mTitle.setTextColor(0xFFFF0000);
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.calendar_title_text_size));
        mTitle.setText(MONTH_CHARACTERS[mActualMonth] + ctx.getString(R.string.chinese_month) + " " + mActualYear);
        mTitle.setTypeface(null, Typeface.BOLD);
        mTitleYear = mActualYear;
        mTitleMonth = mActualMonth;
        mTitleDate = mActualDate;
        FrameLayout titleContainer = new FrameLayout(ctx);
        mTitleContainer = titleContainer;
        titleContainer.addView(mTitle, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layout.addRule(RelativeLayout.CENTER_VERTICAL);
        layout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        bar.addView(titleContainer, layout);
        LinearLayout dayTitleContainer = new LinearLayout(ctx);
        dayTitleContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
        Typeface typeface = Typeface.createFromAsset(getResources().getAssets(), FONT_NAME);
        for (int i = 0;i < DAY_OF_WEEK_TITLE.length;i++) {
            TextView dayTitleView = new TextView(ctx);
            dayTitleView.setTypeface(typeface, Typeface.BOLD);
            dayTitleView.setText(DAY_OF_WEEK_TITLE[i]);
            dayTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.calendar_day_of_week_title_text_size));
            dayTitleView.setTextColor(0xFF000000);
            dayTitleView.setGravity(Gravity.CENTER);
            LayoutParams dayTitleLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            dayTitleContainer.addView(dayTitleView, dayTitleLayout);
        }
        addView(bar, LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.calendar_title_height));
        addView(dayTitleContainer, LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.calendar_day_of_week_title_height));
        ScrollContainer scrollContainer = new ScrollContainer(ctx);
        scrollContainer.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
        addView(scrollContainer, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mScrollableContainer = scrollContainer;
        LinearLayout resizeContainer = new LinearLayout(ctx);
        resizeContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
        scrollContainer.addView(resizeContainer, LayoutParams.MATCH_PARENT, calViewHeight);
        mCalendarPager = new ViewPager(ctx);
        mAdapter = new CalendarPagerAdapter();
        mCalendarPager.setAdapter(mAdapter);
        mCalendarPager.setCurrentItem(1);
        mCalendarPager.setOnPageChangeListener(mPageChangeListener);
        resizeContainer.addView(mCalendarPager, LayoutParams.MATCH_PARENT, calViewHeight);
        mResizableContainer = resizeContainer;
    }
    
    public void setExtraView(View view, int width, int height) {
        if (height == LayoutParams.MATCH_PARENT && getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            throw new IllegalStateException("extra view is match parent but CalendarWidget is wrap content");
        }
        if (height == LayoutParams.MATCH_PARENT) {
            mScrollableContainer.getLayoutParams().height = LayoutParams.MATCH_PARENT;
        }
        ExtraContainer container = new ExtraContainer(getContext(), view);
        mScrollableContainer.addView(container, width, height);
    }


    public void setOnCalendarDateSelectedListener(OnCalendarDateSelectedListener<T> l) {
        mOutListener = l;
    }
    
    /**
     * add event to specified date
     * @param event
     * @param year
     * @param month
     * @param date
     */
    public void addEvent(T event, int year, int month, int date) {
        CalendarDayItem item = new CalendarDayItem(year, month, date);
        List<T> events = mEventsMap.get(item);
        if (events == null) {
            events = new ArrayList<T>();
            mEventsMap.put(item, events);
        }
        events.add(event);
    }
    
    /**
     * add event to current selected date
     * @param event
     */
    public void addEvent(T event) {
        CalendarDayItem date = mAdapter.getCurrentDate();
        addEvent(event, date.year, date.month, date.date);
    }
    
    /**
     * remove the event from current selected date
     * @param event
     * @return true if this operation succeeds, otherwise false.
     */
    public boolean removeEvent(T event) {
        CalendarDayItem date = mAdapter.getCurrentDate();
        return removeEvent(event, date.year, date.month, date.date);
    }
    
    /**
     * remove the event from specified date
     * @param event
     * @param year
     * @param month
     * @param date
     * @return true if this operation succeeds, otherwise false.
     */
    public boolean removeEvent(T event, int year, int month, int date) {
        CalendarDayItem item = new CalendarDayItem(year, month, date);
        List<T> events = mEventsMap.get(item);
        if (events != null) {
            return events.remove(event);
        }
        return false;
    }
    
    
    /**
     * get events for specified date
     * @param year
     * @param month
     * @param date
     * @return events list. It will return null if no events exist for specified date
     */
    public List<T> getEvents(int year, int month, int date) {
        CalendarDayItem item = new CalendarDayItem(year, month, date);
        return mEventsMap.get(item);
    }
    
    
    /**
     * update calendar view manually
     */
    public void updateView() {
        mAdapter.mDataChanged = true;
        mAdapter.finishUpdate(null);
    }
    
    /**
     * get currently selected date
     * @return
     */
    public CalendarDayItem getSelectedDate() {
        return mAdapter.getCurrentDate();
    }
    
    
    private class ExtraContainer extends FrameLayout {
        public ExtraContainer(Context ctx, View content) {
            super(ctx);
            addView(content, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (mExtraNeedLayout) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right,
                int bottom) {
            Log.e("ExtraContainer", "onLayout");
            if (mExtraNeedLayout) {
                Log.e("ExtraContainer", "child layout");
                super.onLayout(changed, left, top, right, bottom);
            }
        }
        
        
    }
    
    private boolean mExtraNeedLayout = true;
    
    private class ScrollContainer extends LinearLayout {

        
        private float mDownPointY;
        private float mDownPointX;
        private float mLastPointY;
        
        private boolean mProcessActionDown;
        
        private float mDeltaY;
        private float mAutoScrollDelta;
        private int mTargetRow;
        public ScrollContainer(Context context) {
            super(context);
            setOrientation(VERTICAL);
        }
        
        
        

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (getChildCount() > 2) {
                throw new IllegalStateException("child count is not correct");
            }
            
            View extraChild = getChildAt(1);
            if (extraChild != null && extraChild.getLayoutParams().height == LayoutParams.MATCH_PARENT) {
                extraChild.getLayoutParams().height = extraChild.getMeasuredHeight();
                requestLayout();
            }
            
            if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
                int totalHeight = 0;
                for (int i = 0;i < getChildCount();i++) {
                    View child = getChildAt(i);
                    totalHeight += child.getMeasuredHeight();
                }
                getLayoutParams().height = totalHeight;
                requestLayout();
            }
        }
        
        

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            boolean intercept = false;
            switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownPointY = ev.getY();
                mDownPointX = ev.getX();
                mProcessActionDown = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                float totalDeltaY = Math.abs(ev.getY() - mDownPointY);
                float totalDeltaX = Math.abs(ev.getX() - mDownPointX);
                if (mViewPagerScrollState == ViewPager.SCROLL_STATE_IDLE && totalDeltaY > 50 && totalDeltaX < totalDeltaY) {
                    mTargetRow = mFocusedRow;
                    intercept = true;
                }
            }
            mLastPointY = ev.getY();
            return intercept;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            String log = null;
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
                mDeltaY = event.getY() - mLastPointY;
                scroll(mDeltaY, false);
                break;
            case MotionEvent.ACTION_UP:
                log = "ACTION_UP";
                if (mProcessActionDown) {
                    break;
                }
                if (mDeltaY > 0) {
                    mAutoScrollDelta = 15;
                } else {
                    mAutoScrollDelta = -15;
                }
                scroll(mAutoScrollDelta, true);
                break;
            }
            Log.d("onTouchEvent", log);
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
                Log.e(TAG, "auto scroll deltay " + deltaY);
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
                    if (mResizableContainer.getHeight() < CalendarView.ROW_COUNT * mRowHeight) {
                        if ((mResizableContainer.getHeight() + deltaY) >= CalendarView.ROW_COUNT * mRowHeight) {
                            mResizableContainer.getLayoutParams().height = CalendarView.ROW_COUNT * mRowHeight;
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
            } else {
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
                    Log.e(TAG, "resize container height " + mResizableContainer.getHeight());
                    if (mResizableContainer.getHeight() > (mTargetRow + 1) * mRowHeight) {
                        if ((mResizableContainer.getHeight() + (int)deltaY) <= (mTargetRow + 1) * mRowHeight) {
                            mResizableContainer.getLayoutParams().height = (mTargetRow + 1) * mRowHeight;
                            mResizableContainer.requestLayout();
                            Log.e(TAG, "unexpand 1");
                            onUnExpanded();
                        } else {
                            mResizableContainer.getLayoutParams().height = mResizableContainer.getHeight() + (int)deltaY;
                            mResizableContainer.requestLayout();
                            Log.e(TAG, "unexpand 2");
                            if (auto) {
                                mHandler.post(mRunnable);
                            }
                        }
                    }
                    Log.e(TAG, "unexpand 3");
                }
            }
        }
        
        
        
        
    }
    
    /**
     * change to week view. If it is already in week status, this method will take no effects.
     */
    public void changeToWeekView() {
        if (mAdapter.mIsWeekView) {
            return;
        }
        mScrollableContainer.mTargetRow = mFocusedRow;
        mScrollableContainer.mAutoScrollDelta = -15;
        mScrollableContainer.scroll(mScrollableContainer.mAutoScrollDelta, true);
    }
    
    /**
     * change to month view. If it's already in month status, this method will take no effects.
     */
    public void changeToMonthView() {
        if (!mAdapter.mIsWeekView) {
            return;
        }
        mScrollableContainer.mTargetRow = mFocusedRow;
        mScrollableContainer.mAutoScrollDelta = 15;
        mScrollableContainer.scroll(mScrollableContainer.mAutoScrollDelta, true);
    }
    
    private void onExpanded() {
        mExtraNeedLayout = true;
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                mAdapter.setMonthView();
            }
        });
    }
    
    private void onUnExpanded() {
        Log.e(TAG, "onUnExpanded");
        mExtraNeedLayout = true;
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                mAdapter.setWeekView();
            }
        });
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
                int current = mCalendarPager.getCurrentItem();
                if (current == 0) {
                    mHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            Log.e(TAG, "change current month");
                            mAdapter.decrement();
                        }
                    });
                    
                } else if (current == 2) {
                    mHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            Log.e(TAG, "change current month");
                            mAdapter.increment();
                        }
                    });
                }
            }
        }
    };
    
    boolean mIsAnimating = false;
    private void setTitle(int year, int month, int date, boolean monthView) {
        if (true) {
            if (year != mTitleYear || month != mTitleMonth) {
                TextView title = new TextView(getContext());
                title.setTextColor(0xFFFF0000);
                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.calendar_title_text_size));
                title.setText(MONTH_CHARACTERS[month] + getContext().getString(R.string.chinese_month) + " " + year);
                title.setTypeface(null, Typeface.BOLD);
                Log.e(TAG, "child count is " + mTitleContainer.getChildCount());
                final View oldTitle = mTitleContainer.getChildAt(0);
                if (mIsAnimating) {
                    mTitleContainer.removeAllViews();
                } else {
                    Animation exitAnimatioin = AnimationUtils.loadAnimation(getContext(), R.anim.left_out_anim);
                    exitAnimatioin.setFillAfter(true);
                    oldTitle.startAnimation(exitAnimatioin);
                    exitAnimatioin.setAnimationListener(new AnimationListener() {
                        
                        @Override
                        public void onAnimationStart(Animation animation) {
                            
                        }
                        
                        @Override
                        public void onAnimationRepeat(Animation animation) {
                            
                        }
                        
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            Log.e(TAG, "onAnimationEnd");
                            mIsAnimating = false;
                            mHandler.post(new Runnable() {
                                
                                @Override
                                public void run() {
                                    int count = mTitleContainer.getChildCount() - 1;
                                    for (int i = 0;i < count;i++) {
                                        mTitleContainer.removeViewAt(0);
                                    }
                                    
                                }
                            });
                        }
                    });
                    mIsAnimating = true;
                }
                mTitleContainer.addView(title, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                title.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.right_in_anim));
                
                mTitleYear = year;
                mTitleMonth = month;
            }
        }
    }
    
    
    private class CalendarPagerAdapter extends PagerAdapter {

        private HashMap<Object, CalendarView> mViewMap = new HashMap<Object, CalendarView>();
        private ArrayList<CalendarView> mRecycleView = new ArrayList<CalendarView>();
        private Data mCurrent;
        private Data mPre;
        private Data mNext;
        
        private boolean mIsWeekView;
        
        private WeekViewData mCurrentWeekViewDate;
        private WeekViewData mPreWeekViewDate;
        private WeekViewData mNextWeekViewDate;
        
        private boolean mDataChanged;
        
        private class CustomCalendarView extends CalendarView {

            CustomCalendarView(Context context) {
                super(context);
            }

            @Override
            protected View onCreateDateView(CalendarDayItem date,
                    CalendarDayItem currentDate, boolean weekOrMonthView) {
                return CalendarWidget.this.onCreateDateView(date, currentDate, weekOrMonthView);
            }

            @Override
            protected void onDateViewInvalidate(View dateView,
                    CalendarDayItem date, CalendarDayItem currentDate,
                    boolean weekOrMonthView) {
                CalendarWidget.this.onDateViewInvalidate(dateView, date, currentDate, weekOrMonthView);
            }

            @Override
            protected void onDateSelected(CalendarDayItem item) {
                if (mIsWeekView) {
                    mCurrentWeekViewDate = new WeekViewData(item.year, item.month, item.date);
                    Calendar cal = new GregorianCalendar(item.year, item.month, item.date);
                    cal.add(Calendar.DATE, -7);
                    mPreWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                    cal.add(Calendar.DATE, 14);
                    mNextWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                    mIsWeekView = true;
                    notifyDataSetChanged();
                    finishUpdate(null);
                } else {
                    if (item.year != mCurrent.year || item.month != mCurrent.month) {
                        mCurrent = new Data(item.year, item.month, item.date);
                        Calendar cal = new GregorianCalendar();
                        cal.set(Calendar.YEAR, item.year);
                        cal.set(Calendar.MONTH, item.month);
                        cal.set(Calendar.DATE, 1);
                        cal.add(Calendar.MONTH, -1);
                        mPre = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                        cal.add(Calendar.MONTH, 2);
                        mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                        notifyDataSetChanged();
                        mCalendarPager.setCurrentItem(1, false);
                    } else {
                        mCurrent = new Data(mCurrent.year, mCurrent.month, item.date);
                        mDataChanged = true;
                        finishUpdate(null);
                    }    
                }
                if (mOutListener != null) {
                    mOutListener.onCalendarDateSelected(item, mEventsMap.get(item));
                }
            }
            
            
            
            
        }
        
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
        
        CalendarDayItem getCurrentDate() {
            if (mIsWeekView) {
                return new CalendarDayItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
            } else {
                return new CalendarDayItem(mCurrent.year, mCurrent.month, mCurrent.date);
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
            CalendarView currentView = mViewMap.get(currentKey);
            if (currentView != null) {
                CalendarDayItem currentDay = currentView.getDay();
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
                    CalendarDayItem item = new CalendarDayItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    mOutListener.onCalendarDateSelected(item, mEventsMap.get(item));
                }
                cal.add(Calendar.DATE, 7);
                mNextWeekViewDate = new WeekViewData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            } else {
                mPre = new Data(mCurrent.year, mCurrent.month, 1);
                Calendar cal = new GregorianCalendar();
                cal.set(Calendar.YEAR, mCurrent.year);
                cal.set(Calendar.MONTH, mCurrent.month);
                cal.set(Calendar.DATE, 1);
                Log.e(TAG, String.format("cal year %d month %d date %d",  cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)));
                cal.add(Calendar.MONTH, 1);
                if (cal.getActualMaximum(Calendar.DATE) < mCurrent.date) {
                    mCurrent = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                } else {
                    mCurrent = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), mCurrent.date);
                }
                if (mOutListener != null) {
                    CalendarDayItem item = new CalendarDayItem(mCurrent.year, mCurrent.month, mCurrent.date);
                    mOutListener.onCalendarDateSelected(item, mEventsMap.get(item));
                }
                cal.add(Calendar.MONTH, 1);
                mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                Log.e(TAG, String.format("increment current key %d pre key %d next key %d", mCurrent.getKey(), mPre.getKey(), mNext.getKey()));
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
                    CalendarDayItem item = new CalendarDayItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    mOutListener.onCalendarDateSelected(item, mEventsMap.get(item));
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
                    CalendarDayItem item = new CalendarDayItem(mCurrent.year, mCurrent.month, mCurrent.date);
                    mOutListener.onCalendarDateSelected(item, mEventsMap.get(item));
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
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return MONTH_CHARACTERS[mPre.month] + getContext().getString(R.string.chinese_month) + mPre.year;
            case 1:
                return MONTH_CHARACTERS[mCurrent.month] + getContext().getString(R.string.chinese_month) + mCurrent.year;
            case 2:
                return MONTH_CHARACTERS[mNext.month] + getContext().getString(R.string.chinese_month) + mNext.year;
            default:
                break;
            }
            return null;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.e(TAG, "destroyItem " + object);
            CalendarView view = mViewMap.get(object);
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
                Log.e(TAG, String.format("current key %d, pre key %d, next key %d", mCurrent.getKey(), mPre.getKey(), mNext.getKey()));
                if (yearAndMonth == mCurrent.getKey()) {
                    Log.e(TAG, String.format("year and month %d new position %d",
                            yearAndMonth, 1));
                    return 1;
                } else if (yearAndMonth == mPre.getKey()) {
                    Log.e(TAG, String.format("year and month %d new position %d",
                            yearAndMonth, 0));
                    return 0;
                } else if (yearAndMonth == mNext.getKey()) {
                    Log.e(TAG, String.format("year and month %d new position %d",
                            yearAndMonth, 2));
                    return 2;
                } else {
                    Log.e(TAG, String.format("year and month %d new position %d",
                            yearAndMonth, POSITION_NONE));
                    return POSITION_NONE;
                }
            }
            
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.e("TestPagerAdapter",
                    String.format("instantiate item pos is %d", position));
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
                CalendarView item = mRecycleView.get(i);
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
                CalendarView item = new CustomCalendarView(container.getContext());
                item.setSelectionDrawable(mSelectionDrawable);
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
                CalendarView middelView = mViewMap.get(middleKey);
                if (middelView != null) {
                    int weekRow = 0;
                    int oldRow = middelView.getRowIndexOfDay();
                    Log.e(TAG, "old row index is " + oldRow);
                    middelView.setMonthView(mCurrentWeekViewDate.year,
                            mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    
                    setTitle(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date, false);
                    weekRow = middelView.getRowIndexOfDay();
                    Log.e(TAG, "new week row index is " + weekRow);
                    if (oldRow == -1 || (oldRow != -1 && weekRow != oldRow)) {
                        mResizableContainer.getLayoutParams().height = (weekRow + 1) * mRowHeight;
                        mResizableContainer.requestLayout();
                        mScrollableContainer.scrollTo(0, weekRow * mRowHeight);
                        mFocusedRow = weekRow;
                    }
                    Integer leftKey = mPreWeekViewDate.getKey();
                    CalendarView leftView = mViewMap.get(leftKey);
                    if (leftView != null) {
                        leftView.setWeekView(mPreWeekViewDate.year, 
                                mPreWeekViewDate.month, mPreWeekViewDate.date, weekRow);
                    }
                        
                    Integer rightKey = mNextWeekViewDate.getKey();
                    CalendarView rightView = mViewMap.get(rightKey);
                    if (rightView != null) {
                        rightView.setWeekView(mNextWeekViewDate.year, mNextWeekViewDate.month, mNextWeekViewDate.date, weekRow);    
                    }
                }
                
            } else {
                Integer currentKey = mCurrent.getKey();
                CalendarView currentView = mViewMap.get(currentKey);
                if (currentView != null) {
                    currentView.setMonthView(mCurrent.year, mCurrent.month, mCurrent.date);
                    setTitle(mCurrent.year, mCurrent.month
                            , mCurrent.date, true);
                    mFocusedRow = currentView.getRowIndexOfDay();
                }
                Integer preKey = mPre.getKey();
                CalendarView preView = mViewMap.get(preKey);
                if (preView != null) {
                    preView.setMonthView(mPre.year, mPre.month, 0);
                }
                Integer nextKey = mNext.getKey();
                CalendarView nextView = mViewMap.get(nextKey);
                if (nextView != null) {
                    nextView.setMonthView(mNext.year, mNext.month, 0);
                }    
            }
            
        }

    }
    
    /**
     * create a new view to hold calendar date.
     * @param date date corresponding to the created view
     * @param selectedDate current selected date
     * @param weekOrMonthView indicate if calendar is in week status or month status
     * @return
     */
    protected abstract View onCreateDateView(CalendarDayItem date,
            CalendarDayItem selectedDate, boolean weekOrMonthView);

    /**
     * called when the view associated with date needs update
     * @param dateView view associated with calendar date
     * @param date calendar date
     * @param selectedDate current selected date
     * @param weekOrMonthView indicate if calendar is in week status or month status
     */
    protected abstract void onDateViewInvalidate(View dateView,
            CalendarDayItem date, CalendarDayItem selectedDate,
            boolean weekOrMonthView);
    
    
    
    
    
    
    
}
