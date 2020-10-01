package com.aggdirect.lens.tensorflow

//import io.reactivex.Single

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.aggdirect.lens.tensorflow.Keys.DIM_BATCH_SIZE
import com.aggdirect.lens.tensorflow.Keys.DIM_IMG_SIZE_X
import com.aggdirect.lens.tensorflow.Keys.DIM_IMG_SIZE_Y
import com.aggdirect.lens.tensorflow.Keys.DIM_PIXEL_SIZE
import com.aggdirect.lens.tensorflow.Keys.MODEL_PATH
import org.checkerframework.checker.nullness.qual.NonNull
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class ImageClassifier constructor(private val assetManager: AssetManager) {

    private var interpreter: Interpreter? = null

    //    private var labelProb: Array<ByteArray>
//    private val labels = Vector<String>()
    private val intValues by lazy { IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y) }
    private var imgData: ByteBuffer

    init {
//        try {
//            val br = BufferedReader(InputStreamReader(assetManager.open(LABEL_PATH)))
//            while (true) {
//                val line = br.readLine() ?: break
//                labels.add(line)
//            }
//            br.close()
//        } catch (e: IOException) {
//            throw RuntimeException("Problem reading label file!", e)
//        }
//        labelProb = Array(1) { ByteArray(labels.size) }
        imgData =
            ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())
        try {
            interpreter = Interpreter(loadModelFile(assetManager, MODEL_PATH))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) return
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                imgData.put((value shr 16 and 0xFF).toByte())
                imgData.put((value shr 8 and 0xFF).toByte())
                imgData.put((value and 0xFF).toByte())
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

    fun getOutput(activity: AppCompatActivity, bitmap: Bitmap): FloatArray {
        // Initialization code
        // Create an ImageProcessor with all ops required. For more ops, please
        // refer to the ImageProcessor Architecture section in this README.

        // Initialization code
        // Create an ImageProcessor with all ops required. For more ops, please
        // refer to the ImageProcessor Architecture section in this README.
        val imageProcessor: ImageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(640, 360, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // Create a TensorImage object. This creates the tensor of the corresponding
        // tensor type (uint8 in this case) that the TensorFlow Lite interpreter needs.

        // Create a TensorImage object. This creates the tensor of the corresponding
        // tensor type (uint8 in this case) that the TensorFlow Lite interpreter needs.
        var tImage = TensorImage(DataType.FLOAT32)

        // Analysis code for every frame
        // Preprocess the image

        // Analysis code for every frame
        // Preprocess the image
        tImage.load(bitmap)
        tImage = imageProcessor.process(tImage)

        // Create a container for the result and specify that this is a quantized model.
        // Hence, the 'DataType' is defined as UINT8 (8-bit unsigned integer)

        // Create a container for the result and specify that this is a quantized model.
        // Hence, the 'DataType' is defined as UINT8 (8-bit unsigned integer)
        val probabilityBuffer =
            TensorBuffer.createFixedSize(intArrayOf(640, 360), DataType.FLOAT32)

//        val probabilityBuffer =
//            TensorBuffer.createDynamic(DataType.FLOAT32)
        // Initialise the model
        try {
            val tfliteModel = FileUtil.loadMappedFile(
                activity,
                MODEL_PATH
            )
            val tflite = Interpreter(tfliteModel)
//            tflite.allocateTensors()
            // Running inference
//            tflite.run(tImage.buffer, probabilityBuffer.buffer)
            Log.e("IC","bitmap width: " + bitmap.width)
            Log.e("IC","bitmap height: " + bitmap.height)
            val inputs = arrayOf(tImage.buffer, bitmap.height.toFloat(), bitmap.width.toFloat())
            val outputs = mutableMapOf<Int,ByteBuffer>()
            outputs[0] = probabilityBuffer.buffer
            tflite.runForMultipleInputsOutputs(inputs, outputs as @NonNull Map<Int, Any>)
            val array = FloatArray(8)
            for ((index, i) in (0..28 step 4).withIndex()) {
                val bufferResult = probabilityBuffer.buffer.getFloat(i)
                array[index] = bufferResult
                Log.e("buffer $index", bufferResult.toString())
            }
//            val intArray = getIntArray(probabilityBuffer.buffer)
//            Log.e("buffer", intArray.toString())
//            getIntArray(probabilityBuffer.buffer)
//            getByteArray(probabilityBuffer.buffer)
            return array
        } catch (e: IOException) {
            Log.e("tfliteSupport", "Error reading model", e)
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

    fun getModelOutput(bitmap: Bitmap): Bitmap {
        /* for (y in 0 until DIM_IMG_SIZE_Y) {
             for (x in 0 until DIM_IMG_SIZE_X) {
                 val px = bitmap.getPixel(x, y)

                 // Get channel values from the pixel value.
                 val r = Color.red(px)
                 val g = Color.green(px)
                 val b = Color.blue(px)

                 // Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
                 // For example, some models might require values to be normalized to the range
                 // [0.0, 1.0] instead.
                 val rf = (r - 127) / 255f
                 val gf = (g - 127) / 255f
                 val bf = (b - 127) / 255f

                 imgData.putFloat(rf)
                 imgData.putFloat(gf)
                 imgData.putFloat(bf)
             }
         }*/
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                imgData.put((value shr 16 and 0xFF).toByte())
                imgData.put((value shr 8 and 0xFF).toByte())
                imgData.put((value and 0xFF).toByte())
            }
        }
//        val bufferSize = 1000 * java.lang.Float.SIZE / java.lang.Byte.SIZE
        val modelOutput =
            ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
                .order(
                    ByteOrder.nativeOrder()
                )
        Log.e("output", modelOutput.toString())
        interpreter?.run(imgData, modelOutput)

        return getOutputImage(modelOutput)
    }

    fun getOutputImage(output: ByteBuffer): Bitmap {
        output.rewind() // Rewind the output buffer after running.

//        val yuvimage =
//            YuvImage(output.array(), ImageFormat.NV21, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, null)
//        val baos = ByteArrayOutputStream()
//        yuvimage.compressToJpeg(
//            Rect(0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y),
//            100,
//            baos
//        ) // Where 100 is the quality of the generated jpeg
//
//        val jpegArray: ByteArray = baos.toByteArray()
//        val bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)
//        return bitmap

//        val imageBytes = ByteArray(output.remaining())
//        output.get(imageBytes)
//        return BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)

//        val bitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, Bitmap.Config.ARGB_8888)
//        bitmap.copyPixelsFromBuffer(output)
//        return bitmap


        val bitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, Bitmap.Config.ARGB_8888)

        val pixels =
            IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y) // Set your expected output's height and width
        for (i in 0 until DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y) {
            val a = 0xFF
            val r: Float = output.float * 255.0f
            val g: Float = output.float * 255.0f
            val b: Float = output.float * 255.0f
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, DIM_IMG_SIZE_X, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)

        return bitmap
    }

//    fun recognizeImage(bitmap: Bitmap): Single<List<Result>> {
//        return Single.just(bitmap).flatMap {
//            convertBitmapToByteBuffer(it)
////            interpreter!!.run(imgData, labelProb)
//            val pq = PriorityQueue<Result>(3,
//                    Comparator<Result> { lhs, rhs ->
//                        // Intentionally reversed to put high confidence at the head of the queue.
//                        java.lang.Float.compare(rhs.confidence!!, lhs.confidence!!)
//                    })
////            for (i in labels.indices) {
////                pq.add(Result("" + i, if (labels.size > i) labels[i] else "unknown", labelProb[0][i].toFloat(), null))
////            }
//            val recognitions = ArrayList<Result>()
//            val recognitionsSize = Math.min(pq.size, MAX_RESULTS)
//            for (i in 0 until recognitionsSize) recognitions.add(pq.poll())
//            return@flatMap Single.just(recognitions)
//        }
//    }

    fun close() {
        interpreter?.close()
    }
}