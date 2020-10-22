package com.aggdirect.lens.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aggdirect.lens.R
import com.aggdirect.lens.application.AppFileManager
import com.aggdirect.lens.tensorflow.ImageClassifier
import com.aggdirect.lens.utils.BitmapHelper
import kotlinx.android.synthetic.main.fragment_camera_preview.view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * A simple [Fragment] subclass.
 */
@SuppressLint("LogNotTimber")
class CameraPreviewFragment : Fragment() {

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName
        private const val PROCESS_DELAY = 800L
    }

    // private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var classifier: ImageClassifier
    private lateinit var rootView: View

    private var photoPath: String = ""

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var floatArray: FloatArray
    private lateinit var mergedBitmap: Bitmap


    private val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    private lateinit var outputDirectory: File

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppCompatActivity) {
            activity = context
            classifier = ImageClassifier(activity.assets)
            outputDirectory =
                activity.externalCacheDir!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_camera_preview, container, false)

        view.btn_capture.setOnClickListener {
            if (::mergedBitmap.isInitialized && !mergedBitmap.isRecycled && ::floatArray.isInitialized) {
                // remove callbacks to get rid of pending preview and result difference
                view.viewFinder.removeCallbacks(runnable)

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

                    // val byteArray = BitmapHelper.compressedBitmapToByteArray(mergedBitmap, 70)

                    activity.setResult(
                        Activity.RESULT_OK,
                        Intent()
                            .putExtra("float_array", floatArray)
                            .putExtra("photo_path", photoPath)
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
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
        performScan(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView.viewFinder.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(rootView.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        /*imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                exception.printStackTrace()
                Log.e(TAG, "onError")
                Log.e(TAG, exception.toString())
            }

            @SuppressLint("UnsafeExperimentalUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                Log.e(TAG, "onCaptureSuccess")
                 val bitmap = imageProxyToBitmap(imageProxy)
//                val bitmap = imageProxy.image?.toBitmap()!!
                val byteArray = BitmapHelper.compressedBitmapToByteArray(bitmap, 70)
                // super.onCaptureSuccess(imageProxy)
                // imageProxy.close()
                activity.setResult(
                    Activity.RESULT_OK,
                    Intent()
                    // .putExtra("float_array", floatArray)
                     .putExtra("byte_array", byteArray)
                )
                activity.finish()
            }
        })*/

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    // Toast.makeText(activity.applicationContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    processBitmap(bitmap)
                }
            })
    }

    /**
     *  convert image proxy to bitmap
     *  @param image
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun performScan(view: View) {
        view.viewFinder.postDelayed(runnable, PROCESS_DELAY)
    }

    private val runnable = Runnable {
        takePhoto()
    }

    private fun processBitmap(rawBitmap: Bitmap) {
        // tensor processing on raw bitmap
        floatArray = classifier.processTensor(activity, rawBitmap)
        // todo: get the non computed float array
        // drawn line bitmap transparent background
        val drawnLinesBitmap = BitmapHelper.drawBitmapByPoints(rawBitmap, floatArray)
        // merged bitmap
        if (::mergedBitmap.isInitialized) mergedBitmap.recycle()
        mergedBitmap = BitmapHelper.drawMergedBitmap(rawBitmap, drawnLinesBitmap)

        // rawBitmap.recycle()
        // drawnLinesBitmap.recycle()
        // update ui
        activity.runOnUiThread {
            rootView.ivBoundingBox.setImageBitmap(drawnLinesBitmap)
            performScan(rootView)
        }
    }
}