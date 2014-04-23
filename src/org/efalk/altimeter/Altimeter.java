
package org.efalk.altimeter;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
//import android.view.MotionEvent;
import android.view.View;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Color;
import android.view.Gravity;


/**
 * Display altimeter. All units in meters and mB internally, but may
 * be converted for display.
 */
public class Altimeter extends View {

    private static final String TAG = AltimeterActivity.TAG;

    public static final int UNITS_FT = 0;
    public static final int UNITS_M = 1;
    public static final int UNITS_HG = 0;
    public static final int UNITS_MB = 1;

    public static final float SQRT2 = 1.41421356f;
    public static final float METER_FT = 3.2808399f;
    public static final float HG_MB = 29.92f/1013f;

    // These numbers are extremely specific to the underlying drawing
    private static final float KOLLSMAN_X = 0.91f;	// right edge
    private static final float KOLLSMAN_Y = 0.5f;	// center
    private static final float GAUGE_X = 0.09f;		// right edge
    private static final float GAUGE_Y = 0.5f;		// center

    private Paint paint, lblPaint;
    private int wid, hgt;
    private Activity ctx;
    private DisplayMetrics metrics;
    private int presUnits = UNITS_HG;
    private int altUnits = UNITS_FT;
    private float pressure = 1013;	// arbitrary
    private float kollsman = 1013;
    private float altitude = 0;		// meters
    private boolean inop = true;
    private Barometer barometer;
    private DecimalFormat fmt = new DecimalFormat("00.00");
    private float xc, yc;
    private float kx, ky, kw, kh, kp;	// Kollsman window
    private float gx, gy;	// Gauge
    private RectF rk;
    private Gauge gauge;

    public Altimeter(Context context) {
	super(context);
	init(context);
    }

    public Altimeter(Context context, AttributeSet attrs) {
	super(context, attrs);
	init(context);
    }

    public Altimeter(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	init(context);
    }

    public void setPresUnits(int units) {
	presUnits = units;
    }

    public void setAltUnits(int units) {
	if (units != altUnits) {
	    altUnits = units;
	    gauge = new Gauge(0, 50000,
	      units == UNITS_FT ? 10 : 5, lblPaint, true);
	    gauge.setXY((int)gx, (int)gy);
	}
    }

    /**
     * Set barometric pressure.
     * @param v    pressure, mB
     * @param now  timestamp, ns
     */
    public void setPressure(float v, long now) {
	pressure = v;
	altitude = barometer.p2aDamped(v, now);
	inop = false;
	invalidate();
    }

    /**
     * Set kollsman window value. v is in mB.
     */
    public void setKollsman(float v) {
	kollsman = v;
	barometer.setKollsman(v);
    }

    /**
     * Return last known pressure, mB
     */
    public float getPressure() {
	return pressure;
    }

    /**
     * Return last known kollsman setting, mB
     */
    public float getKollsman() {
	return kollsman;
    }

    private void init(Context context) {
	ctx = (Activity) context;
	metrics = new DisplayMetrics();
	ctx.getWindowManager().getDefaultDisplay().getMetrics(metrics);

	paint = new Paint();
	paint.setAntiAlias(true);
	paint.setDither(false);
	lblPaint = new Paint(paint);
	float ts = paint.getTextSize() * metrics.scaledDensity;
	paint.setTextSize(ts);
	lblPaint.setTextSize(ts * 1.5f);
	gauge = new Gauge(0, 50000, 10, lblPaint, true);

	barometer = new Barometer();
	barometer.setKollsman(kollsman);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh)
    {
	Log.d(TAG, "altimeter size " + w + "," + h);
	wid = w;
	hgt = h;
	xc = w/2;
	yc = h/2;
	kp = 5 * metrics.scaledDensity;		// padding
	kw = (int)lblPaint.measureText("29.92") + kp*2;
	kh = (int)-lblPaint.ascent() + kp*2;
	kx = wid * KOLLSMAN_X;
	ky = hgt * KOLLSMAN_Y;
	rk = new RectF(kx - kw, ky - kh/2, kx, ky + kh/2);
	gx = wid * GAUGE_X;
	gy = hgt * GAUGE_Y;
	gauge.setXY((int)gx, (int)gy);
    }

