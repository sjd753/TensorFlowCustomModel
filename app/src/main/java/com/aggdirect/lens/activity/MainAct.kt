package com.aggdirect.lens.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aggdirect.lens.R
import com.aggdirect.lens.application.App
import com.aggdirect.lens.tensorflow.ImageClassifier
import com.aggdirect.lens.utils.BitmapHelper
import com.github.buchandersenn.android_permission_manager.PermissionManager
import com.github.buchandersenn.android_permission_manager.PermissionRequest
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException

class MainAct : AppCompatActivity() {

    companion object {
        private val TAG: String = MainAct::class.java.simpleName
        private const val RC_CHOOSE_GALLERY = 1001
        private const val RC_CHOOSE_CAMERA = 1002
    }

    private val permissionManager = PermissionManager.create(this)

    private lateinit var capturedBitmap: Bitmap
    private lateinit var classifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        classifier = ImageClassifier(assets)
        btnGallery.setOnClickListener {
            choosePicture()
        }
        btnCamera.setOnClickListener {
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
        requestStoragePermission()
    }

    private fun requestStoragePermission() {
        // Start building a new request using the with() method.
        // The method takes either a single permission or a list of permissions.
        // Specify multiple permissions in case you need to request both
        // read and write access to the contacts at the same time, for example.
        permissionManager.with(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
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
        if (resultCode == RESULT_OK) txtInfo.visibility = View.INVISIBLE
        if (requestCode == RC_CHOOSE_GALLERY && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val stream = contentResolver.openInputStream(data.data!!)
                // recycle bitmap
                if (::capturedBitmap.isInitialized) capturedBitmap.recycle()
                capturedBitmap = BitmapFactory.decodeStream(stream)

                val floatArray = classifier.processTensor(this@MainAct, capturedBitmap)
                val drawnBitmap = BitmapHelper.drawBitmapByPoints(capturedBitmap, floatArray)
                val mergedBitmap = BitmapHelper.drawMergedBitmap(capturedBitmap, drawnBitmap)
                imageResult.setImageBitmap(mergedBitmap)

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else if (requestCode == RC_CHOOSE_CAMERA && resultCode == Activity.RESULT_OK && data != null) {
            val path = data.getStringExtra("photo_path")
            val bitmap = BitmapFactory.decodeFile(path)
            imageResult.setImageBitmap(bitmap)
            // val byteBuffer = classifier.getOutput(this@ImageActivity, bitmap)
        }
    }
}