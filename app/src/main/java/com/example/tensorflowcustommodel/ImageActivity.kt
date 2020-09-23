package com.example.tensorflowcustommodel

//import io.reactivex.functions.Consumer
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tensorflowcustommodel.Keys.DIM_IMG_SIZE_X
import com.example.tensorflowcustommodel.Keys.DIM_IMG_SIZE_Y
import kotlinx.android.synthetic.main.activity_image.*
import java.io.FileNotFoundException

class ImageActivity : AppCompatActivity() {

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
                Intent(this@ImageActivity, CustomCameraActivity::class.java),
                CHOOSE_CAMERA
            )
        }
    }

    private fun choosePicture() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, CHOOSE_IMAGE)
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
                val floatArray = classifier.getOutput(this@ImageActivity, photoImage)
                val drawnBitmap = BitmapHelper.drawBitmapByPoints(photoImage,floatArray)
                imageResult.setImageBitmap(drawnBitmap)
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