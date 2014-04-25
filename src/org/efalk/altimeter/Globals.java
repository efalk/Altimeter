/**
 * @file
 * Global values for the altimeter app. This is a singleton class.
 */

package org.efalk.altimeter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.util.Log;


/**
 * This is a singleton class to hold state across various activities.
 * As with any Android object, this can be destroyed at any time with
 * very little warning, so any components that take advantage of this
 * class must be prepared to re-create it at need.
 * Values which are specific to an activity are stored in that
 * activity, not here.
 */
public class Globals {
    //private static final String TAG = AltimeterActivity.TAG;
    private static volatile Globals instance = null;

    // User preferences
    boolean flingEnabled = true;

    public static Globals get(Context ctx) {
	if (instance == null) {
	    synchronized(Globals.class) {
		if (instance == null) {
		    instance = new Globals();
		    final SharedPreferences sp =
			PreferenceManager.getDefaultSharedPreferences(ctx);
		    if( sp != null ) {
			instance.restoreUserPreferences(ctx, sp);
			instance.restorePreferences(sp);
		    }
		}
	    }
	}
	return instance;
    }

    /**
     * Restore system variables from bundle
     */
    protected void restoreInstanceState(Bundle state) {
	flingEnabled = state.getBoolean("flingEnabled");
    }

    /**
     * Save system variables to bundle.
     */
    protected void onSaveInstanceState(Bundle state) {
	state.putBoolean("flingEnabled", flingEnabled);
    }

    /**
     * Restore user preferences.
     * User preferences are defined as those which are set in the
     * preferences activity. When the preferences activity terminates,
     * this method should be called to update those preferences.
     */
    protected void restoreUserPreferences(Context ctx, SharedPreferences sp) {
	// These are the preferences a user explicitly sets
	// Reminder: All list preferences must be to be saved and
	// restored from strings; PreferencesActivity doesn't support
	// any other format.
	flingEnabled = sp.getBoolean("flingEnabled", flingEnabled);
    }

    /**
     * Restore other system variables from user preferences.
     */
    void restorePreferences(SharedPreferences sp) {
    }

    /**
     * Store preferences
     */
    void storePreferences(SharedPreferences.Editor sp)
    {
	// User preferences don't need to be saved here because the
	// preferences activity will take care of it. Exception: those
	// user preferences which might also be changed in some other way.
    }
}
