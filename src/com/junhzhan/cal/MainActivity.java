package com.junhzhan.cal;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cal.R;
import com.junhzhan.cal.data.CalendarItem;
import com.junhzhan.cal.widget.CalendarWidgetNew;
import com.junhzhan.cal.widget.OnCalendarDateSelectedListener;


public class MainActivity extends Activity {
    
    private String TAG = "MainActivity";    
    
    private CalendarWidgetNew mCalendar;
    private Handler mHandler = new Handler();
    private int mPickerItemHeight;
    private int mMainPickerColor;
    private int mAltPickerColor;
    
    private FrameLayout mTitleContainer;
    
    private int mTitleYear;
    private int mTitleMonth;
    
    private boolean mIsAnimating = false;
    
    private RelativeLayout mRootView;
    
    private RelativeLayout mPickerView;
    private IntegerPickerAdapter mYearAdapter;
    private IntegerPickerAdapter mMonthAdapter;
    private IntegerPickerAdapter mDateAdapter;
    private ListView mYearList;
    private ListView mMonthList;
    private ListView mDateList;
    private ListScrollListener mYearScrollListener;
    private ListScrollListener mMonthScrollListener;
    private ListScrollListener mDateScrollListener;
    
    private static final int SCROLL_BUFFER_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mRootView = (RelativeLayout)findViewById(R.id.root);
        View today = findViewById(R.id.today);
        mCalendar = (CalendarWidgetNew)findViewById(R.id.calendar);
        today.setOnClickListener(mClickListener);
        mCalendar.setOnCalendarDateSelectedListener(mDateSelectedListener);
        mTitleContainer = (FrameLayout)findViewById(R.id.title_container);
        mTitleContainer.setOnClickListener(mClickListener);
        Calendar cal = new GregorianCalendar();
        setTitle(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        mMainPickerColor = getResources().getColor(R.color.calendar_view_main_textcolor);
        mAltPickerColor = getResources().getColor(R.color.calendar_view_alt_textcolor);
        mPickerItemHeight = getResources().getDimensionPixelOffset(R.dimen.calendar_picker_listitem_height);
    }
    
