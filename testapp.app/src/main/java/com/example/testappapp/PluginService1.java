package com.example.testappapp;


import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import eu.hydrologis.geopaparazzi.extension.types.IMenuEntry;

/**
 * Created by cesar on 6/02/17.
 */

public class PluginService1 extends IntentService {

    public PluginService1() {
        super(PluginService1.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int j = 1;
        int k = j + 2;
    }

    public IBinder onBind (Intent intent) {
        return op;

    }

    private final IMenuEntry.Stub op =
            new IMenuEntry.Stub() {

                @Override
                public String getLabel() throws RemoteException {
                    return PluginService1.class.getName();
                }

                @Override
                public byte[] getIcon() throws RemoteException {
                    return new byte[0];
                }

                @Override
                public String getAction() throws RemoteException {
                    return null;
                }
            };
}
