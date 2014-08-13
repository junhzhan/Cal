package com.junhzhan.cal.widget;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cal.R;

public class CustomCalendarWidget extends CalendarWidget<String> {
    
    

    public CustomCalendarWidget(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomCalendarWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomCalendarWidget(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected View onCreateDateView(CalendarDayItem date,
            CalendarDayItem selectedDate, boolean weekOrMonthView) {
        RelativeLayout view = new RelativeLayout(getContext());
        TextView dateView = new TextView(getContext());
        dateView.setId(R.id.date_view_text_part_id);
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.calendar_date_text_size));
        
        RelativeLayout.LayoutParams textLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        textLayout.addRule(RelativeLayout.CENTER_IN_PARENT);
        view.addView(dateView, textLayout);
        View line = new View(getContext());
        line.setBackgroundColor(0xffff0000);
        int length = getResources().getDimensionPixelOffset(R.dimen.calendar_date_line_segment_length) * 3;
        int strokWidth = getResources().getDimensionPixelOffset(R.dimen.calendar_date_line_strok_width);
        RelativeLayout.LayoutParams lineLayout = new RelativeLayout.LayoutParams(length, strokWidth);
        lineLayout.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lineLayout.addRule(RelativeLayout.BELOW, R.id.date_view_text_part_id);
        view.addView(line, lineLayout);
        return view;
    }

    @Override
    protected void onDateViewInvalidate(View dateView,
            CalendarDayItem date, CalendarDayItem selectedDate,
            boolean weekOrMonthView) {
        RelativeLayout container = (RelativeLayout)dateView;
        View line = container.getChildAt(1);
        if (date.equals(selectedDate)) {
            line.setBackgroundColor(0xffff0000);
            line.getLayoutParams().width = getResources().getDimensionPixelOffset(R.dimen.calendar_date_line_segment_length) * 3;
            line.setVisibility(View.VISIBLE);
        } else {
            line.setVisibility(View.GONE);
            line.setBackgroundColor(0xffdbdbdb);
            List<String> events = getEvents(date.year, date.month, date.date);
            if (events != null) {
                int length = getResources().getDimensionPixelOffset(R.dimen.calendar_date_line_segment_length) * events.size();
                line.getLayoutParams().width = length;
                line.setVisibility(View.VISIBLE);
            } else {
                line.setVisibility(View.GONE);
            }
        }
        
    }


}
