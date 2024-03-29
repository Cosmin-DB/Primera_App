package com.example.primera_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class NotificationReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public NotificationReceiver(Handler handler) {
        super(handler);
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);

    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {

        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }

}