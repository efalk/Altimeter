
package org.efalk.altimeter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
//import android.util.Log;


/**
 * Draw a numeric gauge with scrolling digits. Float values are used
 * for smooth scrolling, but the gauge can only display integers.
 *        +--+
 *   ,+---+40|
 *  h | 12   |  y
 *   `*---+20|
 *        +--+
 *    x
 *
 * w includes full text plus padding plus arrow
 * h includes full text plus padding but not vertical extension
 */
class Gauge {
    //private static final String TAG = FlightDeck.TAG;
    private static final int PAD = 5;

    private int y;
    private int w, h;			// position in View
    private float xt, yt;		// right-bottom of text
    private float tw;			// Total text width
    private float tw2;			// Width of right-most digits
    private float dw;			// Width of a single digit
    private float th;			// Text height
    private float tpad;			// Text padding
    private float value = 0;
    private int step;			// increment of lowest-order digits
    private int stepDigits;		// how many digits is this?
    private int stepMod;		// 10^stepDigits
    private boolean allowNeg;		// Allow negative numbers

    private int arrow;		// arrow: -1=left, 0=none, 1=right
    private final Paint paint;
    private Path path = new Path();

    /**
     * Create a Gauge object.
     * @param arrow     Add arrow to end of gauge: -1=left, 0=none, 1=right
     * @param maxval    Maximum value gauge can display
     * @param step      Value increment
     * @param paint     Paint used to draw the gauge
     * @param allowNeg  Allow negative numbers
     */
    Gauge(int arrow, int maxval, int step, Paint paint, boolean allowNeg)
    {
	this.arrow = arrow;
	this.step = step;
	this.paint = paint;
	this.allowNeg = allowNeg;

	String s = "" + maxval;
	tw = paint.measureText(s);

	// Programming note: the text ascent includes some whitespace above
	// the text, which causes unbalanced display. We'll have to actually
	// measure the text.
	Rect bounds = new Rect();
	paint.getTextBounds(s, 0, s.length(), bounds);
	th = bounds.bottom - bounds.top;
	// Throw in some padding
	tpad = PAD;
	th += PAD*2;

	/*
	Log.d(TAG, "ascent = " + paint.ascent());
	Log.d(TAG, "descent = " + paint.descent());
	Log.d(TAG, "spacing = " + paint.getFontSpacing());
	Log.d(TAG, "s = " + s + ", len=" + s.length());
	Log.d(TAG, "measure = " + paint.measureText(s));
	Log.d(TAG, "bounds: left=" + bounds.left + ", bottom=" + bounds.bottom +
		    ", right=" + bounds.right + ", top=" + bounds.top);
	*/


	w = (int)(tw + PAD*2);
	h = (int)(th + PAD*2);
	if (arrow != 0) w += h/2;

	for (stepDigits = 0; step > 0; ++stepDigits) step /= 10;
	stepMod = 1;
	for (int i=0; i<stepDigits; ++i) stepMod *= 10;
	dw = paint.measureText("9");
	tw2 = dw * stepDigits;
    }

    /**
     * Return the total dimensions required by the gauge.
     */
    public int getWid() { return w; }
    public int getHgt() { return (int)(h + th); }

    /**
     * Set lower-left corner
     */
    public void setXY(int x, int y) {
	if (arrow < 0) x += h/2;
	this.y = y;

	// right-center of text
	xt = x + PAD + tw;
	yt = y + th/2 - tpad;

	// Draw the box outline
	path.rewind();
	path.moveTo(x, y+h/2);
	if (arrow < 0) {
	    path.rLineTo(-h/2, -h/2);
	    path.rLineTo(h/2, -h/2);
	} else {
	    path.rLineTo(0,-h);
	}
	path.rLineTo(tw - tw2 + PAD, 0);
	path.rLineTo(0, -th/2);
	path.rLineTo(tw2 + PAD, 0);
	if (arrow > 0) {
	    path.rLineTo(0, th/2);
	    path.rLineTo(h/2, h/2);
	    path.rLineTo(-h/2, h/2);
	    path.rLineTo(0, th/2);
	} else {
	    path.rLineTo(0, h + th);
	}
	path.rLineTo(-tw2 - PAD, 0);
	path.rLineTo(0, -th/2);

	path.close();
    }

    /**
     * Set value to be displayed
     */
    void setValue(float v) {
	value = v;
    }