    // Main draw entry point
    @Override
    protected void onDraw(Canvas canvas)
    {
	super.onDraw(canvas);
	float alt = altitude;
	if (altUnits == UNITS_FT)
	    alt *= METER_FT;
	gauge.setValue(alt);
	gauge.draw(canvas);
	drawKollsman(canvas);
	if (inop)
	    drawInop(canvas);
	else {
	    // TODO: draw the hands
	}
	// TODO: look at the invalidated region, only redraw what's necessary.
    }


    private void drawKollsman(Canvas canvas) {
	String lbl;
	float ts = paint.getTextSize();
	paint.setTextSize(ts*1.5f);
	if (presUnits == UNITS_MB) {
	    lbl = "" + (int)kollsman;
	} else {
	    lbl = fmt.format(kollsman * HG_MB);
	}

	paint.setColor(Color.BLACK);
	paint.setStyle(Paint.Style.FILL);
	canvas.drawRect(rk, paint);
	paint.setColor(Color.WHITE);
	paint.setStyle(Paint.Style.STROKE);
	canvas.drawRect(rk, paint);
	paint.setStyle(Paint.Style.FILL);

	centerText(canvas, paint, lbl, kx-kp, ky,
	  Gravity.RIGHT|Gravity.CENTER_VERTICAL);

	paint.setTextSize(ts);
    }

    private void drawInop(Canvas canvas)
    {
	float w = lblPaint.measureText("INOP") + kp*2;
	float h = -lblPaint.ascent() + kp*2;
	RectF rect = new RectF(xc - w/2, yc - h/2, xc + w/2, yc + h/2);

	paint.setColor(Color.BLACK);
	paint.setStyle(Paint.Style.FILL);
	canvas.drawRect(rect, paint);
	paint.setColor(Color.WHITE);
	paint.setStyle(Paint.Style.STROKE);
	canvas.drawRect(rect, paint);

	lblPaint.setColor(Color.RED);
	centerText(canvas, lblPaint, "INOP", xc, yc, Gravity.CENTER);
	lblPaint.setColor(Color.WHITE);
    }

    /**
     * Draw centered text according to gravity.  If gravity is e.g.
     * LEFT|BOTTOM, then x,y are the coordinates of the lower-left
     * corner of the text bounding box.
     */
    public static void centerText(Canvas canvas, Paint paint, String str,
		    float x, float y, int gravity, boolean clearBg)
    {
	Rect bounds = new Rect();
	paint.getTextBounds(str, 0, str.length(), bounds);
	switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
	  case Gravity.RIGHT: x -= bounds.right; break;
	  case Gravity.CENTER_HORIZONTAL:
	    x -= (bounds.left + bounds.right) / 2;
	    break;
	  case Gravity.LEFT: x -= bounds.left; break;
	}
	switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
	  case Gravity.TOP: y -= bounds.top; break;
	  case Gravity.CENTER_VERTICAL:
	    y -= (bounds.bottom + bounds.top) / 2;
	    break;
	  case Gravity.BOTTOM: y -= bounds.bottom; break;
	}
	paint.setStyle(Paint.Style.FILL);
	if( clearBg ) {
	    int color = paint.getColor();
	    paint.setColor(Color.BLACK);
	    bounds.offset((int)x, (int)y);
	    canvas.drawRect(bounds, paint);
	    paint.setColor(color);
	}
	canvas.drawText(str, x, y, paint);
    }

    // Draw centered text according to gravity
    static void centerText(Canvas canvas, Paint paint, String str,
		    float x, float y, int gravity)
    {
	centerText(canvas, paint, str, x,y, gravity, false);
    }

}
