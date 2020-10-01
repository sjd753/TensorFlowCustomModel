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
        private val KEY_FRAME_RECT = "FrameRect"
        val EXTRA_ADJUSTING_SETTING = "adjusting_setting"
    }

    // private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var classifier: ImageClassifier

    //    private var mFrameRect: RectF? = null
    private var photoPath: String = ""
    private var guideTogglePortrait = true
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

//        view.viewCropToggle.performClick()
//        view.viewCropToggle.setOnClickListener {
//            if (guideTogglePortrait) {
//                ivGuidelineDynamic.visibility = View.GONE
//                ivGuidelineStatic.visibility = View.VISIBLE
//                txtGuidelineToggle.text = "Portrait"
//                ivGuidelineToggle.setImageResource(R.drawable.ic_crop_portrait_black)
//            } else {
//                ivGuidelineDynamic.visibility = View.VISIBLE
//                ivGuidelineStatic.visibility = View.GONE
//                txtGuidelineToggle.text = "Square"
//                ivGuidelineToggle.setImageResource(R.drawable.ic_crop_square_black)
//            }
//            guideTogglePortrait = !guideTogglePortrait
//        }

        view.cameraView.setFocus(CameraKit.Constants.FOCUS_TAP_WITH_MARKER)
        view.btn_capture.setOnClickListener {
            if (::rawBitmap.isInitialized && !rawBitmap.isRecycled && ::drawnLinesBitmap.isInitialized && !drawnLinesBitmap.isRecycled) {
                val mergedBitmap = BitmapHelper.drawMergedBitmap(rawBitmap, drawnLinesBitmap)
                try {
                    val photoFile = BitmapHelper.bitmapToFile(
                        mergedBitmap,
                        AppFileManager.makeAppDir(context!!.getString(R.string.app_name))!!,
                        false
                    )
                    Log.e(TAG, "photoFile saved: ${photoFile.absolutePath}")
                    photoPath = photoFile.absolutePath
                    activity.setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra("photo_path", photoFile.absolutePath)
                    )
                    activity.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            /*cameraView.captureImage { cameraKitImage ->
                Log.e(TAG, "onImageCaptured: ")
                try {
                    // performScan()

                    *//*val photoFile = BitmapHelper.bitmapToFile(
                        rawBitmap,
                        AppFileManager.makeAppDir(context!!.getString(R.string.app_name))!!,
                        false
                    )
                    Log.e(TAG, "photoFile saved: ${photoFile.absolutePath}")
                    photoPath = photoFile.absolutePath*//*

                    *//*if (checkboxCompress.isChecked) {
                        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                        val compressedFile =
                            BitmapHelper.saveCompressedBitmap(bitmap, 90, photoFile.absolutePath)
                        photoPath = compressedFile.absolutePath
                        // Log.e(TAG, "compressed file size: ${AppFileManager.getFileSizeInKB(compressedFile)}")
                        //            val fileUri: Uri? = if (Build.VERSION.SDK_INT >= N) {
                        //                FileProvider.getUriForFile(activity, activity.packageName + activity.getString(R.string.app_file_provider_name), compressedFile)
                        //            } else {
                        //                Uri.fromFile(compressedFile)
                        //            }
                        // Log.e(TAG, compressedFile?.absolutePath)
                        // uploadFile(compressedFile.absolutePath)
                    } else {
                        // uploadFile(photoFile.absolutePath)
                    }*//*
                    *//*activity.setResult(
                        Activity.RESULT_OK,
                        Intent().apply { putExtra("photo_path", photoPath) })
                    activity.finish()*//*

                    // val bitmap = BitmapFactory.decodeFile(photoPath)
                    // val byteBuffer = classifier.getOutput(activity, bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }*/
        }

        view.btnGallery.setOnClickListener {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
//            if (photoPath.isNotEmpty()) {
//                val appDir = AppFileManager.makeAppDir(context!!.getString(R.string.app_name))
//
//                val fileUri: Uri? = if (Build.VERSION.SDK_INT >= N) {
//                    FileProvider.getUriForFile(activity, activity.packageName + activity.getString(R.string.app_file_provider_name), appDir!!)
//                } else {
//                    Uri.fromFile(appDir)
//                }
//
//                val intent = Intent().apply {
//                    action = Intent.ACTION_VIEW
//                    type = "image/*"
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
////                intent.setDataAndType(fileUri, "image/*")
//                intent.setDataAndType(Uri.withAppendedPath(fileUri, "/" + context!!.getString(R.string.app_name)), "image/*")
//                startActivity(intent)
//            }
        }

//        mFrameRect = cropImageView.actualCropRect
//        if (mFrameRect != null)
//            app.appSettings.saveRecrF(mFrameRect)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        view.cameraView.postDelayed({
            if (view.cameraView.isStarted) {
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
        }, 1000)
    }

    /*private fun loadBitmap(rawBitmap: Bitmap) {
        activity.runOnUiThread {
            val uri = BitmapHelper.bitmapToFileUri(context!!, rawBitmap, false)
            if (uri != null) {
                val loadRequest = cropImageView!!.load(uri).initialFrameScale(0.5f)
                loadRequest.initialFrameRect(cropImageView.actualCropRect)
                loadRequest.execute(object : LoadCallback {
                    override fun onSuccess() {
                        Log.e(TAG, "onLoaded: ")
                        crop(uri)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
            }
        }
    }

    private fun crop(uri: Uri?) {
        cropImageView!!.crop(uri).execute(object : CropCallback {
            override fun onSuccess(cropped: Bitmap) {
                Log.e(TAG, "onCropped: ")
                //use this bitmap for OCR
                // processOcr(cropped);
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }
        })
    }*/

    /*private void processOcr(final Bitmap bitmap) {
        if (getActivity() != null) {
            OcrProcessingThread processingThread = new OcrProcessingThread(getActivity().getAssets(), getString(R.string.app_name), bitmap, "eng");
            processingThread.process(new OcrScanListener() {
                @Override
                public void onScanResult(String scannedText) {
                    Log.e(TAG, "onScanResult: " + scannedText);
                    if (progressDialog != null && progressDialog.isShowing())
                        progressDialog.dismiss();
                    try {
                        app.getAppSettings().setOcrText(scannedText);
                        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Calendar.getInstance().getTime());
                        app.getAppSettings().setOcrTime(time);
                        File file = BitmapHelper.bitmapToFile(bitmap, AppFileManager.makeAppDir(getString(R.string.app_name)), false);
                        app.getAppSettings().setOcrImagePath(file.getPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (getActivity() != null) {
                        getActivity().setResult(RESULT_OK);
                        getActivity().finish();
                    }
                }
                @Override
                public void onScanFailure(Exception e) {
                    Log.e(TAG, "onScanFailure: ");
                    e.printStackTrace();
                }
            });
        }
    }*/
}