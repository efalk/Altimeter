/**
 * @file
 * Main activity for altimeter application.
 */

package org.efalk.altimeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

public class AltimeterActivity extends Activity implements SensorEventListener
{
    // Sensor-related variables
    static final String TAG = "Altimeter";
    private SensorManager sensorManager;
    private Altimeter altimeter;
    private float kollsman = 1013.25f;	// mB

    // User preferences
    int altUnits = Altimeter.UNITS_FT;
    int presUnits = Altimeter.UNITS_HG;
    boolean flingEnabled = true;
    int orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;

    @Override
    public void onCreate(Bundle savedState)
    {
	SharedPreferences sp;
        super.onCreate(savedState);

	setContentView(R.layout.altimeter);
	altimeter = (Altimeter) findViewById(R.id.altimeter);

	Object oldConfiguration = getLastNonConfigurationInstance();
	if( oldConfiguration != null ) {
	    restoreOldConfiguration(oldConfiguration);
	} else if( savedState != null ) {
	    restoreInstanceState(savedState);
	} else {
	    sp = PreferenceManager.getDefaultSharedPreferences(this);
	    if( sp != null )
		recallPreferences(sp);
	}

	altimeter.setAltUnits(altUnits);
	altimeter.setPresUnits(presUnits);

        sensorManager =
          (SensorManager) getSystemService(Context.SENSOR_SERVICE);

	altimeter.setKollsman(kollsman);
    }

    @Override
    public void onResume() {
        super.onResume();
	enableSensors();
    }

    @Override
    public void onPause() {
        super.onPause();
	disableSensors();
	try {
	    SharedPreferences.Editor sp =
	      PreferenceManager.getDefaultSharedPreferences(this).edit();
	    storePreferences(sp);
	    sp.commit();
	} catch (Exception e) {}
    }

    /**
     * Save state before configuration change.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
	kollsman = altimeter.getKollsman();
	return this;
    }

    /**
     * Restore saved instance state after configuration change
     */
    private void restoreOldConfiguration(Object oldConfiguration) {
	AltimeterActivity old = (AltimeterActivity)oldConfiguration;
	// Primary state
	altUnits = old.altUnits;
	presUnits = old.presUnits;
	flingEnabled = old.flingEnabled;
	sensorManager = old.sensorManager;
	kollsman = old.kollsman;
	orientation = old.orientation;
    }

