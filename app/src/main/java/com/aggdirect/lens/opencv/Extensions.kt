package com.aggdirect.lens.opencv

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

fun Bitmap.perspectiveTransform(srcPoints: List<org.opencv.core.Point>):
        Bitmap {
    val dstWidth = max(
        srcPoints[0].distanceFrom(srcPoints[1]),
        srcPoints[2].distanceFrom(srcPoints[3])
    )
    val dstHeight = max(
        srcPoints[0].distanceFrom(srcPoints[2]),
        srcPoints[1].distanceFrom(srcPoints[3])
    )

    val dstPoints: List<org.opencv.core.Point> = listOf(
        org.opencv.core.Point(0.0, 0.0),
        org.opencv.core.Point(dstWidth, 0.0),
        org.opencv.core.Point(0.0, dstHeight),
        org.opencv.core.Point(dstWidth, dstHeight)
    )
    return try {
        val srcMat = Converters.vector_Point2f_to_Mat(srcPoints)
        val dstMat = Converters.vector_Point2f_to_Mat(dstPoints)
        val perspectiveTransformation =
            Imgproc.getPerspectiveTransform(srcMat, dstMat)
        val inputMat = Mat(this.height, this.width, CvType.CV_8UC1)
        Utils.bitmapToMat(this, inputMat)
        val outPutMat = Mat(dstHeight.toInt(), dstWidth.toInt(), CvType.CV_8UC1)
        Imgproc.warpPerspective(
            inputMat,
            outPutMat,
            perspectiveTransformation,
            Size(dstWidth, dstHeight)
        )
        val outPut = Bitmap.createBitmap(
            dstWidth.toInt(),
            dstHeight.toInt(), Bitmap.Config.RGB_565
        )
        //Imgproc.cvtColor(outPutMat , outPutMat , Imgproc.COLOR_GRAY2BGR)
        Utils.matToBitmap(outPutMat, outPut)
        outPut
    } catch (e: Exception) {
        e.printStackTrace()
        this
    }
}

fun org.opencv.core.Point.distanceFrom(srcPoint: org.opencv.core.Point):
        Double {
    val w1 = this.x - srcPoint.x
    val h1 = this.y - srcPoint.y
    val distance = w1.pow(2) + h1.pow(2)
    return sqrt(distance)
}