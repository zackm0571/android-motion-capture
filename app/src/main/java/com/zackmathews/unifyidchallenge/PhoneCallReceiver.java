package com.zackmathews.unifyidchallenge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
/**
 * Base BroadcastReceiver for handling call state.
 */
public abstract class PhoneCallReceiver extends BroadcastReceiver {
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String tmState = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            int state = 0;
            if (tmState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (tmState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (tmState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }
            onCallStateChanged(context, state);
        }
    }

    protected abstract void onIncomingCallReceived(Context context, Date start);

    protected abstract void onIncomingCallAnswered(Context context, Date start);

    protected abstract void onIncomingCallEnded(Context context, Date start, Date end);

    public void onCallStateChanged(Context context, int state) {
        if (lastState == state) {
            //No change in state
            Log.d(getClass().getSimpleName(), "No change in call state, breaking");
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                onIncomingCallReceived(context, callStartTime);
                Log.d(getClass().getSimpleName(), "Incoming call received");
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    onIncomingCallReceived(context, new Date());
                } else {
                    isIncoming = true;
                    callStartTime = new Date();
                    onIncomingCallAnswered(context, callStartTime);
                    Log.d(getClass().getSimpleName(), "Incoming call answered");
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                // Call end
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(getClass().getSimpleName(), "Missed call");
                } else if (isIncoming) {
                    onIncomingCallEnded(context, callStartTime, new Date());
                    Log.d(getClass().getSimpleName(), "Incoming call ended");
                } else {
                    onIncomingCallEnded(context, callStartTime, new Date());
                    Log.d(getClass().getSimpleName(), "Outgoing call ended");
                }
                break;
        }
        lastState = state;
    }
}