    /**
     * Save state before this app goes away.
     */
    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putFloat("kollsman", kollsman);
	state.putInt("altUnits", altUnits);
	state.putInt("presUnits", presUnits);
	state.putBoolean("flingEnabled", flingEnabled);
	state.putInt("orientation", orientation);
    }

    /**
     * Restore saved instance state after app comes back.
     */
    private void restoreInstanceState(Bundle state) {
	// Primary state
	kollsman = state.getFloat("kollsman");
	altUnits = state.getInt("altUnits");
	presUnits = state.getInt("presUnits");
	flingEnabled = state.getBoolean("flingEnabled");
	orientation = state.getInt("orientation");
    }

    /**
     * Store preferences before app goes away for good. User preferences
     * set by the preferences activity do not need to be saved here.
     */
    private void storePreferences(SharedPreferences.Editor sp)
    {
        sp.putFloat("kollsman", kollsman);
    }

    /**
     * Load preferences when app starts up. User preferences are loaded
     * in their own function.
     */
    protected void recallPreferences(SharedPreferences sp)
    {
	kollsman = sp.getFloat("kollsman", kollsman);
	updatePreferences(sp);
    }

    /**
     * Update user preferences.	 Called after a preferences activity
     * returns or when app starts up.
     */
    protected void updatePreferences(SharedPreferences sp) {
	/* Protip: ListPreference only works with string arrays.  If
	 * you want to represent an integer value in a ListPreference
	 * item, you'll have to convert it from String.  You will also
	 * have to store it as String or preferences activity will crash.
	 */
	altUnits = Integer.parseInt(sp.getString("altUnits", ""+altUnits));
	presUnits = Integer.parseInt(sp.getString("presUnits", ""+presUnits));
	orientation = Integer.parseInt(sp.getString("orientation",
	  ""+ActivityInfo.SCREEN_ORIENTATION_SENSOR));
	if (getRequestedOrientation() != orientation)
	    setRequestedOrientation(orientation);
    }


    // Menu-related stuff

    static protected final int MENU_ABOUT = 0;
    static protected final int MENU_PREFERENCES = 1;
    static protected final int MENU_KOLLSMAN = 2;

    /**
     * Called before the options menu is opened for the first time.
     * There is also a way to do this from an xml file.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ABOUT, 0, R.string.about)
          .setAlphabeticShortcut('a')
          .setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_PREFERENCES, 0, R.string.preferences)
          .setAlphabeticShortcut('p')
          .setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
          case MENU_ABOUT: showDialog(DIALOG_ABOUT); break;
	  case MENU_PREFERENCES:
	    Intent intent =
	      new Intent().setClass(this, AltimeterPreferences.class);
	    startActivityForResult(intent, MENU_PREFERENCES);
	    break;
          default: return false;
        }
        return true;
    }


    // Dialog-related stuff

    protected static final int DIALOG_ABOUT = 0;
    protected static final int DIALOG_HELP = 1;

    /**
     * This function creates and returns a Dialog object.  It shows
     * two different ways to create a dialog: either with AlertDialog.Builder,
     * or by creating a custom class and returning an instance of it.
     */
    @Override
    protected Dialog onCreateDialog(int id) {
      switch( id ) {
        case DIALOG_ABOUT: // return new About(this);
          return new AlertDialog.Builder(this)
              .setTitle(R.string.aboutWelcome)
	      .setMessage("Altimeter " + getVersion() +
		getString(R.string.aboutContent))
              .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface d, int w) {}})
              .create();
        //case DIALOG_HELP: return new HelpContents(this);
        default: return super.onCreateDialog(id);
      }
    }

    /**
     * Utility: Get this application's package version string.
     */
    private String getVersion() {
	PackageManager pm = getPackageManager();
	try {
	    PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
	    return pi.versionName;
	} catch (PackageManager.NameNotFoundException e) {
	    Log.e(TAG, "unable to find package name", e);
	    return "";
	}
    }

    @Override
    protected void
    onActivityResult(int requestCode, int resultCode, Intent data)
    {
	switch (requestCode) {
	  case MENU_PREFERENCES:
	    SharedPreferences sp =
	      PreferenceManager.getDefaultSharedPreferences(this);
	    updatePreferences(sp);
	    altimeter.setAltUnits(altUnits);
	    altimeter.setPresUnits(presUnits);
	    break;
	  case MENU_KOLLSMAN:
	    if (data != null) {
		float f = data.getIntExtra("value", 1013);
		if (f != 0) {
		    if (altUnits == Altimeter.UNITS_HG)
			f *= .01 / Altimeter.HG_MB;
		    altimeter.setKollsman(f);
		}
	    }
	    break;
	}
    }



    @Override
    public final boolean onTouchEvent(MotionEvent event) {
	if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
	Log.d(TAG, "Bring up Kollsman window");
	float k = altimeter.getKollsman();
	float p = altimeter.getPressure();
	float altConv = 1;
	if (presUnits == Altimeter.UNITS_HG) {
	    // Convert to Hg and mulitiply by 100 for display purposes
	    k *= Altimeter.HG_MB * 100;
	    p *= Altimeter.HG_MB * 100;
	}
	if (altUnits == Altimeter.UNITS_FT)
	    altConv = 1.0f/Altimeter.METER_FT;
	Kollsman.launch(this, MENU_KOLLSMAN, (int)(k+.5), (int)(p+.5), altConv);
	return true;
    }

    private void enableSensors() {
	if (sensorManager != null) {
	    Sensor sensor;
	    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    if (sensor != null) {
		sensorManager.registerListener(this, sensor,
		    SensorManager.SENSOR_DELAY_UI);
	    } else {
		Log.e(TAG, "No pressure sensors on this device");
	    }
        }
    }

    private void disableSensors() {
        if (sensorManager != null)
	    sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int arg1) { }

    public void onSensorChanged(SensorEvent event) {
	altimeter.setPressure(event.values[0], event.timestamp);
    }
}
