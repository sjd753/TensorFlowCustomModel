package com.aggdirect.lens.extras;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

import com.aggdirect.lens.BuildConfig;
import com.aggdirect.lens.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sajjad on 23/11/20.
 */
public class PolyCropLayout extends FrameLayout {

    protected Context context;
    private Paint paint;
    private ImageView pointer1;
    private ImageView pointer2;
    private ImageView pointer3;
    private ImageView pointer4;
    private ImageView midPointer13;
    private ImageView midPointer12;
    private ImageView midPointer34;
    private ImageView midPointer24;
    private PolyCropLayout polyCropLayout;

    public PolyCropLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public PolyCropLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public PolyCropLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        polyCropLayout = this;
        pointer1 = getImageView(0, 0);
        pointer2 = getImageView(getWidth(), 0);
        pointer3 = getImageView(0, getHeight());
        pointer4 = getImageView(getWidth(), getHeight());
        midPointer13 = getImageView(0, getHeight() / 2);
        midPointer13.setOnTouchListener(new MidPointTouchListenerImpl(pointer1, pointer3));

        midPointer12 = getImageView(0, getWidth() / 2);
        midPointer12.setOnTouchListener(new MidPointTouchListenerImpl(pointer1, pointer2));

        midPointer34 = getImageView(0, getHeight() / 2);
        midPointer34.setOnTouchListener(new MidPointTouchListenerImpl(pointer3, pointer4));

        midPointer24 = getImageView(0, getHeight() / 2);
        midPointer24.setOnTouchListener(new MidPointTouchListenerImpl(pointer2, pointer4));

