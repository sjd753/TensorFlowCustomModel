package com.aggdirect.lens.extras

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Magnifier
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.aggdirect.lens.BuildConfig
import com.aggdirect.lens.R
import java.util.*
import kotlin.math.abs

/**
 * Created by Sajjad on 23/11/20.
 */
class PolyCropLayout : FrameLayout {

    private inner class TouchableAppCompatImageView(context: Context) :
        AppCompatImageView(context) {
        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    private lateinit var paint: Paint
    private lateinit var pointer1: TouchableAppCompatImageView
    private lateinit var pointer2: TouchableAppCompatImageView
    private lateinit var pointer3: TouchableAppCompatImageView
    private lateinit var pointer4: TouchableAppCompatImageView
    private lateinit var midPointer13: TouchableAppCompatImageView
    private lateinit var midPointer12: TouchableAppCompatImageView
    private lateinit var midPointer34: TouchableAppCompatImageView
    private lateinit var midPointer24: TouchableAppCompatImageView
    private lateinit var polyCropLayout: PolyCropLayout
    private lateinit var magnifier: Magnifier
    private val cropLayoutTouchListener = LayoutTouchListenerImpl()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        polyCropLayout = this
        pointer1 = getTouchableImageView(0, 0)
        pointer2 = getTouchableImageView(width, 0)
        pointer3 = getTouchableImageView(0, height)
        pointer4 = getTouchableImageView(width, height)
        midPointer13 = getTouchableImageView(0, height / 2)
        midPointer13.setOnTouchListener(MidPointTouchListenerImpl(pointer1, pointer3))
        midPointer12 = getTouchableImageView(0, width / 2)
        midPointer12.setOnTouchListener(MidPointTouchListenerImpl(pointer1, pointer2))
        midPointer34 = getTouchableImageView(0, height / 2)
        midPointer34.setOnTouchListener(MidPointTouchListenerImpl(pointer3, pointer4))
        midPointer24 = getTouchableImageView(0, height / 2)
        midPointer24.setOnTouchListener(MidPointTouchListenerImpl(pointer2, pointer4))
        addView(pointer1)
        addView(pointer2)
        addView(midPointer13)
        addView(midPointer12)
        addView(midPointer34)
        addView(midPointer24)
        addView(pointer3)
        addView(pointer4)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                magnifier = Magnifier.Builder(polyCropLayout)
                    .setCornerRadius(24.0f)
                    .setElevation(16.0f)
                    .setSize(300, 150)
                    .setDefaultSourceToMagnifierOffset(0, -196)
                    .build()
            }
        }
        initPaint()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (BuildConfig.DEBUG) Log.e("PolyView", "onSizeChanged w: $w")
        if (BuildConfig.DEBUG) Log.e("PolyView", "onSizeChanged h: $h")
    }

    private fun initPaint() {
        paint = Paint()
        paint.color = ContextCompat.getColor(context, android.R.color.white)
        paint.strokeWidth = 4f
        paint.isAntiAlias = true
    }

    var points: Map<Int, PointF>
        get() {
            val points: MutableList<PointF> = ArrayList()
            points.add(PointF(pointer1.x + pointer1.width / 2, pointer1.y + pointer1.height / 2))
            points.add(PointF(pointer2.x + pointer2.width / 2, pointer2.y + pointer2.height / 2))
            points.add(PointF(pointer3.x + pointer3.width / 2, pointer3.y + pointer3.height / 2))
            points.add(PointF(pointer4.x + pointer4.width / 2, pointer4.y + pointer4.height / 2))
            return getOrderedPoints(points)
        }
        set(pointFMap) {
            if (pointFMap.size == 4) {
                setPointsCoordinates(pointFMap)
            }
        }

    private fun getOrderedPoints(points: List<PointF>): Map<Int, PointF> {
        val centerPoint = PointF()
        val size = points.size
        for (pointF in points) {
            centerPoint.x += pointF.x / size
            centerPoint.y += pointF.y / size
        }
        val orderedPoints: MutableMap<Int, PointF> = HashMap()
        for (pointF in points) {
            var index = -1
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3
            }
            orderedPoints[index] = pointF
        }
        return orderedPoints
    }

    private fun setPointsCoordinates(pointFMap: Map<Int, PointF>) {
        pointer1.x = pointFMap.getValue(0).x - pointer1.width / 2
        pointer1.y = pointFMap.getValue(0).y - pointer1.height / 2
        pointer2.x = pointFMap.getValue(1).x - pointer2.width / 2
        pointer2.y = pointFMap.getValue(1).y - pointer2.height / 2
        pointer3.x = pointFMap.getValue(2).x - pointer3.width / 2
        pointer3.y = pointFMap.getValue(2).y - pointer3.height / 2
        pointer4.x = pointFMap.getValue(3).x - pointer4.width / 2
        pointer4.y = pointFMap.getValue(3).y - pointer4.height / 2
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawLine(
            pointer1.x + pointer1.width / 2,
            pointer1.y + pointer1.height / 2,
            pointer3.x + pointer3.width / 2,
            pointer3.y + pointer3.height / 2,
            paint
        )
        canvas.drawLine(
            pointer1.x + pointer1.width / 2,
            pointer1.y + pointer1.height / 2,
            pointer2.x + pointer2.width / 2,
            pointer2.y + pointer2.height / 2,
            paint
        )
        canvas.drawLine(
            pointer2.x + pointer2.width / 2,
            pointer2.y + pointer2.height / 2,
            pointer4.x + pointer4.width / 2,
            pointer4.y + pointer4.height / 2,
            paint
        )
        canvas.drawLine(
            pointer3.x + pointer3.width / 2,
            pointer3.y + pointer3.height / 2,
            pointer4.x + pointer4.width / 2,
            pointer4.y + pointer4.height / 2,
            paint
        )
        midPointer13.x = pointer3.x - (pointer3.x - pointer1.x) / 2
        midPointer13.y = pointer3.y - (pointer3.y - pointer1.y) / 2
        midPointer24.x = pointer4.x - (pointer4.x - pointer2.x) / 2
        midPointer24.y = pointer4.y - (pointer4.y - pointer2.y) / 2
        midPointer34.x = pointer4.x - (pointer4.x - pointer3.x) / 2
        midPointer34.y = pointer4.y - (pointer4.y - pointer3.y) / 2
        midPointer12.x = pointer2.x - (pointer2.x - pointer1.x) / 2
        midPointer12.y = pointer2.y - (pointer2.y - pointer1.y) / 2
    }

    private fun getTouchableImageView(x: Int, y: Int): TouchableAppCompatImageView {
        val compatImageView = TouchableAppCompatImageView(context)
        val layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.NO_GRAVITY)
        compatImageView.layoutParams = layoutParams
        compatImageView.setImageResource(R.drawable.shape_circle_white_24dp)
        compatImageView.x = x.toFloat()
        compatImageView.y = y.toFloat()
        compatImageView.setOnTouchListener(TouchListenerImpl())
        return compatImageView
    }

    fun isValidShape(pointFMap: Map<Int, PointF>): Boolean {
        return pointFMap.size == 4
    }

    private inner class MidPointTouchListenerImpl(
        private val mainPointer1: ImageView?,
        private val mainPointer2: ImageView?
    ) : OnTouchListener {
        var downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            cropLayoutTouchListener.onTouch(polyCropLayout, event)
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (abs(mainPointer1!!.x - mainPointer2!!.x) > abs(
                            mainPointer1.y - mainPointer2.y
                        )
                    ) {
                        if (mainPointer2.y + mv.y + v.height < polyCropLayout.height && mainPointer2.y + mv.y > 0) {
                            v.x = (startPT.y + mv.y)
                            startPT = PointF(v.x, v.y)
                            mainPointer2.y = (mainPointer2.y + mv.y)
                        }
                        if (mainPointer1.y + mv.y + v.height < polyCropLayout.height && mainPointer1.y + mv.y > 0) {
                            v.x = (startPT.y + mv.y)
                            startPT = PointF(v.x, v.y)
                            mainPointer1.y = (mainPointer1.y + mv.y)
                        }
                    } else {
                        if (mainPointer2.x + mv.x + v.width < polyCropLayout.width && mainPointer2.x + mv.x > 0) {
                            v.x = (startPT.x + mv.x)
                            startPT = PointF(v.x, v.y)
                            mainPointer2.x = (mainPointer2.x + mv.x)
                        }
                        if (mainPointer1.x + mv.x + v.width < polyCropLayout.width && mainPointer1.x + mv.x > 0) {
                            v.x = (startPT.x + mv.x)
                            startPT = PointF(v.x, v.y)
                            mainPointer1.x = (mainPointer1.x + mv.x)
                        }
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    val color: Int = if (isValidShape(points)) {
                        ContextCompat.getColor(context, android.R.color.white)
                    } else {
                        ContextCompat.getColor(context, android.R.color.holo_red_light)
                    }
                    paint.color = color
                }
                else -> {
                }
            }
            polyCropLayout.invalidate()
            return true
        }
    }

    private inner class TouchListenerImpl : OnTouchListener {
        var downPT = PointF() // Record Mouse Position When Pressed Down
        var startPT = PointF() // Record Start Position of 'img'

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            cropLayoutTouchListener.onTouch(polyCropLayout, event)
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - downPT.x, event.y - downPT.y)
                    if (startPT.x + mv.x + v.width < polyCropLayout.width && startPT.y + mv.y + v.height < polyCropLayout.height && startPT.x + mv.x > 0 && startPT.y + mv.y > 0) {
                        v.x = (startPT.x + mv.x)
                        v.y = (startPT.y + mv.y)
                        startPT = PointF(v.x, v.y)
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    downPT.x = event.x
                    downPT.y = event.y
                    startPT = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    val color = if (isValidShape(points)) {
                        ContextCompat.getColor(context, android.R.color.white)
                    } else {
                        ContextCompat.getColor(context, android.R.color.holo_red_light)
                    }
                    paint.color = color
                }
                else -> {
                }
            }
            polyCropLayout.invalidate()
            return true
        }
    }

    private inner class LayoutTouchListenerImpl : OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (::magnifier.isInitialized) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                val viewPosition = IntArray(2)
                                v.getLocationOnScreen(viewPosition)
                                magnifier.show(
                                    event.rawX - viewPosition[0],
                                    event.rawY - viewPosition[1]
                                )
                                Log.e("onTouch", "rawX " + event.rawX)
                                Log.e("onTouch", "rawY " + event.rawY)
                                Log.e("onTouch", "viewPositionX " + viewPosition[0])
                                Log.e("onTouch", "viewPositionY " + viewPosition[1])
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (::magnifier.isInitialized) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            magnifier.dismiss()
                        }
                    }
                }
            }
            // polyCropLayout.invalidate()
            return true
        }
    }
}