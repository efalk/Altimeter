/**
 * This module contains both the Kollsman-setting activity, and the
 * Kollsman wheel scroller view.
 */

package org.efalk.altimeter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector;
import android.widget.Button;
import android.util.Log;

/**
 * An activity to read an airport, navaid, or waypoint ident from
 * the user and return it to the caller.
 */
public class Kollsman extends Activity
{
    private static final String TAG = AltimeterActivity.TAG;

    // User preferences
    boolean flingEnabled = true;

    private int value;
    private int barom;
    private float convert;
    private Button okButton;
    private KollsmanView kview;

    /**
     * Utility: launch this activity.
     * @param initial  Initial base pressure value
     * @param barom    Current barometer value
     * @param convert  Conversion factor from meters to displayed units
     * It doesn't matter what units the initial and current pressure
     * values are, as long as they're consistent. Values are int, so you
     * may want to multiply by a scale factor, e.g. 100 if your units are
     * "Hg
     * The conversion factor is multiplied by meters to get whatever
     * units you want, e.g. .30480 to get feet
     */
    public static void launch(Activity ctx, int resultCode,
	    int initial, int barom, float convert)
    {
	Intent intent = new Intent(ctx, Kollsman.class);
	intent.putExtra("initial", initial);
	intent.putExtra("barom", barom);
	intent.putExtra("convert", convert);
	try {
	    ctx.startActivityForResult(intent, resultCode);
	} catch (ActivityNotFoundException e) {
	    Log.e(TAG, "Cant launch activity, " + e.getMessage());
	}
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedState)
    {
	SharedPreferences sp;

	super.onCreate(savedState);

	// Restore settings from previous invocation.
	final Object oldConfiguration = getLastNonConfigurationInstance();
	if( oldConfiguration != null )
	    restoreOldConfiguration(oldConfiguration);
	else if( savedState != null )
	    restoreInstanceState(savedState);
	else {
	    sp = PreferenceManager.getDefaultSharedPreferences(this);
	    if( sp != null )
		recallPreferences(sp);
	    Intent intent = getIntent();
	    if( intent != null ) {
		value = intent.getIntExtra("initial", 1013);
		barom = intent.getIntExtra("barom", 1013);
		convert = intent.getFloatExtra("convert", 1.0f);
		flingEnabled =
		  intent.getBooleanExtra("flingEnabled", flingEnabled);
	    }
	}

	setContentView(R.layout.kollsman);

	kview = (KollsmanView)findViewById(R.id.kview);
	okButton = (Button)findViewById(R.id.ok);

	// Make a guess as to the upper and lower limits
	int lower = barom > 2000 ? 2700 : 900;
	int upper = barom > 2000 ? 3300 : 1150;
	kview.setParams(value, lower, upper, barom, convert);

	okButton.setOnClickListener(new Button.OnClickListener() {
	    public void onClick(View v) {
		sendResult();
		finish();
	   }
	});
    }

    //-------------
    // SAVE/RESTORE
    //-------------

    @Override
    public Object onRetainNonConfigurationInstance() {
	value = kview.p;
	return this;
    }

    /**
     * Restore system variables from old instance of Kollsman
     */
    protected void restoreOldConfiguration(Object oldConfiguration) {
	Kollsman old = (Kollsman) oldConfiguration;
	value = old.value;
	barom = old.barom;
	convert = old.convert;
	flingEnabled = old.flingEnabled;
    }

    /**
     * Save system variables to bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle state) {
	value = kview.p;
	state.putInt("value", value);
	state.putInt("barom", barom);
	state.putFloat("convert", convert);
	state.putBoolean("flingEnabled", flingEnabled);
    }

    /**
     * Restore system variables from bundle
     */
    protected void restoreInstanceState(Bundle state) {
	value = state.getInt("value");
	barom = state.getInt("barom");
	convert = state.getFloat("convert");
	flingEnabled = state.getBoolean("flingEnabled");
    }

    /**
     * Load preferences when app starts up. User preferences are loaded
     * in their own function.
     */
    protected void recallPreferences(SharedPreferences sp)
    {
	flingEnabled = sp.getBoolean("flingEnabled", flingEnabled);
    }





    private void sendResult() {
	Intent intent = new Intent();
	intent.putExtra("value", kview.p);
	setResult(RESULT_OK, intent);
    }

