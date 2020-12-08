package com.aggdirect.lens.tensorflow

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class BoundingBoxDetector constructor(private val assetManager: AssetManager) {

    companion object {
        const val MODEL_PATH = "ticket_scan_v2.tflite"

        const val INPUT_SIZE = 224
        const val MAX_RESULTS = 3
        const val DIM_BATCH_SIZE = 4
        const val DIM_PIXEL_SIZE = 3
        const val DIM_IMG_SIZE_X = 126
        const val DIM_IMG_SIZE_Y = 224
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        val intValues by lazy { IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y) }
        val byteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        byteBuffer.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                byteBuffer.put((value shr 16 and 0xFF).toByte())
                byteBuffer.put((value shr 8 and 0xFF).toByte())
                byteBuffer.put((value and 0xFF).toByte())
            }
        }
    }

    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun processTensor(activity: AppCompatActivity, bitmap: Bitmap): FloatArray {
        // Initialization code
        // Create an ImageProcessor with all ops required. For more ops, please
        // refer to the ImageProcessor Architecture section in this README.
        val imageProcessor: ImageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(DIM_IMG_SIZE_Y, DIM_IMG_SIZE_X, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        // Create a TensorImage object. This creates the tensor of the corresponding
        // tensor type (uint8 in this case) that the TensorFlow Lite interpreter needs.
        var tImage = TensorImage(DataType.FLOAT32)

        // Analysis code for every frame
        // Pre-process the image
        val inputBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
        tImage.load(inputBitmap)
        Log.e("processTensor", tImage.height.toString())
        Log.e("processTensor", tImage.width.toString())
        tImage = imageProcessor.process(tImage)

        // Create a container for the result and specify that this is a quantized model.
        // Hence, the 'DataType' is defined as UINT8 (8-bit unsigned integer)
        val outputBuffer =
            TensorBuffer.createFixedSize(
                intArrayOf(DIM_IMG_SIZE_Y, DIM_IMG_SIZE_X),
                DataType.FLOAT32
            )

        // Initialise the model
        try {
            val tfliteModel = FileUtil.loadMappedFile(
                activity,
                MODEL_PATH
            )
            val tfliteInterpreter = Interpreter(tfliteModel)

            // val inputs = arrayOf(tImage.buffer, bitmap.height.toFloat(), bitmap.width.toFloat())
            // val outputs = mutableMapOf<Int, ByteBuffer>()
            // outputs[0] = outputBuffer.buffer
            tfliteInterpreter.run(tImage.buffer, outputBuffer.buffer)
            // tfliteInterpreter.runForMultipleInputsOutputs(inputs, outputs as @NonNull Map<Int, Any>)
            val array = FloatArray(8)
            for ((index, i) in (0..28 step 4).withIndex()) {
                val bufferResult = outputBuffer.buffer.getFloat(i)
                if (index % 2 == 0)
                    array[index] = bufferResult * bitmap.width / DIM_IMG_SIZE_X
                else
                    array[index] = bufferResult * bitmap.height / DIM_IMG_SIZE_Y
                Log.e("buffer $index", bufferResult.toString())
                Log.e("computed buffer $index", array[index].toString())
            }
            tfliteInterpreter.close()
            return array
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("processTensor", "Error reading model", e)
        }

        return FloatArray(0)
    }

    fun getIntArray(byteBuffer: ByteBuffer): IntArray {
        byteBuffer.rewind()
        val array = IntArray(3686396)
        for (i in array.indices) {
            array[i] = byteBuffer.getInt(i)
        }
        Log.e("IntArray", array.map { it.toString() }.toString())
        return array
    }

    fun getByteArray(byteBuffer: ByteBuffer): ByteArray {
        byteBuffer.rewind()
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        Log.e("IntArray", byteArray.toString())
        return byteArray
    }
}