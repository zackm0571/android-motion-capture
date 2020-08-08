package com.zackmathews.unifyidchallenge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Base BroadcastReceiver for handling call state.
 */
public abstract class PhoneCallReceiver extends BroadcastReceiver {
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
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

    /**
     * Notifies consumers of an incoming call.
     *
     * @param context
     */
    protected abstract void onIncomingCallReceived(Context context);

    /**
     * Notifies consumers that an incoming call has been answered.
     *
     * @param context
     */
    protected abstract void onIncomingCallAnswered(Context context);

    /**
     * Notifies consumers that an incoming call has ended.
     *
     * @param context
     */
    protected abstract void onIncomingCallEnded(Context context);

    /**
     * Handles state change and invokes appropriate callback.
     *
     * @param context
     * @param state
     */
    public void onCallStateChanged(Context context, int state) {
        if (lastState == state) {
            //No change in state
            Log.d(getClass().getSimpleName(), "No change in call state, breaking");
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                onIncomingCallReceived(context);
                Log.d(getClass().getSimpleName(), "Incoming call received");
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(getClass().getSimpleName(), "Outgoing call initiated");
                    isIncoming = false;
                    // test as I don't have another phone to call myself with
                    onIncomingCallReceived(context);
                } else {
                    isIncoming = true;
                    onIncomingCallAnswered(context);
                    Log.d(getClass().getSimpleName(), "Incoming call answered");
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                // Call end
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(getClass().getSimpleName(), "Missed call");
                } else if (isIncoming) {
                    onIncomingCallEnded(context);
                    Log.d(getClass().getSimpleName(), "Incoming call ended");
                } else {
                    Log.d(getClass().getSimpleName(), "Outgoing call ended");
                    // test as I don't have another phone to call myself with
                    onIncomingCallEnded(context);
                }
                break;
        }
        lastState = state;
    }
}