    /**
     * Draw it.
     */
    void draw(Canvas canvas) {
	char[] lbl;

	canvas.save(Canvas.CLIP_SAVE_FLAG);

	paint.setColor(Color.BLACK);
	canvas.drawPath(path, paint);
	paint.setColor(Color.WHITE);
	paint.setStyle(Paint.Style.STROKE);
	canvas.drawPath(path, paint);
	paint.setStyle(Paint.Style.FILL);

	// TODO: how expensive is a complex clip path? We could
	// use two rectangular clips instead, one for the low-order
	// digits, and one for the high-order digits.

	canvas.clipPath(path);

	// Displaying the value. To display the low-order digits,
	// we round the value down to a multiple of step.
	// The delta between the value and the rounded value is
	// the scroll offset of the low-order digits. The scroll
	// value is the offset in pixels toward the bottom of the
	// screen.

	int ival = (int)(Math.floor(value / step) * step);
	float scroll = th * (value - ival) / step;

	// Display four values: the one closest to the true value (v1),
	// and the two above (v2,v3) and one below (v0) that. Numbers
	// increase towards the top of the display (mostly; the Garmin
	// G1000 goes the other way). Chances are, one of the four values
	// will be clipped completely.
	// v2 is significant: if it's zero, it indicates that higher-order
	// digits are scrolling too.

	float x = xt - tw2;

	// The value itself
	lbl = formatTrail(ival);
	canvas.drawText(lbl, 0, stepDigits, x, yt + scroll, paint);

	// The value above, v2
	int v2 = ival + step;
	lbl = formatTrail(v2);
	canvas.drawText(lbl, 0, stepDigits, x, yt + scroll - th, paint);

	// The value above, v3
	if (!clipped(yt + scroll - th*2)) {
	    int v3 = v2 + step;
	    lbl = formatTrail(v3);
	    canvas.drawText(lbl, 0, stepDigits, x, yt + scroll - th*2, paint);
	}

	// The value below, v0
	if (!clipped(yt + scroll + th)) {
	    int v0 = ival - step;
	    if (v0 >= 0 || allowNeg) {
		lbl = formatTrail(v0);
		canvas.drawText(lbl, 0, stepDigits, x, yt + scroll + th, paint);
	    }
	}

	// Now for the leading digits.

	// Scrolling the rest of the digits is easier. In general,
	// we'll be scrolling the digits down, and displaying the
	// current digit and the next higher one. Don't display leading
	// zeros.

	boolean negative = ival < 0;
	if (negative) ival = -ival;
	boolean doScroll;
	if (negative)
	    doScroll = ival % stepMod == 0;
	else
	    doScroll = v2 % stepMod == 0;
	ival /= stepMod;

	do {
	    int digit = ival % 10;
	    ival /= 10;
	    x -= dw;
	    if (doScroll) {
		if (!negative) {
		    canvas.drawText(format1(digit+1), 0,1,
			    x, yt + scroll - th, paint);
		    if (ival != 0 || digit != 0)
			canvas.drawText(format1(digit), 0,1,
				x, yt + scroll, paint);
		    doScroll = digit == 9;
		} else {
		    if (ival != 0 || digit != 0)
			canvas.drawText(format1(digit), 0,1,
				x, yt + scroll, paint);
		    if (ival != 0 || digit != 1)
			canvas.drawText(format1(digit-1), 0,1,
				x, yt + scroll - th, paint);
		    doScroll = digit == 0;
		}
	    } else {
		if (ival != 0 || digit != 0)
		    canvas.drawText(format1(digit), 0,1, x, yt, paint);
	    }
	} while (ival > 0 || doScroll);
	if (negative)
	    canvas.drawText("-", x-dw, yt, paint);

	canvas.restore();
    }

    private char[] fmtBuf = null;
    private static final char[] digits =
	{'0','1','2','3','4','5','6','7','8','9'};

    /**
     * Format the trailing digits. These require leading zeros.
     * Caller must ensure that v >= 0
     */
    private char[] formatTrail(int v) {
	if (v < 0) v = -v;
	if (fmtBuf == null) fmtBuf = new char[stepDigits];
	for (int i=stepDigits-1; i >= 0; --i) {
	    fmtBuf[i] = digits[v%10];
	    v /= 10;
	}
	return fmtBuf;
    }

    /**
     * Format one digit, mod 10.
     */
    private char[] format1(int v) {
	if (v < 0) v += 10;
	else if (v >= 10) v -= 10;
	fmtBuf[0] = digits[v];
	return fmtBuf;
    }

    /**
     * Return true if text based at y would be clipped
     */
    private boolean clipped(float yt) {
	if (yt < y - h/2 - th/2) return true;
	if (yt - th > y + h/2 + th/2) return true;
	return false;
    }
}