    public static class KollsmanView extends View
	implements GestureDetector.OnGestureListener
    {
	private Paint paint;
	private int wid, hgt;
	private int p = 1013, p0 = 900, p1 = 1200;
	private int barom = 1013;
	private float value = 1013;	// float version of p, for scrolling
	private float aconv = 1;
	private float th, tspace;
	private float margin, pad;
	private GestureDetector detector;
	private boolean scrolling = false;
	private boolean flinging = false;
	private boolean settling = false;
	private int flingTime = 1000;		// ms
	private int settleTime = 250;	// ms
	private Globals g;
	private DisplayMetrics metrics;
	Handler animation;
	long animate_t0, animate_t1;
	float ady, ady0;

	public KollsmanView(Context ctx) {
	    super(ctx);
	    init(ctx, null);
	}

	public KollsmanView(Context ctx, AttributeSet attrs) {
	    super(ctx, attrs);
	    init(ctx, attrs);
	}

	public KollsmanView(Context ctx, AttributeSet attrs, int defStyle) {
	    super(ctx, attrs, defStyle);
	    init(ctx, attrs);
	}

	private void init(Context ctx, AttributeSet attrs)
	{
	    // Handle whatever common initialization is appropriate for
	    // this widget.
	    detector = new GestureDetector(ctx, this);
	    g = Globals.get((Activity)ctx);
	    animation = new Handler();

	    metrics = new DisplayMetrics();
	    ((Activity) ctx).getWindowManager()
		.getDefaultDisplay().getMetrics(metrics);
	    paint = new Paint();
	    //float ts = paint.getTextSize() * metrics.scaledDensity;
	    float ts = 18 * metrics.scaledDensity;
	    paint.setTextSize(ts);
	    th = -paint.ascent();
	    tspace = paint.getFontSpacing();

	    margin = 16 * metrics.scaledDensity;	// from bg drawable
	    pad = 2 * metrics.scaledDensity;
	}

	/**
	 * Set the drawing and scaling parameters.
	 * @param p      current pressure
	 * @param p0     minimum pressure to display
	 * @param p1     maximum pressure to display
	 * @param aconv  conversion factor meters -> whatever
	 *
	 * We use integer values for pressure and altitude to avoid the
	 * need for expensive formatting.
	 */
	public void setParams(int p, int p0, int p1, int barom, float aconv)
	{
	    // Fun trivia: Bar Yehuda Airfield on the shores of the Dead
	    // Sea is the lowest airport in the world, at -378 meters.
	    // Since it's unlikely that our aircraft could possibly be
	    // lower than this, we compute a new lower limit for p0.
	    int p00 = (int)Barometer.a2sealevel(barom, -380);
	    if (p0 < p00) p0 = p00;
	    if (p < p00) p = p00;
	    this.p = p;
	    this.value = p;
	    this.p0 = p0;
	    this.p1 = p1;
	    this.barom = barom;
	    this.aconv = 1f/aconv;
	}

	public void stopAnimations() {
	    stopFling();
	    stopSettle();
	}

	@Override
	protected void
	onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    int w = getSuggestedMinimumWidth();
	    int h = getSuggestedMinimumHeight();
	    int minWid = (int)paint.measureText("9999      -99999");
	    minWid += 16 * metrics.density * 2;
	    w = getDefaultSize(w, widthMeasureSpec);
	    h = getDefaultSize(h, heightMeasureSpec);
	    switch (MeasureSpec.getMode(widthMeasureSpec)) {
		case MeasureSpec.AT_MOST:
		    if (minWid < w) w = minWid;
		    break;
		case MeasureSpec.EXACTLY:
		case MeasureSpec.UNSPECIFIED: 
		    break;
	    }
	    setMeasuredDimension(w,h);
	}


	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh)
	{
	    wid = w;
	    hgt = h;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
	    super.onDraw(canvas);
	    float tx = margin + pad;
	    float ty = hgt/2 + th/2;	// position of text at the hairline

	    canvas.clipRect(margin, margin, wid-margin, hgt-margin,
		    Region.Op.REPLACE);
	    // Pressure values on the left
	    // Altitude values on the right
	    paint.setColor(Color.BLACK);
	    int ptop = (int)(value - (hgt/2 / tspace));
	    int pbot = (int)(value + (hgt/2 / tspace));
	    if (ptop < p0) ptop = p0;
	    if (pbot > p1) pbot = p1;
	    for (int i = ptop; i <= pbot; ++i) {
		float y = ty + (i-value) * tspace;
		canvas.drawText(""+i, tx, y, paint);
		float alt = Math.round(Barometer.p2a(i, barom) * aconv);
		String a = ""+(int)alt;
		float awid = paint.measureText(a);
		canvas.drawText(a, wid - margin - pad - awid, y, paint);
	    }

	    // The hairline
	    canvas.clipRect(0, 0, wid, hgt, Region.Op.REPLACE);
	    paint.setColor(Color.RED);
	    canvas.drawLine(0, hgt/2, wid-1, hgt/2, paint);
	}

	@Override
	protected void
	onDetachedFromWindow() {
	    stopAnimations();
	    super.onDetachedFromWindow();
	}

	// While in motion, there are three different kinds of motion
	// that can be on-going
	//  1) scroll: direct response to user drag on screen
	//  2) fling: fast animation that decays to a stop over one second
	//  3) settle: move to final location over 1/4 second
	// An ACTION_DOWN event stops all motions immediately.
	// A scroll beyond the end bounds drags to a stop
	// A fling beyond the end bounds drags to a stop over 1/4 second

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if( scrolling && event.getAction() == MotionEvent.ACTION_UP )
		scrollDone();
	    // Pass to gesture detector
	    return detector.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
	    stopFling();
	    stopSettle();
	    return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
		float dx, float dy)
	{
	    doScroll(dy);
	    return true;
	}

	@Override
	public final boolean
	onFling(MotionEvent e1, MotionEvent e2, float vx, float vy)
	{
	    if (!g.flingEnabled) return false;
	    if (vy > -50 && vy < 50) return false;
	    startFling(vy);
	    return true;
	}

	private void startFling(float vy) {
	    stopSettle();
	    animate_t0 = animate_t1 = System.currentTimeMillis();
	    ady = ady0 = vy;
	    flinging = true;
	    animation.post(animateCB);
	}

	private void stopFling() {
	    flinging = false;
	}

	/**
	 * Arrange for scroller to move from current value to given value
	 * over the course of 1/4 second.
	 */
	private void startSettle(float v) {
	    stopFling();
	    animate_t0 = animate_t1 = System.currentTimeMillis();
	    ady = ady0 = tspace * (value - v) / (settleTime/1000f);
	    settling = true;
	    animation.post(animateCB);
	}

	private void stopSettle() {
	    settling = false;
	}

	// Animate a fling over a period of one second
	// or a settle over 1/4 second.
	private final Runnable animateCB = new Runnable() {
	    public void run() {
		long now = System.currentTimeMillis();
		float dt = (now - animate_t1) * .001f;
		animate_t1 = now;
		if (flinging) {
		    doScroll(-ady*dt);
		    ady -= ady0 * dt;
		    if( now > animate_t0 + flingTime ) {
			flinging = false;
			scrollDone();
		    } else {
			animation.postDelayed(animateCB, 20);
		    }
		} else if (settling) {
		    if( now > animate_t0 + settleTime ) {
			settling = false;
			value = p;
			invalidate();
		    } else {
			doScroll(-ady*dt);
			animation.postDelayed(animateCB, 20);
		    }
		} else {
		}
	    }
	};

	private void doScroll(float dy) {
	    // Resist scrolling beyond limits
	    if (flinging && (value < p0 - 6 || value > p1 + 6)) {
		stopFling();
		scrollDone();
	    } else if (value < p0 && dy < 0) {
		dy *= (7-(p0-value)) / 7;
	    } else if (value > p1 && dy > 0) {
		dy *= (7-(value - p1)) / 7;
	    }
	    value += dy / tspace;
	    scrolling = true;
	    invalidate();
	}

	/**
	 * Scrolling finished, settle to the nearest integer value.
	 */
	private void scrollDone() {
	    p = (int)(value + .5);
	    if (p < p0) p = p0;
	    else if (p > p1) p = p1;
	    startSettle(p);
	}

	@Override
	public void onLongPress(MotionEvent e) { }

	@Override
	public void onShowPress(MotionEvent e) { }

	@Override
	public boolean onSingleTapUp(MotionEvent e) { return false; }
    }
}
