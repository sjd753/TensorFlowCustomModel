package com.aggdirect.lens.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.aggdirect.lens.R
import com.aggdirect.lens.tensorflow.LensBoundingBoxDetector
import com.aggdirect.lens.utils.LensBitmapHelper
import kotlinx.android.synthetic.main.lens_fragment_camera_preview.view.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Helper type alias used for analysis use case callbacks */
typealias boundingBoxListener = (boundingBox: Bitmap) -> Unit


/**
 * A simple [Fragment] subclass.
 */

class CameraPreviewFragment : Fragment() {

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName
        private const val FLASH_MODE_OFF = -1
        private const val FLASH_MODE_AUTO = 0
        private const val FLASH_MODE_TORCH = 1
    }

    // private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var detector: LensBoundingBoxDetector
    private lateinit var rootView: View

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var floatArray: FloatArray
    private lateinit var rawBitmap: Bitmap
    private lateinit var camera: Camera
    private var flashMode = FLASH_MODE_OFF // -1 = off, 0 = auto, 1 = on
    private var isTorchEnabled = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppCompatActivity) {
            activity = context
            detector = LensBoundingBoxDetector(activity.assets)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.lens_fragment_camera_preview, container, false)

        view.btn_capture.setOnClickListener {
            Log.e(TAG, "setOnClickListener")
            if (::rawBitmap.isInitialized && !rawBitmap.isRecycled && ::floatArray.isInitialized) {
                try {
                    if (flashMode == FLASH_MODE_AUTO) {
                        // check darkness
                        val isDark = LensBitmapHelper.isDark(rawBitmap)
                        // Toast.makeText(activity, "Bitmap is dark: $isDark", Toast.LENGTH_LONG).show()
                        if (isDark && !isTorchEnabled) {
                            camera.cameraControl.enableTorch(true)
                            isTorchEnabled = true
                            view.btn_capture.postDelayed({
                                view.btn_capture.performClick()
                                camera.cameraControl.enableTorch(false)
                                isTorchEnabled = false
                            }, 1000)
                        } else {
                            // get bytes from compressed bitmap
                            val bytes = LensBitmapHelper.compressedBitmapToByteArray(rawBitmap, 70)
                            // ser results and finish
                            activity.setResult(
                                Activity.RESULT_OK,
                                Intent()
                                    .putExtra("float_array", floatArray)
                                    .putExtra("photo_bytes", bytes)
                            )
                            activity.finish()
                        }
                    } else {
                        // merged bitmap
                        // val mergedBitmap = BitmapHelper.drawMergedBitmap(rawBitmap, drawnLinesBitmap)
                        // get bytes from compressed bitmap
                        val bytes = LensBitmapHelper.compressedBitmapToByteArray(rawBitmap, 70)
                        // ser results and finish
                        activity.setResult(
                            Activity.RESULT_OK,
                            Intent()
                                .putExtra("float_array", floatArray)
                                .putExtra("photo_bytes", bytes)
                        )
                        activity.finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        view.btnFlash.setOnClickListener {
            if (::camera.isInitialized && camera.cameraInfo.hasFlashUnit()) {
                // toggle flash modes
                if (flashMode == FLASH_MODE_TORCH) flashMode = FLASH_MODE_OFF else flashMode++
                var drawable = R.drawable.ic_flash_off
                when (flashMode) {
                    FLASH_MODE_OFF -> {
                        camera.cameraControl.enableTorch(false)
                        drawable = R.drawable.ic_flash_off
                    }
                    FLASH_MODE_AUTO -> {
                        camera.cameraControl.enableTorch(false)
                        drawable = R.drawable.ic_flash_auto
                    }
                    FLASH_MODE_TORCH -> {
                        camera.cameraControl.enableTorch(true)
                        drawable = R.drawable.ic_torch
                    }
                }
                view.btnFlash.setImageResource(drawable)
            }
        }

//        view.btnGallery.setOnClickListener {
//            val intent = Intent().apply {
//                action = Intent.ACTION_VIEW
//                type = "image/*"
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            }
//            startActivity(intent)
//        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (rootView.display.displayId == displayId) {
                val rotation = rootView.display.rotation
                // imageAnalysis.targetRotation = rotation
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
                // imageAnalysis.targetRotation = rotation
                // Log.e(TAG, "orientation: $orientation rotation: $rotation")
            }
        }
    }

    private fun startCamera() {
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
            // imageCapture = ImageCapture.Builder()
            //    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            //    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            //    .setTargetRotation(Surface.ROTATION_90)
            //    .build()

            // configure image analysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BoundingBoxAnalyzer(activity) { bitmap ->
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
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity))
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

    private fun processBitmap(rawBitmap: Bitmap) {
        Log.e(TAG, "processBitmap")
        // recycle bitmap if initialized
        if (this::rawBitmap.isInitialized) this.rawBitmap.recycle()
        this.rawBitmap = rawBitmap

        // tensor processing on raw bitmap
        floatArray = detector.processTensor(activity, rawBitmap)

        // drawn line bitmap transparent background
        val drawnLinesBitmap = LensBitmapHelper.drawLinesByPoints(rawBitmap, floatArray)

        // rawBitmap.recycle()
        // drawnLinesBitmap.recycle()
        // update ui
        activity.runOnUiThread {
            rootView.ivBoundingBox.setImageBitmap(drawnLinesBitmap)
            Log.e(TAG, "setImageBitmap")
        }
    }

    private class BoundingBoxAnalyzer(
        private val activity: AppCompatActivity,
        private val listener: boundingBoxListener
    ) :
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

        private fun createScaledBitmap(bitmap: Bitmap): Bitmap {
            // display metrics
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels

            val projectedHeight = width * bitmap.height / bitmap.width
            Log.e("result", "projectedHeight h: $projectedHeight")

            return Bitmap.createScaledBitmap(bitmap, width, projectedHeight, true)
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
                        LensBitmapHelper.rotateBitmap(bitmap, rotation.toFloat())
                    // scale bitmap according to screen resolution width
                    val scaled = createScaledBitmap(rotatedBitmap)
                    // invoke listener
                    listener(scaled)
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
                        LensBitmapHelper.rotateBitmap(bitmap, rotation.toFloat())
                    // scale bitmap according to screen resolution width
                    val scaled = createScaledBitmap(rotatedBitmap)
                    // invoke listener
                    listener(scaled)
                }
            }
            // close image proxy
            image.close()
        }
    }
}