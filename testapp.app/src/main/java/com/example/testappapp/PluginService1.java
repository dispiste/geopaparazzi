package com.example.testappapp;


import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.example.testappapp.aidl.PluginInterface;

/**
 * Created by cesar on 6/02/17.
 */

public class PluginService1 extends IntentService {

    public PluginService1() {
        super(PluginService1.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public IBinder onBind (Intent intent) {
        return op;

    }

    private final PluginInterface.Stub op =
            new PluginInterface.Stub() {

                @Override
                public String getTitle() throws RemoteException {
                    return PluginService1.class.getName();
                }
            };
}
