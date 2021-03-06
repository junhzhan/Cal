package com.junhzhan.cal.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.cal.R;
import com.junhzhan.cal.Constant;
import com.junhzhan.cal.data.CalendarItem;
import com.junhzhan.cal.data.CustomEvent;
import com.junhzhan.cal.data.Event;
import com.junhzhan.cal.data.EventItem;
import com.junhzhan.cal.data.SyncEvent;

public class CalendarWidgetNew extends LinearLayout {
    
    private static final String TAG = "CalendarWidgetNew";
    
    private static final String[] DAY_OF_WEEK_TITLE = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
    
    private static final String FONT_NAME = "Helvetica.ttf";
    
    private View mResizableContainer;
    private ScrollContainer mScrollableContainer;
    
    
    private int mActualMonth;
    private int mActualYear;
    private int mActualDate;
    
    private int mRowHeight;
    private int mViewPagerScrollState;
    private int mFocusedRow;
    
    private OnCalendarDateSelectedListener mOutListener;
    private OnEventListScrollListener mEventListScrollListener;
    
    private ViewPager mCalendarPager;
    
    private Handler mHandler = new Handler();
    
    private CalendarPagerAdapter mAdapter;
    
    private boolean mExtraNeedLayout = true;
    
    private EventAdapter mEventAdapter;
    
    private EventListView mList;
    
    private int mListPositionDelta;
    
    private HashMap<CalendarItem, EventItem> mEvents = new HashMap<CalendarItem, EventItem>();
    
    private HashMap<CalendarItem, Set<Integer>> mHasExtraItems = new HashMap<CalendarItem, Set<Integer>>();
    

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
        final EventListView list = new EventListView(getContext());
        EventAdapter eventAdatper = new EventAdapter();
        list.setAdapter(eventAdatper);
        list.setDividerHeight(0);
        list.setOnScrollListener(mListScrollListener);
        mList = list;
        mEventAdapter = eventAdatper;
        final CalendarItem item = new CalendarItem(mActualYear, mActualMonth, mActualDate);
        mScrollableContainer.addView(list, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        list.setDate(item);
    }
    
    
    
    
    public void setOnCalendarDateSelectedListener(OnCalendarDateSelectedListener l) {
        mOutListener = l;
    }
    
    /**
     * add custom events to specified date
     * @param year
     * @param month
     * @param date
     * @param events
     */
    public void addCustomEvent(int year, int month, int date, List<CustomEvent> events) {
        CalendarItem item = new CalendarItem(year, month, date);
        checkDateValid(item);
        if (events == null) {
            return;
        }
        
        EventItem eventItem = mEvents.get(item);
        if (eventItem == null) {
            eventItem = new EventItem();
            mEvents.put(item, eventItem);
        }
        int firstVisiblePosition = mList.getFirstVisiblePosition();
        ItemInfo info = mEventAdapter.getItemInfo(firstVisiblePosition);
        if (info.date.hashCode() > item.hashCode()) {
            if (eventItem.getCustomEventCount() + eventItem.getSyncEventCount() > 0) {
                mListPositionDelta += events.size();
            } else {
                mListPositionDelta += events.size() - 1;
            }
        }
        
        eventItem.addCustomEvent(events);
    }
    
    /**
     * add sync event to specified date
     * @param year
     * @param month
     * @param date
     * @param events
     */
    public void addSyncEvent(int year, int month, int date, List<SyncEvent> events) {
        CalendarItem item = new CalendarItem(year, month, date);
        checkDateValid(item);
        if (events == null) {
            return;
        }
        
        EventItem eventItem = mEvents.get(item);
        if (eventItem == null) {
            eventItem = new EventItem();
            mEvents.put(item, eventItem);
        }
        int firstVisiblePosition = mList.getFirstVisiblePosition();
        ItemInfo info = mEventAdapter.getItemInfo(firstVisiblePosition);
        if (info.date.hashCode() > item.hashCode()) {
            if (eventItem.getCustomEventCount() + eventItem.getSyncEventCount() > 0) {
                mListPositionDelta += events.size();
            } else {
                mListPositionDelta += events.size() - 1;
            }
        }
        eventItem.addSyncEvent(events);
    }
    
