/**
 * @file
 * Global values for the altimeter app. This is a singleton class.
 */

package org.efalk.altimeter;

import android.content.Context;
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
		}
	    }
	}
	return instance;
    }
}
