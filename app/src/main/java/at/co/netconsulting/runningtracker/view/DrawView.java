package at.co.netconsulting.runningtracker.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawView extends View {
    Paint paint = new Paint();

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //canvas reactangular red bottom left
        paint.setStrokeWidth(3);
        paint.setColor(Color.RED);
        canvas.translate(0,canvas.getHeight());   // reset where 0,0 is located
        canvas.scale(1,-1);    // invert canvas so that it is aligned bottom left
        canvas.drawRect(33, 510, 77, 255, paint );
        //canvas reactangular yellow left middle bottom left
        paint.setColor(Color.YELLOW);
        canvas.drawRect(33, 765, 77, 510, paint );
        //canvas reactangular green left middle bottom left
        paint.setColor(Color.GREEN);
        canvas.drawRect(33, 1020, 77, 765, paint );
    }
}
