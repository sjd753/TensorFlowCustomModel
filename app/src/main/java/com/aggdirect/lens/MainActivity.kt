package com.aggdirect.lens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.buchandersenn.android_permission_manager.PermissionManager
import com.github.buchandersenn.android_permission_manager.PermissionRequest
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionCallback
import kotlinx.android.synthetic.main.activity_image.*
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private val permissionManager = PermissionManager.create(this)
    private val CHOOSE_IMAGE = 1001
    private val CHOOSE_CAMERA = 1002
    private lateinit var photoImage: Bitmap
    private lateinit var classifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        classifier = ImageClassifier(assets)
        btnGallery.setOnClickListener {
            choosePicture()
        }
        btnCamera.setOnClickListener {
            startActivityForResult(
                Intent(this@MainActivity, CustomCameraActivity::class.java),
                CHOOSE_CAMERA
            )
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
                    AlertDialog.Builder(this@MainActivity).apply {
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
                    AlertDialog.Builder(this@MainActivity).setTitle("Permission Denied")
                        .setMessage("Enable storage permission from settings app")
                        .setCancelable(false)
                        .setPositiveButton("Ok") { _, _ ->
                            App.startInstalledAppDetailsActivity(this@MainActivity)
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
        startActivityForResult(intent, CHOOSE_IMAGE)
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
        if (requestCode == CHOOSE_IMAGE && resultCode == Activity.RESULT_OK && data != null) {

            try {
                val stream = contentResolver!!.openInputStream(data.data!!)
                if (::photoImage.isInitialized) photoImage.recycle()
                photoImage = BitmapFactory.decodeStream(stream)
//                photoImage =
//                    Bitmap.createScaledBitmap(photoImage, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
                // imageResult.setImageBitmap(photoImage)
//                classifier.getOutput(this@ImageActivity,photoImage)
//                val bitmap = classifier.getModelOutput(photoImage)
                val floatArray = classifier.getOutput(this@MainActivity, photoImage)
                val drawnBitmap = BitmapHelper.drawBitmapByPoints(photoImage, floatArray)
                val mergedBitmap = BitmapHelper.drawMergedBitmap(photoImage, drawnBitmap)
                imageResult.setImageBitmap(mergedBitmap)
//                 val bitmap = classifier.getOutputImage(byteBuffer)
//                val tensorImage = TensorImage.fromBitmap(bitmap)
//                 imageResult.setImageBitmap(bitmap)
//                classifier.recognizeImage(photoImage).subscribe(object : Consumer<List<Result>> {
//                    override fun accept(t: List<Result>) {
//                        txtResult.text = t.size.toString()
//                    }
//                })
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else if (requestCode == CHOOSE_CAMERA && resultCode == Activity.RESULT_OK && data != null) {
            val path = data.getStringExtra("photo_path")
            val bitmap = BitmapFactory.decodeFile(path)
            imageResult.setImageBitmap(bitmap)
            // val byteBuffer = classifier.getOutput(this@ImageActivity, bitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}