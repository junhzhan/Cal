package com.junhzhan.cal;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.cal.R;
import com.junhzhan.cal.widget.CalendarDayItem;
import com.junhzhan.cal.widget.CustomCalendarWidget;
import com.junhzhan.cal.widget.OnCalendarDateSelectedListener;


public class MainActivity extends Activity {
    
    private String TAG = "MainActivity";
    
    private HashMap<CalendarDayItem, List<String>> mEvents = new HashMap<CalendarDayItem, List<String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        LinearLayout root = new LinearLayout(this);
//        root.setOrientation(LinearLayout.VERTICAL);
//        final CustomCalendarWidget cal = new CustomCalendarWidget(this);
//        cal.setOnCalendarDateSelectedListener(mListener);
//        root.addView(cal, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//        Button addEvent = new Button(this);
//        addEvent.setText("Add event");
//        addEvent.setOnClickListener(new OnClickListener() {
//            
//            @Override
//            public void onClick(View v) {
//                CalendarDayItem item = cal.getSelectedDate();
//                List<String> events = mEvents.get(item);
//                if (events == null) {
//                    events = new ArrayList<String>();
//                    mEvents.put(item, events);
//                }
//                events.add("test");
//                cal.addEvent("test");
//                
//            }
//        });
//        root.addView(addEvent, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//        Button expand = new Button(this);
//        expand.setText("expand");
//        expand.setOnClickListener(new OnClickListener() {
//            
//            @Override
//            public void onClick(View v) {
//                cal.changeToMonthView();
//            }
//        });
//        root.addView(expand, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//        Button unexpand = new Button(this);
//        unexpand.setText("unexpand");
//        unexpand.setOnClickListener(new OnClickListener() {
//            
//            @Override
//            public void onClick(View v) {
//                cal.changeToWeekView();
//            }
//        });
//        root.addView(unexpand, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//        setContentView(root);
        setContentView(R.layout.main);
        CustomCalendarWidget calendar = (CustomCalendarWidget)findViewById(R.id.calendar);
        
    }
    
    private class TestAdapter extends ArrayAdapter<String> {
        TestAdapter(Context ctx) {
            super(ctx, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                TextView textView = new TextView(getContext());
                textView.setGravity(Gravity.CENTER_VERTICAL);
                convertView = textView;
                convertView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, 200));
            }
            ((TextView)convertView).setText(getItem(position));
            return convertView;
        }
        
        
    }
    
    
    private OnCalendarDateSelectedListener<String> mListener = new OnCalendarDateSelectedListener<String>() {

        @Override
        public void onCalendarDateSelected(CalendarDayItem item,
                List<String> events) {
            Log.e(TAG, String.format("calendar date selected %d %d %d", item.year, item.month, item.date));
            if (events != null) {
                for (String event : events) {
                    Log.e(TAG, String.format("event %s", event));
                }
            }
        }
    };
    
}
