
/**
 * This class converts between pressure and altitude
 */

package org.efalk.altimeter;

//import android.util.Log;

import java.lang.Math;

public class Barometer {
    /*
     * The Engineering toolbox at
     * http://www.engineeringtoolbox.com/air-altitude-pressure-d_462.html
     * gives this equation:
     *
     *  p = 101325 (1 - 2.25577e-5 h)^5.25588
     *
     * Solving for h gives
     *
     *  h = (1 - (p / 101325)^(1/5.25588))/2.25577e-5
     *
     * Where 101325 is p0 = atmospheric pressure at sea level
     * in Pascals and h is altitude in meters
     *
     * This is all approximation anyway, since we can only guess at
     * the temperature and pressure gradient in the atmosphere.
     *
     * We can use any units for pressure we want by setting p0
     * accordingly. The Android sensor stack uses mb, so that's
     * what we'll use here.
     *
     * Other references:
     *  http://www.hills-database.co.uk/altim.html
     *  https://en.wikipedia.org/wiki/Altimeter:
     */

    //static private final String TAG = FlightDeck.TAG;
    //static private final float ATM = 101325f;	// Pa
    static private final float ATM = 1013.25f;	// mB
    //static private final float ATM = 29.92f;	// "Hg
    static private final double SCALE = 2.25577e-5;
    static private final double EXP = 5.25588;
    static private final float DAMPING = 0.85f;
    static private final float VSI_DAMPING = 0.95f;

    float kollsman = ATM;
    float pres = ATM;		// Last recorded pressure
    float alt = 0;
    float vsi = 0;
    long lastTs = 0;
    long lastTime;

    public void resetKollsman() {
	kollsman = ATM;
    }

    public void setKollsman(float k) {
	kollsman = k;
    }

    /**
     * Convert altitude in meters to pressure
     */
    public float a2p(float meters) {
	return kollsman * a2ratio(meters);
    }

    /**
     * Convert pressure to altitude in meters
     */
    public float p2a(float pres) {
	this.pres = pres;
	return ratio2a(pres / kollsman);
    }

    /**
     * Convert sealevel pressure and local pressure to altitude in meters
     */
    public static float p2a(float sealevel, float pres) {
	return ratio2a(pres / sealevel);
    }

    /**
     * Convert sealevel pressure and altitude in meters to local pressure
     */
    public static float a2p(float sealevel, float meters) {
	return sealevel * a2ratio(meters);
    }

    /**
     * Convert local pressure and altitude in meters to sealevel pressure.
     */
    public static float a2sealevel(float pres, float meters) {
	return pres / a2ratio(meters);
    }

    /**
     * Convert pressure to altitude in meters, damped.
     * Also compute vsi
     */
    public float p2aDamped(float pres, long now) {
	float x = p2a(pres);
	if (lastTs > 0) {
	    float oldAlt = alt;
	    alt = x * (1-DAMPING) + alt * DAMPING;
	    if (now > lastTs) {
		// TODO: FAA standards actually say what the time constant
		// for a VSI should be. Winging it for now.
		float dt = .000000001f * (now - lastTs);
		float rate = (alt - oldAlt) / dt;
		vsi = rate * (1-VSI_DAMPING) + vsi * VSI_DAMPING;
	    }
	} else {
	    alt = x;
	}
	lastTs = now;
	lastTime = System.currentTimeMillis();
	return alt;
    }

    /* True if last update was within ten seconds */
    public boolean recent(long millis) {
	return (millis - lastTime) < 10000;
    }

    /**
     * Convert altitude in meters to pressure/basePressure ratio
     */
    public static float a2ratio(float meters) {
	return (float)Math.pow(1 - SCALE*meters, EXP);
    }

    /**
     * Convert a pressure/basePressure ratio to an altitude
     */
    public static float ratio2a(float ratio) {
	return (float)((1 - Math.pow(ratio, 1/EXP))*(1./SCALE));
    }


    //for alt in xrange(-5000,50000,500):
    //  pa = a2p(alt*.3048)
    //  print '%.0f	%.0f	%.1f	%.0f' % \
    //(alt, alt*.3048+.5, pa / 100, p2a(pa))
}
