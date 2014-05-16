package com.msopentech.thali.genericandroidlistener.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.couchbase.lite.*;
import com.couchbase.lite.android.*;
import com.couchbase.lite.listener.*;

import java.io.*;

public class MainActivity extends ActionBarActivity {
    private LiteListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndroidContext androidContext = new AndroidContext(getApplicationContext());
        Manager manager = null;
        try {
            manager = new Manager(androidContext, null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        listener = new LiteListener(manager, 10001, null);
        listener.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
