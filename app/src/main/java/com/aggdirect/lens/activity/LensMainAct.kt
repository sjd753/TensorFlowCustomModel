package com.aggdirect.lens.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aggdirect.lens.BuildConfig
import com.aggdirect.lens.R
import com.aggdirect.lens.application.LensApp
import com.aggdirect.lens.tensorflow.LensBoundingBoxDetector
import com.aggdirect.lens.utils.LensBitmapHelper
import com.github.buchandersenn.android_permission_manager.PermissionManager
import com.github.buchandersenn.android_permission_manager.PermissionRequest
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionCallback
import kotlinx.android.synthetic.main.lens_activity_main.*
import java.io.FileNotFoundException


class LensMainAct : AppCompatActivity() {

    companion object {
        private val TAG: String = LensMainAct::class.java.simpleName
        private const val RC_CHOOSE_GALLERY = 1001
        private const val RC_CHOOSE_CAMERA = 1002
        private const val RC_APPLY_TRANSFORM = 1003
    }

    private val permissionManager = PermissionManager.create(this)

    private lateinit var detector: LensBoundingBoxDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lens_activity_main)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        detector = LensBoundingBoxDetector(assets)

        val version = "version: ${BuildConfig.VERSION_NAME}"
        txtVersion.text = version
        cardGallery.setOnClickListener {
            choosePicture()
        }
        cardCamera.setOnClickListener {
            AlertDialog.Builder(this@LensMainAct)
                .setTitle("Note")
                .setMessage("Focus camera on document and avoid movement. Please place the document on a dark background for better results")
                .setPositiveButton(
                    "Proceed"
                ) { _, _ ->
                    startActivityForResult(
                        Intent(this@LensMainAct, LensCameraAct::class.java),
                        RC_CHOOSE_CAMERA
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .show()

        }
        // for lens module implementation open camera instantly
        startActivityForResult(
            Intent(this@LensMainAct, LensCameraAct::class.java),
            RC_CHOOSE_CAMERA
        )
    }

    override fun onStart() {
        super.onStart()
        requestPermissions()
    }

    private fun requestPermissions() {
        // Start building a new request using the with() method.
        // The method takes either a single permission or a list of permissions.
        // Specify multiple permissions in case you need to request both
        // read and write access to the contacts at the same time, for example.
        permissionManager.with(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
            // Optionally, specify a callback handler for all three callbacks
            .onCallback(object : OnPermissionCallback {
                override fun onPermissionShowRationale(permissionRequest: PermissionRequest) {
                    Log.i(TAG, "storage permission show rationale")
                    AlertDialog.Builder(this@LensMainAct).apply {
                        setMessage("Storage permission is required")
                        setCancelable(false)
                        setPositiveButton("OK") { dialog, which ->
                            permissionRequest.acceptPermissionRationale()
                        }
                    }.show()
                }

                override fun onPermissionGranted() {
                    Log.i(TAG, "storage permission granted")
                }

                override fun onPermissionDenied() {
                    Log.i(TAG, "storage permission denied")
                    AlertDialog.Builder(this@LensMainAct).setTitle("Permission Denied")
                        .setMessage("Enable storage permission from settings app")
                        .setCancelable(false)
                        .setPositiveButton("Ok") { _, _ ->
                            LensApp.startInstalledAppDetailsActivity(this@LensMainAct)
                        }
                        .setNegativeButton("CANCEL") { _, _ -> finish() }.show()
                }
            })
            // Finally, perform the request
            .request()
    }

    private fun choosePicture() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, RC_CHOOSE_GALLERY)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionManager.handlePermissionResult(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_CHOOSE_GALLERY && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val stream = contentResolver.openInputStream(data.data!!)
                val bitmap = BitmapFactory.decodeStream(stream)

                // display metrics
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels
                val width = displayMetrics.widthPixels

                val projectedHeight = width * bitmap.height / bitmap.width
                Log.e("result", "projectedHeight h: $projectedHeight")

                val scaled = Bitmap.createScaledBitmap(bitmap, width, projectedHeight, true)
                val floatArray = detector.processTensor(this@LensMainAct, scaled)

                // get bytes from compressed bitmap
                val compressedPhotoBytes = LensBitmapHelper.compressedBitmapToByteArray(scaled, 100)
                // save byte array as file
                val originalFile =
                    LensBitmapHelper.bytesToFile(this@LensMainAct, compressedPhotoBytes, false)
                // start polygon crop activity
                startActivity(Intent(this@LensMainAct, LensPolyCropAct::class.java).apply {
                    putExtra("float_array", floatArray)
                    putExtra("original_file_path", originalFile.absolutePath)
                })
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else if (requestCode == RC_CHOOSE_CAMERA && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val originalFilePath = data.getStringExtra("original_file_path")
                val scaledFloatArray = data.getFloatArrayExtra("float_array")
                val captureDuration = data.getLongExtra("capture_duration", 0L)
                Log.e(TAG, "float_array: ${scaledFloatArray?.toString()}")
                scaledFloatArray?.forEach {
                    Log.e(TAG, "float_array: $it")
                }
                scaledFloatArray?.let {
                    originalFilePath?.let {
                        // val scaled = LensBitmapHelper.bytesToBitmap(scaledPhotoBytes)
                        // get bytes from compressed bitmap
                        // val compressedPhotoBytes =
                        //    LensBitmapHelper.compressedBitmapToByteArray(scaled, 70)
                        // start polygon crop activity
                        startActivityForResult(
                            Intent(
                                this@LensMainAct,
                                LensPolyCropAct::class.java
                            ).apply {
                                putExtra("float_array", scaledFloatArray)
                                putExtra("original_file_path", originalFilePath)
                                putExtra("capture_duration", captureDuration)
                            }, RC_APPLY_TRANSFORM
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (requestCode == RC_APPLY_TRANSFORM && resultCode == Activity.RESULT_OK && data != null) {
            val originalFilePath = data.getStringExtra("original_file_path")
            val transformedFilePath = data.getStringExtra("transformed_file_path")
            val originalCoordinates = data.getFloatArrayExtra("original_coordinates")
            val adjustedCoordinates = data.getFloatArrayExtra("adjusted_coordinates")
            val captureDuration = data.getLongExtra("capture_duration", 0L)
            val transformedDuration = data.getLongExtra("transform_duration", 0L)

            Log.e(TAG, "originalFilePath: $originalFilePath")
            Log.e(TAG, "transformedFilePath: $transformedFilePath")
            /*originalCoordinates?.forEach {
                Log.e(TAG, "original_coordinates: $it")
            }
            adjustedCoordinates?.forEach {
                Log.e(TAG, "adjusted_coordinates: $it")
            }*/
            setResult(RESULT_OK, Intent().apply {
                putExtra("original_file_path", originalFilePath)
                putExtra("transformed_file_path", transformedFilePath)
                putExtra("original_coordinates", originalCoordinates)
                putExtra("adjusted_coordinates", adjustedCoordinates)
                putExtra("capture_duration", captureDuration)
                putExtra("transform_duration", transformedDuration)
            })
            finish()
        } else {
            // if result code do not match or result code not equals to RESULT_OK or data is null
            finish()
        }
    }
}