    private OnClickListener mClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.today:
                Calendar cal = new GregorianCalendar();
                mCalendar.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                break;
            case R.id.title_container:
                CalendarItem item = mCalendar.getCurrentDate();
                showDatePicker(item.year, item.month, item.date);
                break;
            case R.id.close:
                dismissDatePicker();
                break;
            case R.id.confirm:
                int year = mYearScrollListener.getCurrentTargetPosition() - SCROLL_BUFFER_COUNT + Constant.START_YEAR;
                int month = mMonthScrollListener.getCurrentTargetPosition() - SCROLL_BUFFER_COUNT + Calendar.JANUARY;
                int date = mDateScrollListener.getCurrentTargetPosition() -SCROLL_BUFFER_COUNT + 1;
                mCalendar.setDate(year, month, date);
                setTitle(year, month, date);
                dismissDatePicker();
                break;
            default:
                break;
            }
        }
    };
    
    private OnCalendarDateSelectedListener mDateSelectedListener = new OnCalendarDateSelectedListener() {
        
        @Override
        public void onCalendarDateSelected(CalendarItem item) {
            setTitle(item.year, item.month, item.date);
        }
    };
    
    private void setTitle(int year, int month, int date) {
        if (year != mTitleYear || month != mTitleMonth) {
            TextView title = new TextView(this);
            title.setTextColor(getResources().getColor(
                    R.color.month_title_textcolor));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources()
                    .getDimension(R.dimen.calendar_month_title_text_size));
            title.setText(getResources().getString(R.string.month_title, year,
                    month + 1));
            title.setTypeface(null, Typeface.BOLD);
            final View oldTitle = mTitleContainer.getChildAt(0);
            if (mIsAnimating) {
                mTitleContainer.removeAllViews();
            } else {
                Animation exitAnimatioin = AnimationUtils.loadAnimation(
                        this, R.anim.left_out_anim);
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
                                for (int i = 0; i < count; i++) {
                                    mTitleContainer.removeViewAt(0);
                                }

                            }
                        });
                    }
                });
                mIsAnimating = true;
            }
            mTitleContainer.addView(title, LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            title.startAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.right_in_anim));

            mTitleYear = year;
            mTitleMonth = month;
        }
    }
    
    private class IntegerPickerAdapter extends BaseAdapter {

        private int mEnd;
        private int mStart;
        private 
        IntegerPickerAdapter(int start, int end) {
            super();
            mStart = start;
            mEnd = end;
        }
        @Override
        public int getCount() {
            return mEnd - mStart + 1 + SCROLL_BUFFER_COUNT * 2;
        }

        @Override
        public String getItem(int position) {
            if ((position < SCROLL_BUFFER_COUNT) || (position >= getCount() - SCROLL_BUFFER_COUNT)) {
                return "";
            } else {
                return String.valueOf(mStart + position - SCROLL_BUFFER_COUNT);
            }
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                TextView textView = new TextView(MainActivity.this);
                textView.setLayoutParams(
                        new android.widget.AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelOffset(R.dimen.calendar_picker_listitem_height)));
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(getResources().getColor(R.color.calendar_view_alt_textcolor));
                convertView = textView;
            }
            TextView itemView = (TextView)convertView;
            String item = getItem(position);
            itemView.setText(item);
            return convertView;
        }
        
        public int getStart() {
            return mStart;
        }
        
        public int getEnd() {
            return mEnd;
        }
        
        public void setEnd(int end) {
            if (end == mEnd) {
                return;
            }
            if (mStart >= end) {
                throw new IllegalArgumentException();
            }
            mEnd = end;
            notifyDataSetChanged();
        }
        
        public void setStart(int start) {
            if (start == mStart) {
                return;
            }
            if (start >= mEnd) {
                throw new IllegalArgumentException();
            }
            mStart = start;
            notifyDataSetChanged();
        }
        
    }
    
    private static interface OnTargetItemChangedListener {
        void onTargetItemChanged(int newTargetPosition, int oldTargetPosition);
    }
    
    private class ListScrollListener implements OnScrollListener {
        
        private int mFirstVisibleItem = -1;
        private int mOldTargetPosition = -1;
        private int mScrollState;
        private OnTargetItemChangedListener mOnTargetItemChangedListener;
        
        public void setOnTargetItemChangedListener(OnTargetItemChangedListener l) {
            mOnTargetItemChangedListener = l;
        }

        @Override
        public void onScrollStateChanged(final AbsListView view, int scrollState) {
            if (mScrollState != scrollState && scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                View firstChild = view.getChildAt(0);
                if (firstChild != null) {
                    int height = firstChild.getHeight();
                    int bottom = firstChild.getBottom();
                    if (bottom < height / 2) {
                        Log.e("MainActivity", " 1 first visible item is " + mFirstVisibleItem);
                        
                        final int position = mFirstVisibleItem;
                        mHandler.post(new Runnable() {
                            
                            @Override
                            public void run() {
                                view.smoothScrollToPositionFromTop(position + 1, 0, 100);
                            }
                        });
                    } else {
                        final int position = mFirstVisibleItem;
                        mHandler.post(new Runnable() {
                            
                            @Override
                            public void run() {
                                view.smoothScrollToPositionFromTop(position, 0, 100);
                            }
                        });
                        Log.e("MainActivity", " 2 first visible item is " + mFirstVisibleItem);
                    }
                    
                }
            }
            mScrollState = scrollState;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            for (int i = 0; i < view.getChildCount();i++) {
                View child = view.getChildAt(i);
                if (child != null && child instanceof TextView) {
                    TextView text = (TextView)child;
                    int top = text.getTop();
                    if ((top > mPickerItemHeight * 3 - mPickerItemHeight / 2) && (top < mPickerItemHeight * 3 + mPickerItemHeight / 2)) {
                        text.setTextColor(mMainPickerColor);
                        int newTargetPosition = firstVisibleItem + i;
                        if (mOldTargetPosition != newTargetPosition) {
                            if (mOnTargetItemChangedListener != null) {
                                mOnTargetItemChangedListener.onTargetItemChanged(newTargetPosition, mOldTargetPosition);
                            }
                            mOldTargetPosition = newTargetPosition;
                        }
                    } else {
                        text.setTextColor(mAltPickerColor);
                    }
                }
            }
            mFirstVisibleItem = firstVisibleItem;
        }
        
        public int getCurrentTargetPosition() {
            return mOldTargetPosition;
        }
        
        
    }
    
    private void showDatePicker(final int year, int month, int date) {
        if (mPickerView == null) {
            mYearAdapter = new IntegerPickerAdapter(Constant.START_YEAR, Constant.END_YEAR);
            mMonthAdapter = new IntegerPickerAdapter(Calendar.JANUARY + 1, Calendar.DECEMBER + 1);
            mDateAdapter = new IntegerPickerAdapter(1, 31);
            RelativeLayout picker = (RelativeLayout)LayoutInflater.from(this).inflate(R.layout.picker, null);
            View close = picker.findViewById(R.id.close);
            close.setOnClickListener(mClickListener);
            View confirm = picker.findViewById(R.id.confirm);
            confirm.setOnClickListener(mClickListener);
            mYearList = (ListView)picker.findViewById(R.id.year_list);
            mYearScrollListener = new ListScrollListener();
            mYearList.setOnScrollListener(mYearScrollListener);
            mYearScrollListener.setOnTargetItemChangedListener(new OnTargetItemChangedListener() {
                
                @Override
                public void onTargetItemChanged(int newTargetPosition, int oldTargetPosition) {
                    int targetYear = newTargetPosition - SCROLL_BUFFER_COUNT + Constant.START_YEAR;
                    if (targetYear >= Constant.START_YEAR && targetYear <= Constant.END_YEAR) {
                        int month = mMonthScrollListener.getCurrentTargetPosition() - SCROLL_BUFFER_COUNT + Calendar.JANUARY;
                        Calendar cal = new GregorianCalendar(targetYear, month, 1);
                        int maximumDate = cal.getActualMaximum(Calendar.DATE);
                        mDateAdapter.setEnd(maximumDate);
                    }
                }
            });
            mYearList.setAdapter(mYearAdapter);
            mMonthList = (ListView)picker.findViewById(R.id.month_list);
            mMonthList.setAdapter(mMonthAdapter);
            mMonthScrollListener = new ListScrollListener();
            mMonthScrollListener.setOnTargetItemChangedListener(new OnTargetItemChangedListener() {
                
                @Override
                public void onTargetItemChanged(int newTargetPosition, int oldTargetPosition) {
                    int targetMonth = Calendar.JANUARY + newTargetPosition - SCROLL_BUFFER_COUNT;
                    if (targetMonth >= Calendar.JANUARY && targetMonth <= Calendar.DECEMBER) {
                        int year = mYearScrollListener.getCurrentTargetPosition() - SCROLL_BUFFER_COUNT + Constant.START_YEAR; 
                        Calendar cal = new GregorianCalendar(year, targetMonth, 1);
                        int maximumDate = cal.getActualMaximum(Calendar.DATE);
                        mDateAdapter.setEnd(maximumDate);
                    }
                }
            });
            mMonthList.setOnScrollListener(mMonthScrollListener);
            mDateList = (ListView)picker.findViewById(R.id.date_list);
            mDateList.setAdapter(mDateAdapter);
            mDateScrollListener = new ListScrollListener();
            mDateList.setOnScrollListener(mDateScrollListener);
            RelativeLayout.LayoutParams pickerLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, 
                    LayoutParams.MATCH_PARENT);
            pickerLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mRootView.addView(picker, pickerLayout);
            mPickerView = picker;
        }
        Calendar cal = new GregorianCalendar(year, month, date);
        mYearList.setSelection(year - Constant.START_YEAR);
        mMonthList.setSelection(month - Calendar.JANUARY);
        int maxiumDate = cal.getActualMaximum(Calendar.DATE);
        int currentDateMax = mDateAdapter.getEnd();
        if (maxiumDate != currentDateMax) {
            mDateAdapter.setEnd(maxiumDate);
        }
        mDateList.setSelection(date - 1);
        mPickerView.setVisibility(View.VISIBLE);
    }
    
    private void dismissDatePicker() {
        mPickerView.setVisibility(View.GONE);
    }
    
}
