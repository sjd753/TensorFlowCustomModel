package com.aggdirect.lens.activity

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aggdirect.lens.R
import com.aggdirect.lens.opencv.perspectiveTransform
import com.aggdirect.lens.utils.LensBitmapHelper
import kotlinx.android.synthetic.main.lens_activity_poly_crop.*
import org.opencv.android.OpenCVLoader
import org.opencv.core.Point
import java.io.ByteArrayOutputStream


class LensPolyCropAct : AppCompatActivity() {
    private lateinit var croppedBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lens_activity_poly_crop)

        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            OpenCVLoader.initDebug()
        }

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        if (intent.hasExtra("float_array") && intent.hasExtra("photo_bytes")) {
            val floatArray = intent.getFloatArrayExtra("float_array")!!
            val photoBytes = intent.getByteArrayExtra("photo_bytes")!!
            val captureDuration = intent.getLongExtra("capture_duration", 0L)
            frameLayout.post {
                // scaling not needed anymore as float array is generated on scaled bitmap
                // val scaledFloatArray = getScaledPoints(floatArray, photoBytes)
                // set point on the bitmap
                setPoints(floatArray, photoBytes)
//                val pointFs = polygonView.points
//                val originalCoordinates = floatArrayOf(
//                    pointFs.getValue(0).x,
//                    pointFs.getValue(0).y,
//                    pointFs.getValue(1).x,
//                    pointFs.getValue(1).y,
//                    pointFs.getValue(2).x,
//                    pointFs.getValue(2).y,
//                    pointFs.getValue(3).x,
//                    pointFs.getValue(3).y,
//                )
                // button click events
                btnCancel.setOnClickListener {
                    setResult(RESULT_CANCELED)
                    finish()
                }
                btnSaveOriginal.setOnClickListener {
                    val file = LensBitmapHelper.bytesToFile(this@LensPolyCropAct, photoBytes, false)
                    Toast.makeText(
                        this@LensPolyCropAct,
                        "File saved at ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                btnConfirm.setOnClickListener {
                    val bitmap = LensBitmapHelper.bytesToBitmap(photoBytes)
                    val pointFs = polygonView.points
                    val adjustedCoordinates = floatArrayOf(
                        pointFs.getValue(0).x,
                        pointFs.getValue(0).y,
                        pointFs.getValue(1).x,
                        pointFs.getValue(1).y,
                        pointFs.getValue(2).x,
                        pointFs.getValue(2).y,
                        pointFs.getValue(3).x,
                        pointFs.getValue(3).y,
                    )
                    val croppedBitmap =
                        LensBitmapHelper.drawBitmapByPoints(bitmap, adjustedCoordinates)
                    /*val file = BitmapHelper.bitmapToFile(
                        croppedBitmap,
                        AppFileManager.makeAppDir(getString(R.string.app_name))!!
                    )
                    Toast.makeText(
                        this@PolyCropAct,
                        "File saved at ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()*/
                    // set cropped bitmap and update buttons
                    // ivImage.setImageBitmap(croppedBitmap)
                    polygonView.visibility = View.GONE
                    btnSaveOriginal.visibility = View.GONE
                    btnConfirm.visibility = View.GONE
                    // btnApplyPT.visibility = View.VISIBLE
                    // Apply transformation on cropped bitmap
                    applyTransform(
                        croppedBitmap,
                        photoBytes,
                        originalCoordinates = floatArray,
                        adjustedCoordinates = adjustedCoordinates,
                        captureDuration = captureDuration
                    )
                    /*AlertDialog.Builder(this@PolyCropAct).setMessage("Apply Perspective Transform?")
                        .setPositiveButton("Apply") { dialog, which ->
                            val transformed = croppedBitmap.perspectiveTransform(
                                listOf(
                                    Point(
                                        pointFs.getValue(0).x.toDouble(),
                                        pointFs.getValue(0).y.toDouble()
                                    ),
                                    Point(
                                        pointFs.getValue(1).x.toDouble(),
                                        pointFs.getValue(1).y.toDouble()
                                    ),
                                    Point(
                                        pointFs.getValue(2).x.toDouble(),
                                        pointFs.getValue(2).y.toDouble()
                                    ),
                                    Point(
                                        pointFs.getValue(3).x.toDouble(),
                                        pointFs.getValue(3).y.toDouble()
                                    )
                                )
                            )
                            ivImage.setImageBitmap(transformed)
                        }
                        .setNegativeButton("Cancel") { dialog, which ->
                            dialog.dismiss()
                        }
                        .show()
                    // EXPERIMENTAL CODE*/
                }
            }
        }
    }

    private fun getScaledPoints(floatArray: FloatArray, photoBytes: ByteArray): FloatArray {
        // ##EXPERIMENTAL CODE
        val bitmap = LensBitmapHelper.bytesToBitmap(photoBytes)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        Log.e("result", "display w: $width")
        Log.e("result", "display h: $height")

        val projectedHeight = width * bitmap.height / bitmap.width

        val array = FloatArray(8)
        for ((index, float) in floatArray.withIndex()) {
            if (index % 2 == 0)
                array[index] = float * width / bitmap.width
            else
                array[index] = float * projectedHeight / bitmap.height
            Log.e("buffer $index", float.toString())
            Log.e("computed buffer $index", array[index].toString())
        }
        return array
    }

    private fun setPoints(scaledFloatArray: FloatArray, photoBytes: ByteArray) {
        val bitmap = LensBitmapHelper.bytesToBitmap(photoBytes)
        ivImage.setImageBitmap(bitmap)
        if (scaledFloatArray.isEmpty() || scaledFloatArray.size != 8) throw IllegalArgumentException(
            "float array must contain 8 elements"
        )

        val pointFs = mutableMapOf<Int, PointF>()
        pointFs[0] = PointF(scaledFloatArray[0], scaledFloatArray[1])
        pointFs[1] = PointF(scaledFloatArray[2], scaledFloatArray[3])
        pointFs[2] = PointF(scaledFloatArray[4], scaledFloatArray[5])
        pointFs[3] = PointF(scaledFloatArray[6], scaledFloatArray[7])
        // static points for test
        // pointFs[0] = PointF(245.11855F, 279.0646F)
        // pointFs[1] = PointF(715.1659F, 344.33545F)
        // pointFs[2] = PointF(201.82712F, 1155.0286F)
        // pointFs[3] = PointF(764.13245F, 1151.5259F)
        ivImage.post {
            polygonView.points = pointFs
            polygonView.invalidate()

            // val layoutParams = FrameLayout.LayoutParams(
            //     ivImage.width,
            //     ivImage.height
            // )
            // polygonView.layoutParams = layoutParams
            // polygonView.requestLayout()
        }
    }

    private fun scaleBitmapAtMost(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val matrix = Matrix()
        matrix.setRectToRect(
            RectF(
                0f,
                0f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat()
            ), RectF(0f, 0f, width.toFloat(), height.toFloat()), Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is MarginLayoutParams) {
            val marginLayoutParams = view.layoutParams as MarginLayoutParams
            marginLayoutParams.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    private fun applyTransform(
        croppedBitmap: Bitmap,
        photoBytes: ByteArray,
        originalCoordinates: FloatArray,
        adjustedCoordinates: FloatArray,
        captureDuration: Long
    ) {
        val transformStartTime = System.currentTimeMillis()
        val pointFs = polygonView.points
        val transformed = croppedBitmap.perspectiveTransform(
            listOf(
                Point(
                    pointFs.getValue(0).x.toDouble(),
                    pointFs.getValue(0).y.toDouble()
                ),
                Point(
                    pointFs.getValue(1).x.toDouble(),
                    pointFs.getValue(1).y.toDouble()
                ),
                Point(
                    pointFs.getValue(2).x.toDouble(),
                    pointFs.getValue(2).y.toDouble()
                ),
                Point(
                    pointFs.getValue(3).x.toDouble(),
                    pointFs.getValue(3).y.toDouble()
                )
            )
        )
        val transformedDuration = System.currentTimeMillis() - transformStartTime
        ivImage.setImageBitmap(transformed)
        btnApplyPT.visibility = View.GONE
        btnCancel.text = "Done"
        btnCancel.setOnClickListener {
            // save original bitmap as file
            // val originalFile = BitmapHelper.bytesToFile(this@PolyCropAct, photoBytes, false)
            // save transformed bitmap as file
            // val transformedFile = BitmapHelper.bitmapToFile(
            //    transformed,
            //    AppFileManager.makeAppDir(getString(R.string.app_name))!!
            // )
            val stream = ByteArrayOutputStream()
            transformed.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val transformedBytes = stream.toByteArray()

            val originalFile = LensBitmapHelper.bytesToFile(this@LensPolyCropAct, photoBytes, false)
            val transformedFile =
                LensBitmapHelper.bytesToFile(this@LensPolyCropAct, transformedBytes, false)

            setResult(RESULT_OK, Intent().apply {
                putExtra("original_file_path", originalFile.absolutePath)
                putExtra("transformed_file_path", transformedFile.absolutePath)
                putExtra("original_coordinates", originalCoordinates)
                putExtra("adjusted_coordinates", adjustedCoordinates)
                putExtra("capture_duration", captureDuration)
                putExtra("transform_duration", transformedDuration)
            })
            finish()
        }
    }
}