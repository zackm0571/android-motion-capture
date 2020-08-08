package com.zackmathews.unifyidchallenge;

import android.content.Context;
import android.util.Log;

import java.util.Date;

public class UnifyCallReceiver extends PhoneCallReceiver {
    private static int ACTIVE_CALLS = 0;
    //todo fix static context
    private static SensorDataRepo repo;

    @Override
    protected void onIncomingCallReceived(Context context, Date start) {
        ACTIVE_CALLS++;
        Log.d(getClass().getSimpleName(), "onIncomingCallReceived");
        Log.d(getClass().getSimpleName(), String.format("Active calls: %d", ACTIVE_CALLS));
        if (repo == null) {
            repo = new SensorDataRepo(context);
        }
        repo.startSensorCapture();
    }

    @Override
    protected void onIncomingCallAnswered(Context context, Date start) {
        ACTIVE_CALLS--;
        Log.d(getClass().getSimpleName(), "onIncomingCallAnswered");
        Log.d(getClass().getSimpleName(), String.format("Active calls: %d", ACTIVE_CALLS));
        if (repo != null && ACTIVE_CALLS == 0) {
            repo.stopSensorCapture();
            repo = null;
        }
    }

    @Override
    protected void onIncomingCallEnded(Context context, Date start, Date end) {
        ACTIVE_CALLS--;
        Log.d(getClass().getSimpleName(), "onIncomingCallEnded");
        Log.d(getClass().getSimpleName(), String.format("Active calls: %d", ACTIVE_CALLS));
        if (repo != null && ACTIVE_CALLS == 0) {
            repo.stopSensorCapture();
            repo = null;
        }
    }
}
