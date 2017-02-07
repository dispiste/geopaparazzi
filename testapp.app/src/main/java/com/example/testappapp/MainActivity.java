package com.example.testappapp;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("testGeopap", 0);
        SharedPreferences.Editor editor = settings.edit();
        String s1 = settings.getString("testGeopapReferrer2", "");
        Log.w("TEST", "Referrer is: " + s1);
        s1 = settings.getString("testGeopapReferrer", "");
        Log.w("TEST", "Referrer is: " + s1);
    }
}