        addView(pointer1);
        addView(pointer2);
        addView(midPointer13);
        addView(midPointer12);
        addView(midPointer34);
        addView(midPointer24);
        addView(pointer3);
        addView(pointer4);
        initPaint();
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (BuildConfig.DEBUG) Log.e("PolyView", "onSizeChanged w: " + w);
        if (BuildConfig.DEBUG) Log.e("PolyView", "onSizeChanged h: " + h);
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.white));
        paint.setStrokeWidth(4);
        paint.setAntiAlias(true);
    }

    public Map<Integer, PointF> getPoints() {

        List<PointF> points = new ArrayList<PointF>();
        points.add(new PointF(pointer1.getX(), pointer1.getY()));
        points.add(new PointF(pointer2.getX(), pointer2.getY()));
        points.add(new PointF(pointer3.getX(), pointer3.getY()));
        points.add(new PointF(pointer4.getX(), pointer4.getY()));

        return getOrderedPoints(points);
    }

    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {

        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        pointer1.setX(pointFMap.get(0).x - 12);
        pointer1.setY(pointFMap.get(0).y - 12);

        pointer2.setX(pointFMap.get(1).x - 12);
        pointer2.setY(pointFMap.get(1).y - 12);

        pointer3.setX(pointFMap.get(2).x - 12);
        pointer3.setY(pointFMap.get(2).y - 12);

        pointer4.setX(pointFMap.get(3).x - 12);
        pointer4.setY(pointFMap.get(3).y - 12);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawLine(pointer1.getX() + (pointer1.getWidth() / 2), pointer1.getY() + (pointer1.getHeight() / 2), pointer3.getX() + (pointer3.getWidth() / 2), pointer3.getY() + (pointer3.getHeight() / 2), paint);
        canvas.drawLine(pointer1.getX() + (pointer1.getWidth() / 2), pointer1.getY() + (pointer1.getHeight() / 2), pointer2.getX() + (pointer2.getWidth() / 2), pointer2.getY() + (pointer2.getHeight() / 2), paint);
        canvas.drawLine(pointer2.getX() + (pointer2.getWidth() / 2), pointer2.getY() + (pointer2.getHeight() / 2), pointer4.getX() + (pointer4.getWidth() / 2), pointer4.getY() + (pointer4.getHeight() / 2), paint);
        canvas.drawLine(pointer3.getX() + (pointer3.getWidth() / 2), pointer3.getY() + (pointer3.getHeight() / 2), pointer4.getX() + (pointer4.getWidth() / 2), pointer4.getY() + (pointer4.getHeight() / 2), paint);
        midPointer13.setX(pointer3.getX() - ((pointer3.getX() - pointer1.getX()) / 2));
        midPointer13.setY(pointer3.getY() - ((pointer3.getY() - pointer1.getY()) / 2));
        midPointer24.setX(pointer4.getX() - ((pointer4.getX() - pointer2.getX()) / 2));
        midPointer24.setY(pointer4.getY() - ((pointer4.getY() - pointer2.getY()) / 2));
        midPointer34.setX(pointer4.getX() - ((pointer4.getX() - pointer3.getX()) / 2));
        midPointer34.setY(pointer4.getY() - ((pointer4.getY() - pointer3.getY()) / 2));
        midPointer12.setX(pointer2.getX() - ((pointer2.getX() - pointer1.getX()) / 2));
        midPointer12.setY(pointer2.getY() - ((pointer2.getY() - pointer1.getY()) / 2));
    }


    private AppCompatImageView getImageView(int x, int y) {
        AppCompatImageView compatImageView = new AppCompatImageView(context);
        LayoutParams layoutParams = new LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.NO_GRAVITY);
        compatImageView.setLayoutParams(layoutParams);
        compatImageView.setImageResource(R.drawable.shape_circle_white_24dp);
        compatImageView.setX(x);
        compatImageView.setY(y);
        compatImageView.setOnTouchListener(new TouchListenerImpl());
        return compatImageView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }

    private class MidPointTouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        private ImageView mainPointer1;
        private ImageView mainPointer2;

        public MidPointTouchListenerImpl(ImageView mainPointer1, ImageView mainPointer2) {
            this.mainPointer1 = mainPointer1;
            this.mainPointer2 = mainPointer2;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);

                    if (Math.abs(mainPointer1.getX() - mainPointer2.getX()) > Math.abs(mainPointer1.getY() - mainPointer2.getY())) {
                        if (((mainPointer2.getY() + mv.y + v.getHeight() < polyCropLayout.getHeight()) && (mainPointer2.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setY((int) (mainPointer2.getY() + mv.y));
                        }
                        if (((mainPointer1.getY() + mv.y + v.getHeight() < polyCropLayout.getHeight()) && (mainPointer1.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setY((int) (mainPointer1.getY() + mv.y));
                        }
                    } else {
                        if ((mainPointer2.getX() + mv.x + v.getWidth() < polyCropLayout.getWidth()) && (mainPointer2.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setX((int) (mainPointer2.getX() + mv.x));
                        }
                        if ((mainPointer1.getX() + mv.x + v.getWidth() < polyCropLayout.getWidth()) && (mainPointer1.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setX((int) (mainPointer1.getX() + mv.x));
                        }
                    }

                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(android.R.color.white);
                    } else {
                        color = getResources().getColor(android.R.color.holo_red_light);
                    }
                    paint.setColor(color);
                    break;
                default:
                    break;
            }
            polyCropLayout.invalidate();
            return true;
        }
    }

    private class TouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                    if (((StartPT.x + mv.x + v.getWidth()) < polyCropLayout.getWidth() && (StartPT.y + mv.y + v.getHeight() < polyCropLayout.getHeight())) && ((StartPT.x + mv.x) > 0 && StartPT.y + mv.y > 0)) {
                        v.setX((int) (StartPT.x + mv.x));
                        v.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(v.getX(), v.getY());
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    if (isValidShape(getPoints())) {
                        color = getResources().getColor(android.R.color.white);
                    } else {
                        color = getResources().getColor(android.R.color.holo_red_light);
                    }
                    paint.setColor(color);
                    break;
                default:
                    break;
            }
            polyCropLayout.invalidate();
            return true;
        }
    }
}