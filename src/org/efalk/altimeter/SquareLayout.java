/**
 * Variant of LinearLayout which maintains a specific aspect ratio.
 * Written by Jan Němec
 * http://stackoverflow.com/a/3147157/338479
 */

package org.efalk.altimeter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLayout extends LinearLayout {
    private static final float mScale = 1;

    public SquareLayout(Context context) {
        super(context);
    }

    public SquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > (int)(mScale * height + 0.5)) {
            width = (int)(mScale * height + 0.5);
        } else {
            height = (int)(width / mScale + 0.5);
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }
}
