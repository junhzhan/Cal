package com.junhzhan.cal.data;

import java.util.ArrayList;
import java.util.List;

public class EventItem {
    private List<CustomEvent> mCustomEvents = new ArrayList<CustomEvent>();
    private List<SyncEvent> mSyncEvents = new ArrayList<SyncEvent>();
    
    /**
     * add custom events to this item
     * @param events
     */
    public void addCustomEvent(List<CustomEvent> events) {
        if (events == null) {
            return;
        }
        mCustomEvents.addAll(events);
    }
    
    /**
     * add sync events to this item
     * @param events
     */
    public void addSyncEvent(List<SyncEvent> events) {
        if (events == null) {
            return;
        }
        mSyncEvents.addAll(events);
    }
    
    /**
     * get custom events
     * @return
     */
    public CustomEvent[] getCustomEvents() {
        return mCustomEvents.toArray(new CustomEvent[mCustomEvents.size()]);
    }
    
    /**
     * get sync events
     * @return
     */
    public SyncEvent[] getSyncEvents() {
        return mSyncEvents.toArray(new SyncEvent[mSyncEvents.size()]);
    }
    
    
    public int getCustomEventCount() {
        return mCustomEvents.size();
    }
    
    public int getSyncEventCount() {
        return mSyncEvents.size();
    }
    
    public Event[] getEvents() {
        ArrayList<Event> events = new ArrayList<Event>();
        events.addAll(mCustomEvents);
        events.addAll(mSyncEvents);
        return events.toArray(new Event[events.size()]);
    }
    
    
}
