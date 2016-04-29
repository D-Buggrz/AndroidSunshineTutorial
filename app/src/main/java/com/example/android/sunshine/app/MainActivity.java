package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String LOGGING_STRING = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGGING_STRING, "Calling onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOGGING_STRING, "Calling onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOGGING_STRING, "Calling onPause");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOGGING_STRING, "Calling onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOGGING_STRING, "Calling onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOGGING_STRING, "Calling onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.d(ForecastFragment.class.getName(), "Launching the settings activity");
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (id == R.id.action_show_map) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String locationString = sharedPref.getString(SettingsActivity.LOCATION_PREF_ID, getString(R.string.pref_location_default));
            return showLocationOnMap(locationString, this);
        }

        return super.onOptionsItemSelected(item);
    }

    public static boolean showLocationOnMap(String location, Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri intentGeoData = Uri.parse("geo:0,0?").buildUpon().appendQueryParameter("q", location).build();
        Log.d(MainActivity.class.getSimpleName(), "Calling implicit intent for: " + intentGeoData.toString());
        intent.setData(intentGeoData);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, R.string.error_no_map_found,
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }


}