    /**
     * notify data has been changed. Views reflecting the data will need to refresh themselves
     */
    public void notifyEventDataChanged() {
        mAdapter.mDataChanged = true;
        mAdapter.finishUpdate(null);
        mEventAdapter.notifyDataSetChanged();
    }
    
    /**
     * set current selected date to specified date
     * @param year
     * @param month
     * @param date
     */
    public void setDate(int year, int month, int date) {
        mAdapter.setDate(year, month, date);
        mList.setDate(new CalendarItem(year, month, date));
    }
    
    private void checkDateValid(CalendarItem item) {
        if (item.year > Constant.END_YEAR || item.year < Constant.START_YEAR) {
            throw new IllegalArgumentException();
        }
        if (item.month < Calendar.JANUARY || item.month > Calendar.DECEMBER) {
            throw new IllegalArgumentException();
        }
        Calendar cal = new GregorianCalendar(item.year, item.month, 1);
        if (item.date > cal.getActualMaximum(Calendar.DATE) || item.date < 1) {
            throw new IllegalArgumentException();
        }
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


    private class ScrollContainer extends LinearLayout {

        
        private float mDownPointY;
        private float mDownPointX;
        private float mLastPointY;
        
        private boolean mProcessActionDown;
        
        private float mDeltaY;
        private float mAutoScrollDelta;
        private int mTargetRow;
        
        private boolean mDownActionInList;
        
        public ScrollContainer(Context context) {
            super(context);
            setOrientation(VERTICAL);
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
                if (scrolledPositionX >= mList.getLeft() && scrolledPositionX <= mList.getRight()
                        && scrolledPositionY >= mList.getTop() && scrolledPositionY <= mList.getBottom()) {
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
                        // ListView will request disable intercept in some condition
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
                
                mDeltaY = event.getY() - mLastPointY;
                scroll(mDeltaY, false);
                break;
            case MotionEvent.ACTION_UP:
                log = "ACTION_UP";
                if (mProcessActionDown) {
                    break;
                }
                if (mResizableContainer.getHeight() - getScrollY() > mRowHeight * CalendarViewEfficient.ROW_COUNT / 2) {
                    mAutoScrollDelta = 15;
                } else {
                    mAutoScrollDelta = -15;
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
                Log.e(TAG, "auto scroll deltay " + deltaY);
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
                    if (mResizableContainer.getHeight() < CalendarViewEfficient.ROW_COUNT * mRowHeight) {
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
                    Log.e(TAG, "resize container height " + mResizableContainer.getHeight());
                    if (mResizableContainer.getHeight() >= (mTargetRow + 1) * mRowHeight) {
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
                mList.adjustHeight(false);
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
        mHandler.post(new Runnable() {
            
            @Override
            public void run() {
                mList.adjustHeight(true);
            }
        });
    }
    
    
    
    
    private OnScrollListener mListScrollListener = new OnScrollListener() {
        
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && !mAdapter.isWeekOrMonthView()) {
                mList.adjustHeight(false);
            }
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                if (mList != null && mList.getAdapter() != null) {
                    int firstVisibleItem = mList.getFirstVisiblePosition();
                    EventAdapter eventAdapter = (EventAdapter)mList.getAdapter();
                    CalendarItem scrolledDate = eventAdapter.getItemInfo(firstVisibleItem).date;
                    if (mAdapter != null) {
                        Log.e(TAG, String.format("scroll to year %d month %d date %d",  scrolledDate.year, scrolledDate.month,
                                scrolledDate.date));
                        mAdapter.setDate(scrolledDate.year, scrolledDate.month, scrolledDate.date);
                    }
                }
            }
        }
        
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
           ((EventListView)view).configureHeaderView(firstVisibleItem);
           if (mEventListScrollListener != null) {
               CalendarItem firstVisibleDate = mEventAdapter.getItemInfo(firstVisibleItem).date;
               CalendarItem lastVisibleDate = mEventAdapter.getItemInfo(visibleItemCount + totalItemCount - 1).date;
               mEventListScrollListener.onScrolled(firstVisibleDate, lastVisibleDate);
           }
            
        }
    };
    
    private class EventListView extends ListView {

        private boolean mFirstLayout = true;
        
        private TextView mHeaderView;
        private boolean mHeaderVisible;
        
        public EventListView(Context context) {
            super(context);
            TextView header = new TextView(context);
            header.setTextColor(getResources().getColor(R.color.event_listitem_header_textcolor));
            header.setBackgroundResource(R.drawable.event_listview_header_bg);
            header.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.event_listitem_header_height)));
            header.setPadding(getResources().getDimensionPixelOffset(R.dimen.event_listitem_header_paddingleft), 0, 0, 0);
            header.setGravity(Gravity.CENTER_VERTICAL);
            mHeaderView = header;
            
        }
        

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (mFirstLayout) {
                adjustHeight(false);
                mFirstLayout = false;
            }
            
        }
        
        
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
        }
        
        

        public void configureHeaderView(int firstVisibleItemPosition) {
            ListAdapter adapter = getAdapter();
            if (adapter == null) {
                return;
            }
            EventAdapter eventAdapter = (EventAdapter)adapter;
            ItemInfo info = eventAdapter.getItemInfo(firstVisibleItemPosition);
            CalendarItem date = info.date;
            Set<Integer> extraPositions = mHasExtraItems.get(date);
            if (extraPositions == null) {
                extraPositions = new HashSet<Integer>();
            }
            int eventCount = 0;
            EventItem eventItem = mEvents.get(date);
            if (eventItem == null) {
                eventCount = 1;
            } else {
                eventCount = eventItem.getCustomEventCount() + eventItem.getSyncEventCount();
            }
            int defaultListItemHeight = getResources().getDimensionPixelOffset(R.dimen.event_listitem_height);
            int scrolledItemHeight = 0;
            int totalHeight = 0;
            mHeaderView.setText(String.format("%d-%d-%d", date.year, date.month + 1, date.date));
            View firstView = getChildAt(0);
            if (firstView != null && firstView.getTop() <= 0) {
                int positionInDate = info.positionInDate;
                int extraHeight = getResources().getDimensionPixelOffset(R.dimen.event_listitem_extra_height);
                for (int i = 0;i < positionInDate;i++) {
                    scrolledItemHeight += extraPositions.contains(i) ? defaultListItemHeight + extraHeight: defaultListItemHeight;
                }
                totalHeight += scrolledItemHeight;
                for (int i = positionInDate; i < eventCount;i++) {
                    totalHeight += extraPositions.contains(i) ? defaultListItemHeight + extraHeight: defaultListItemHeight;
                }
                
                int headerHeight = getResources().getDimensionPixelOffset(R.dimen.event_listitem_header_height);
                if (positionInDate > 0) {
                    scrolledItemHeight += headerHeight;
                }
                totalHeight += headerHeight;
                
                mHeaderVisible = true;
                int top = (int)(((float)(scrolledItemHeight - firstView.getTop())) / totalHeight * mHeaderView.getMeasuredHeight());
                int bottom = -top + mHeaderView.getMeasuredHeight();
                mHeaderView.layout(getPaddingLeft(), -top, getPaddingLeft() + mHeaderView.getMeasuredWidth(), bottom);
            } else {
                mHeaderVisible = false;
            }
        }
        
        
        public void addExtraItem(ItemInfo itemInfo) {
            Set<Integer> positionsInDate = mHasExtraItems.get(itemInfo.date);
            if (positionsInDate == null) {
                positionsInDate = new HashSet<Integer>();
                mHasExtraItems.put(itemInfo.date, positionsInDate);
            }
            boolean success = positionsInDate.add(itemInfo.positionInDate);
            if (success) {
                configureHeaderView(mList.getFirstVisiblePosition());
            }
        }
        
        public void removeExtraItem(ItemInfo itemInfo) {
            Set<Integer> positionsInDate = mHasExtraItems.get(itemInfo.date);
            if (positionsInDate != null) {
                boolean success = positionsInDate.remove(itemInfo.positionInDate);
                if (success) {
                    if (positionsInDate.size() == 0) {
                        mHasExtraItems.remove(itemInfo.date);
                    }
                    configureHeaderView(mList.getFirstVisiblePosition());
                }
            }
        }
        
        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (mHeaderVisible) {
                drawChild(canvas, mHeaderView, getDrawingTime());
            }
        }


        /**
         * adjust height of this ListView according to its items or just fill its container
         * @param fullScreen true to fill its container, false to calculate its height according to its items
         */
        public void adjustHeight(boolean fillParent) {
            int scrollContainerHeight = mScrollableContainer.getHeight() == 0 ? mScrollableContainer.getMeasuredHeight() : 
                mScrollableContainer.getHeight();
            int resizeContainerHeight = mResizableContainer.getHeight() == 0 ? mResizableContainer.getMeasuredHeight() :
                mResizableContainer.getHeight();
            int listVisibleHeight = scrollContainerHeight - resizeContainerHeight - mScrollableContainer.getScrollY();
            if (fillParent) {
                Log.d(TAG, String.format("listview fill scroll container %d resize container %d scroll y %d", scrollContainerHeight, 
                        resizeContainerHeight, getScrollY()));
                getLayoutParams().height = scrollContainerHeight - (resizeContainerHeight - mScrollableContainer.getScrollY());
                requestLayout();
            } else {
                for (int i = 0;i < getChildCount();i++) {
                    View child = getChildAt(i);
                    if (child.getTop() < listVisibleHeight && child.getBottom() > listVisibleHeight) {
                        getLayoutParams().height = child.getBottom();
                        requestLayout();
                        break;
                    }
                 }
            }
            
            
        }

        public void setDate(CalendarItem target) {
            ListAdapter adapter = getAdapter();
            if (adapter == null) {
                throw new IllegalStateException("setAdapter first");
            }
            EventAdapter eventAdapter = (EventAdapter)adapter;
            int position = eventAdapter.getFirstPositionForDate(target);
            setSelection(position);
        }
        
        
    }
    
    
    private class ItemInfo {
        Event event;
        CalendarItem date;
        int positionInDate;
    }
    
    /**
     * Adapter for event list
     * @author junhzhan
     *
     */
    private class EventAdapter extends BaseAdapter {
        
        private int mCount;
        EventAdapter() {
            int total = 0;
            for (int year = Constant.START_YEAR;year <= Constant.END_YEAR;year++) {
                Calendar cal = new GregorianCalendar(year, Calendar.JANUARY, 1);
                int dayCountOfYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                total += dayCountOfYear;
            }
            mCount = total;
            Log.e(TAG, "adapter count " + mCount);
        }
        
        private int[] mEventPosition;
        private int[] mExtraEventCount;
        private ArrayList<CalendarItem> mSortedItems = new ArrayList<CalendarItem>();
        
        private Comparator<CalendarItem> mComparator = new Comparator<CalendarItem>() {

            @Override
            public int compare(CalendarItem lhs, CalendarItem rhs) {
                if (lhs.hashCode() < rhs.hashCode()) {
                    return -1;
                } else if (lhs.hashCode() > rhs.hashCode()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        
        @Override
        public void notifyDataSetChanged() {
            ArrayList<CalendarItem> sortedItems = new ArrayList<CalendarItem>();
            Set<CalendarItem> calendarItems = mEvents.keySet();
            sortedItems.addAll(calendarItems);
            
            int total = 0;
            for (int year = Constant.START_YEAR;year <= Constant.END_YEAR;year++) {
                Calendar cal = new GregorianCalendar(year, Calendar.JANUARY, 1);
                int dayCountOfYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                total += dayCountOfYear;
            }
            mCount = total;
            Collections.sort(sortedItems, mComparator);
            int accumulate = 0;
            mEventPosition = new int[sortedItems.size()];
            mExtraEventCount = new int[sortedItems.size()];
            int i = 0;
            for (CalendarItem item : sortedItems) {
                int originalPos = getOriginalPositionForDate(item);
                mEventPosition[i] = originalPos + accumulate;
                Log.d(TAG, String.format("calendar %d %d %d, origin position %d, actual position %d", item.year, 
                        item.month + 1, item.date, originalPos, mEventPosition[i]));
                EventItem eventItem = mEvents.get(item);
                int eventCount = eventItem.getCustomEventCount() + eventItem.getSyncEventCount();
                int extraEventCount = 0;
                if (eventCount == 0) {
                    extraEventCount = 0;
                } else {
                    extraEventCount = eventCount - 1;
                }
                accumulate += extraEventCount;
                mExtraEventCount[i] = extraEventCount;
                i++;
            }
            mCount += accumulate;
            mSortedItems = sortedItems;
            super.notifyDataSetChanged();
            Log.d(TAG, "position delta is " + mListPositionDelta);
            int adjustPosition = mListPositionDelta + mList.getFirstVisiblePosition();
            int offsetY = mList.getChildAt(0) == null ? 0 : mList.getChildAt(0).getTop();
            mList.setSelectionFromTop(adjustPosition, offsetY);
            mListPositionDelta = 0;
        }




        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public CalendarItem getItem(int position) {
            Calendar cal = new GregorianCalendar(Constant.START_YEAR, Calendar.JANUARY, 1);
            cal.add(Calendar.DATE, position);
            return new CalendarItem(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
        
        private ItemInfo getItemInfo(int position) {
            ItemInfo info = new ItemInfo();
            if (mEventPosition == null || mEventPosition.length == 0) {
                CalendarItem calendarItem = getOriginalDateForPosition(position);
                info.positionInDate = 0;
                info.date = calendarItem;
                return info;
            }
            int index = Arrays.binarySearch(mEventPosition, position);
            if (index >= 0) {
                CalendarItem calendarItem = mSortedItems.get(index);
                EventItem eventItem = mEvents.get(calendarItem);
                info.event = eventItem.getEvents()[0];
                info.positionInDate = 0;
                info.date = calendarItem;
            } else {
                int insertIndex = -index - 1;
                if (insertIndex == 0) {
                    CalendarItem calendarItem = getOriginalDateForPosition(position);
                    info.positionInDate = 0;
                    info.date = calendarItem;
                } else {
                    int prePos = mEventPosition[insertIndex - 1];
                    CalendarItem preDate = mSortedItems.get(insertIndex - 1);
                    EventItem eventItem = mEvents.get(preDate);
                    Event[] events = eventItem.getEvents();
                    if (position > prePos && position < prePos + events.length) {
                        info.date = preDate;
                        info.positionInDate = position - prePos;
                        info.event = events[position - prePos];
                    } else if (position >= prePos + events.length) {
                        info.positionInDate = 0;
                        int accumulateExtra = 0;
                        for (int i = 0;i <= insertIndex - 1;i++) {
                            accumulateExtra += mExtraEventCount[i];
                        }
                        CalendarItem calendarItem = getOriginalDateForPosition(position - accumulateExtra);
                        info.date = calendarItem;
                    } else {
                        throw new IllegalStateException();
                    }
                    
                }
            }
            return info;
        }
        
        public int getFirstPositionForDate(CalendarItem date) {
            int index = Collections.binarySearch(mSortedItems, date, mComparator);
            int position = 0;
            if (index >= 0) {
                position = mEventPosition[index];
            } else {
                int insertIndex = -index - 1;
                Log.d(TAG, "first position for date insert index " + insertIndex);
                int accumulate = 0;
                int originalPosition = getOriginalPositionForDate(date);
                for (int i = 0;i < insertIndex;i++) {
                    accumulate += mExtraEventCount[i];
                }
                position = originalPosition + accumulate;
                Log.d(TAG, String.format("first position for year %d, month %d, date %d, original %d, current %d", 
                        date.year, date.month + 1, date.date, originalPosition, position));
            }
            return position;
        }
        

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemInfo info = getItemInfo(position);
            Log.d(TAG, String.format("isHeaderItem %s | date year %d month %d date %d | event null %s", 
                    String.valueOf(info.positionInDate == 0), info.date.year, info.date.month + 1, info.date.date, String.valueOf(info.event == null)));
            if (convertView == null) {
                final LinearLayout item = new LinearLayout(getContext());
                item.setOrientation(LinearLayout.VERTICAL);
                
                TextView header = new TextView(getContext());
                header.setTextColor(getResources().getColor(R.color.event_listitem_header_textcolor));
                header.setGravity(Gravity.CENTER_VERTICAL);
                header.setBackgroundResource(R.drawable.event_listview_header_bg);
                header.setPadding(getResources().getDimensionPixelOffset(R.dimen.event_listitem_header_paddingleft), 0, 0, 0);
                item.addView(header, LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.event_listitem_header_height));
                TextView textContent = new TextView(getContext());
                textContent.setText("No content");
                textContent.setGravity(Gravity.CENTER_VERTICAL);
                textContent.setBackgroundColor(0xffffffff);
                textContent.setTextColor(0xff000000);
                textContent.setOnClickListener(new OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        ItemInfo info = (ItemInfo)v.getTag();
                        View extra = item.getChildAt(2);
                        if (extra.getVisibility() == View.VISIBLE) {
                            extra.setVisibility(View.GONE);
                            mList.removeExtraItem(info);
                        } else {
                            extra.setVisibility(View.VISIBLE);
                            mList.addExtraItem(info);
                        }
                    }
                });
                item.addView(textContent, LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.event_listitem_height));
                TextView extra = new TextView(getContext());
                extra.setText("Extra content");
                extra.setGravity(Gravity.CENTER_VERTICAL);
                extra.setBackgroundColor(0xff000000);
                extra.setTextColor(0xffffffff);
                extra.setVisibility(View.GONE);
                item.addView(extra, LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.event_listitem_extra_height));
                convertView = item;
            }
            LinearLayout item = (LinearLayout)convertView;
            TextView header = (TextView)item.getChildAt(0);
            if (info.positionInDate == 0) {
                header.setVisibility(View.VISIBLE);
                header.setText(String.format("%d-%d-%d", info.date.year, info.date.month + 1, info.date.date));
            } else {
                header.setVisibility(View.GONE);
            }
            View extra = item.getChildAt(2);
            item.getChildAt(1).setTag(info);
            Set<Integer> extraPositions = mHasExtraItems.get(info.date);
            if (extraPositions == null || !extraPositions.contains(info.positionInDate)) {
                extra.setVisibility(View.GONE);
            } else {
                extra.setVisibility(View.VISIBLE);
            }
            return convertView;
        }
        
        /**
         * get original position for specified date provided that there is no event.
         * @param item
         * @return
         */
        public int getOriginalPositionForDate(CalendarItem item) {
            checkDateValid(item);
            int total = 0;
            Calendar cal = new GregorianCalendar();
            for (int year = Constant.START_YEAR;year < item.year;year++) {
                cal.set(year, Calendar.JANUARY, 1);
                total += cal.getActualMaximum(Calendar.DAY_OF_YEAR);
            }
            cal.set(Calendar.YEAR, item.year);
            for (int month = Calendar.JANUARY;month < item.month;month++) {
                cal.set(Calendar.MONTH, month);
                total += cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            }
            total += item.date;
            return total - 1;
        }
        
        /**
         * get original date for position provided that there is no event.
         * @param pos
         * @return
         */
        public CalendarItem getOriginalDateForPosition(int pos) {
            if (pos >= getCount()) {
                throw new IllegalArgumentException();
            }
            Calendar cal = new GregorianCalendar(Constant.START_YEAR, Calendar.JANUARY, 1);
            cal.add(Calendar.DATE, pos);
            return new CalendarItem(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        }
        
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
                mList.setDate(item);
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
                mList.setDate(new CalendarItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date));
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
                    CalendarItem item = new CalendarItem(mCurrent.year, mCurrent.month, mCurrent.date);
                    mOutListener.onCalendarDateSelected(item);
                }
                cal.add(Calendar.MONTH, 1);
                mNext = new Data(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
                mList.setDate(new CalendarItem(mCurrent.year, mCurrent.month, mCurrent.date));
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
                mList.setDate(new CalendarItem(mCurrentWeekViewDate.year, mCurrentWeekViewDate.month, mCurrentWeekViewDate.date));
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
                mList.setDate(new CalendarItem(mCurrent.year, mCurrent.month, mCurrent.date));
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
            Log.e(TAG, "destroyItem " + object);
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
                    Log.e(TAG, "old row index is " + oldRow);
                    middelView.setMonthView(mCurrentWeekViewDate.year,
                            mCurrentWeekViewDate.month, mCurrentWeekViewDate.date);
                    middelView.setEvents(mEvents);
                    weekRow = middelView.getRowIndexOfDay();
                    Log.e(TAG, "new week row index is " + weekRow);
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
    
}
