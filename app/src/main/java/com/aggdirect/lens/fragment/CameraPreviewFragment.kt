package com.aggdirect.lens.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aggdirect.lens.R
import com.aggdirect.lens.application.AppFileManager
import com.aggdirect.lens.tensorflow.ImageClassifier
import com.aggdirect.lens.utils.BitmapHelper
import com.wonderkiln.camerakit.CameraKit
import kotlinx.android.synthetic.main.fragment_camera_preview.*
import kotlinx.android.synthetic.main.fragment_camera_preview.view.*


/**
 * A simple [Fragment] subclass.
 */
@SuppressLint("LogNotTimber")
class CameraPreviewFragment : Fragment() {

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName
        private const val PROCESS_DELAY = 600L
    }

    // private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var classifier: ImageClassifier
    private lateinit var rootView: View

    private var photoPath: String = ""

    private lateinit var rawBitmap: Bitmap
    private lateinit var drawnLinesBitmap: Bitmap

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppCompatActivity) {
            activity = context
//            app = context.application as App
//            app.appSettings = AppSettings(context)
            classifier = ImageClassifier(activity.assets)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_camera_preview, container, false)

        view.cameraView.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS)
        view.cameraView.setMethod(CameraKit.Constants.METHOD_STILL)

        view.btn_capture.setOnClickListener {
            if (::rawBitmap.isInitialized && !rawBitmap.isRecycled && ::drawnLinesBitmap.isInitialized && !drawnLinesBitmap.isRecycled) {
                // remove callbacks to get rid of pending preview and result difference
                view.cameraView.removeCallbacks(runnable)
                val mergedBitmap = BitmapHelper.drawMergedBitmap(rawBitmap, drawnLinesBitmap)
                try {
                    val photoFile = BitmapHelper.bitmapToFile(
                        mergedBitmap,
                        AppFileManager.makeAppDir(context!!.getString(R.string.app_name))!!,
                        false
                    )
                    Log.e(TAG, "photoFile saved: ${photoFile.absolutePath}")
                    photoPath = photoFile.absolutePath

                    // val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                    // val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                    // val compressedFile = BitmapHelper.saveCompressedBitmap(bitmap, 90, photoFile.absolutePath)

                    activity.setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra("photo_path", photoFile.absolutePath)
                    )
                    activity.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        view.btnGallery.setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
        performScan(view)
    }

    override fun onStart() {
        super.onStart()
        cameraView.start()
    }

    override fun onStop() {
        super.onStop()
        cameraView.stop()
    }

    private fun performScan(view: View) {
        view.cameraView.postDelayed(runnable, PROCESS_DELAY)
    }

    private val runnable = Runnable {
        if (rootView.cameraView.isStarted) {
            val view = rootView
            view.cameraView.captureImage { cameraKitImage ->
                if (::rawBitmap.isInitialized) rawBitmap.recycle()
                rawBitmap = cameraKitImage.bitmap

                val floatArray = classifier.processTensor(activity, rawBitmap)
                if (::drawnLinesBitmap.isInitialized) drawnLinesBitmap.recycle()
                drawnLinesBitmap = BitmapHelper.drawBitmapByPoints(rawBitmap, floatArray)
                activity.runOnUiThread {
                    view.ivBoundingBox.setImageBitmap(drawnLinesBitmap)
                    performScan(view)
                }
            }
        }
    }
}