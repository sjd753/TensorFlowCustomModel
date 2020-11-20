package com.aggdirect.lens.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.utils.Exif
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aggdirect.lens.R
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

/** Helper type alias used for analysis use case callbacks */
typealias boundingBoxListener = (boundingBox: Bitmap) -> Unit


/**
 * A simple [Fragment] subclass.
 */
@SuppressLint("LogNotTimber")
class CameraPreviewFragment : Fragment() {

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName
        private const val PROCESS_DELAY = 600L
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    }

    // private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var classifier: ImageClassifier
    private lateinit var rootView: View

    // private var photoPath: String = ""

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var floatArray: FloatArray
    private lateinit var rawBitmap: Bitmap
    private lateinit var drawnLinesBitmap: Bitmap
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
            Log.e(TAG, "setOnClickListener")
            if (::rawBitmap.isInitialized && !rawBitmap.isRecycled && ::drawnLinesBitmap.isInitialized && !drawnLinesBitmap.isRecycled && ::floatArray.isInitialized) {
                // remove callbacks to get rid of pending preview and result difference
                view.viewFinder.removeCallbacks(runnable)

                try {
                    // merged bitmap
                    val mergedBitmap = BitmapHelper.drawMergedBitmap(rawBitmap, drawnLinesBitmap)

                    val bytes = BitmapHelper.compressedBitmapToByteArray(mergedBitmap, 70)

                    activity.setResult(
                        Activity.RESULT_OK,
                        Intent()
                            .putExtra("float_array", floatArray)
                            .putExtra("photo_bytes", bytes)
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
        startCamera(view)
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
        val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
        val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView.viewFinder.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (rootView.display.displayId == displayId) {
                val rotation = rootView.display.rotation
                // imageAnalysis.targetRotation = rotation
                imageCapture.targetRotation = rotation
                Log.e(TAG, "display rotation: $rotation")
            }
        }

        override fun onDisplayAdded(displayId: Int) {
        }

        override fun onDisplayRemoved(displayId: Int) {
        }
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                // Log.e(TAG, "orientation: $orientation rotation: $rotation")
                // imageAnalysis.targetRotation = rotation
                imageCapture.targetRotation = rotation
            }
        }
    }

    private fun startCamera(view: View) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(rootView.viewFinder.surfaceProvider)
                }

            // configure image capture
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_90)
                .build()

            // configure image analysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BoundingBoxAnalyzer { bitmap ->
                        Log.d(TAG, "BoundingBoxAnalyzer callback")
                        // process bitmap with correct orientation
                        processBitmap(bitmap)
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

                // perform scan now
                // performScan(view)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity))
    }

    private fun takePhoto() {
        Log.e(TAG, "takePhoto")
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

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.e(TAG, "onImageSaved")
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    // Toast.makeText(activity.applicationContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    // decode file to bitmap
                    // val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                    /*val compressedFile =
                        BitmapHelper.saveCompressedBitmap(bitmap, 90, photoFile.absolutePath)
                    val compressedBitmap = BitmapFactory.decodeFile(compressedFile.absolutePath)*/

                    val exif = Exif.createFromFile(photoFile)
                    val rotation = exif.rotation
                    Log.e(TAG, "exif rotation: $rotation")

                    // find orientation and rotate if required
                    // val orientation = BitmapHelper.findOrientation(compressedFile)
                    val rotatedBitmap =
                        BitmapHelper.rotateBitmap(bitmap, rotation.toFloat())
                    // process bitmap with correct orientation
                    processBitmap(rotatedBitmap)
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

    private fun performScan(view: View) {
        // view.viewFinder.postDelayed(runnable, PROCESS_DELAY)
        view.viewFinder.post(runnable)
    }

    private val runnable = Runnable {
        // takePhoto()
    }

    private fun processBitmap(rawBitmap: Bitmap) {
        Log.e(TAG, "processBitmap")
        // recycle bitmap if initialized
        if (this::rawBitmap.isInitialized) this.rawBitmap.recycle()
        this.rawBitmap = rawBitmap

        // tensor processing on raw bitmap
        floatArray = classifier.processTensor(activity, rawBitmap)

        // recycle bitmap if initialized
        if (this::drawnLinesBitmap.isInitialized) this.drawnLinesBitmap.recycle()
        // drawn line bitmap transparent background
        drawnLinesBitmap = BitmapHelper.drawBitmapByPoints(rawBitmap, floatArray)

        // rawBitmap.recycle()
        // drawnLinesBitmap.recycle()
        // update ui
        activity.runOnUiThread {
            rootView.ivBoundingBox.setImageBitmap(drawnLinesBitmap)
            Log.e(TAG, "setImageBitmap")
            // performScan(rootView)
        }
    }

    private class BoundingBoxAnalyzer(private val listener: boundingBoxListener) :
        ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        private fun jpegProxyToBitmap(image: ImageProxy): Bitmap {
            val buffer = image.planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val clonedBytes = bytes.clone()
            return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.size)
        }

        private fun yuv420888ProxyToBitmap(image: ImageProxy): Bitmap {
            val yBuffer = image.planes[0].buffer // Y
            val uBuffer = image.planes[1].buffer // U
            val vBuffer = image.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            //U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }

        override fun analyze(image: ImageProxy) {
            Log.e(TAG, "analyze: image format: ${image.format}")
            when (image.format) {
                ImageFormat.JPEG -> {
                    Log.e(TAG, "analyze: image format: JPEG")
                    val bitmap = jpegProxyToBitmap(image)
                    // rotation degrees
                    val rotation = image.imageInfo.rotationDegrees
                    Log.e(TAG, "analyze rotation: $rotation")
                    // find orientation and rotate if required
                    // val orientation = BitmapHelper.findOrientation(compressedFile)
                    val rotatedBitmap =
                        BitmapHelper.rotateBitmap(bitmap, rotation.toFloat())
                    // invoke listener
                    listener(rotatedBitmap)
                }
                ImageFormat.YUV_420_888 -> {
                    Log.e(TAG, "analyze: image format: YUV_420_888")
                    val bitmap = yuv420888ProxyToBitmap(image)
                    // rotation degrees
                    val rotation = image.imageInfo.rotationDegrees
                    Log.e(TAG, "analyze rotation: $rotation")
                    // find orientation and rotate if required
                    // val orientation = BitmapHelper.findOrientation(compressedFile)
                    val rotatedBitmap =
                        BitmapHelper.rotateBitmap(bitmap, rotation.toFloat())
                    // invoke listener
                    listener(rotatedBitmap)
                }
            }

            image.close()
        }
    }
}