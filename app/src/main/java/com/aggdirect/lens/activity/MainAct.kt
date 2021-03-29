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
import com.aggdirect.lens.application.App
import com.aggdirect.lens.tensorflow.BoundingBoxDetector
import com.aggdirect.lens.utils.BitmapHelper
import com.github.buchandersenn.android_permission_manager.PermissionManager
import com.github.buchandersenn.android_permission_manager.PermissionRequest
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionCallback
import kotlinx.android.synthetic.main.lens_activity_main.*
import java.io.FileNotFoundException


class MainAct : AppCompatActivity() {

    companion object {
        private val TAG: String = MainAct::class.java.simpleName
        private const val RC_CHOOSE_GALLERY = 1001
        private const val RC_CHOOSE_CAMERA = 1002
        private const val RC_APPLY_TRANSFORM = 1003
    }

    private val permissionManager = PermissionManager.create(this)

    private lateinit var detector: BoundingBoxDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lens_activity_main)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        detector = BoundingBoxDetector(assets)

        val version = "version: ${BuildConfig.VERSION_NAME}"
        txtVersion.text = version
        cardGallery.setOnClickListener {
            choosePicture()
        }
        cardCamera.setOnClickListener {
            AlertDialog.Builder(this@MainAct)
                .setTitle("Note")
                .setMessage("Focus camera on document and avoid movement. Please place the document on a dark background for better results")
                .setPositiveButton(
                    "Proceed"
                ) { _, _ ->
                    startActivityForResult(
                        Intent(this@MainAct, CameraAct::class.java),
                        RC_CHOOSE_CAMERA
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .show()

        }
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
                    AlertDialog.Builder(this@MainAct).apply {
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
                    AlertDialog.Builder(this@MainAct).setTitle("Permission Denied")
                        .setMessage("Enable storage permission from settings app")
                        .setCancelable(false)
                        .setPositiveButton("Ok") { _, _ ->
                            App.startInstalledAppDetailsActivity(this@MainAct)
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
                val floatArray = detector.processTensor(this@MainAct, scaled)

                // get bytes from compressed bitmap
                val compressedPhotoBytes = BitmapHelper.compressedBitmapToByteArray(scaled, 70)
                // start polygon crop activity
                startActivity(Intent(this@MainAct, PolyCropAct::class.java).apply {
                    putExtra("float_array", floatArray)
                    putExtra("photo_bytes", compressedPhotoBytes)
                })
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else if (requestCode == RC_CHOOSE_CAMERA && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val scaledPhotoBytes = data.getByteArrayExtra("photo_bytes")
                val scaledFloatArray = data.getFloatArrayExtra("float_array")

                scaledFloatArray?.let {
                    scaledPhotoBytes?.let {
                        val scaled = BitmapHelper.bytesToBitmap(scaledPhotoBytes)
                        // get bytes from compressed bitmap
                        val compressedPhotoBytes =
                            BitmapHelper.compressedBitmapToByteArray(scaled, 70)
                        // start polygon crop activity
                        startActivityForResult(Intent(this@MainAct, PolyCropAct::class.java).apply {
                            putExtra("float_array", scaledFloatArray)
                            putExtra("photo_bytes", compressedPhotoBytes)
                        }, RC_APPLY_TRANSFORM)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (requestCode == RC_APPLY_TRANSFORM && resultCode == Activity.RESULT_OK && data != null) {
            val originalBytes = data.getByteArrayExtra("original_bytes")
            val transformedBytes = data.getByteArrayExtra("transformed_bytes")
            setResult(RESULT_OK, Intent().apply {
                putExtra("original_bytes", originalBytes)
                putExtra("transformed_bytes", transformedBytes)
            })
            finish()
        }
    }
}