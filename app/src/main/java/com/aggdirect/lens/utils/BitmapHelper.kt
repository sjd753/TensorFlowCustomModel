package com.aggdirect.lens.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.N
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.aggdirect.lens.R
import com.aggdirect.lens.application.AppFileManager
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Sajjad Mistri on 24-01-2017.
 */

object BitmapHelper {

    private val TAG = BitmapHelper::class.java.simpleName

    @Synchronized
    @Throws(IOException::class)
    fun bitmapToFile(bitmap: Bitmap, dir: File, formatPNG: Boolean = false): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMAGE_$timeStamp"
        val suffix = if (formatPNG) ".png" else ".jpg"

        //create a file to write bitmap data
        val file = File.createTempFile(imageFileName, suffix, dir)

        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmap.compress(
            if (formatPNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
            100 /*ignored for PNG*/,
            bos
        )
        val bitmapData = bos.toByteArray()

        //write the bytes in file
        val fos = FileOutputStream(file)
        fos.write(bitmapData)
        fos.flush()
        fos.close()

        return file
    }

    fun bitmapToFileUri(context: Context, bitmap: Bitmap, formatPNG: Boolean): Uri? {
        try {
            val photoFile = bitmapToFile(
                bitmap,
                AppFileManager.makeAppDir(context.getString(R.string.app_name))!!,
                formatPNG
            )
            val uri: Uri?
            uri = if (Build.VERSION.SDK_INT >= N) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + context.getString(R.string.app_file_provider_name),
                    photoFile
                )
            } else {
                Uri.fromFile(photoFile)
            }
            if (uri != null) {
                Log.e(TAG, uri.path!!)
                return uri
            } else {
                Log.e(TAG, "Can not resolve Uri")
            }
        } catch (ex: Exception) {
            // Error occurred while creating the File
            ex.printStackTrace()
        }

        return null
    }

    fun bytesToBitmap(bitmapData: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
    }

    fun bytesToFile(context: Context, bitmapData: ByteArray, formatPNG: Boolean): File {
        val bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
        return bitmapToFile(
            bitmap,
            AppFileManager.makeAppDir(context.getString(R.string.app_name))!!,
            formatPNG
        )
    }

    fun bytesToFileUri(context: Context, bitmapData: ByteArray, formatPNG: Boolean): Uri? {
        val bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
        return bitmapToFileUri(context, bitmap, formatPNG)
    }

    fun cropBitmapByPoints(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        //        val bitmap =  Bitmap.create

        return Bitmap.createBitmap(bitmap, x, y, width, height)

        //        compositeImageView = (ImageView) findViewById (R.id.imageView);
        //
        //        Bitmap bitmap1 = BitmapFactory . decodeResource (getResources(), R.drawable.batman_ad);
        //        Bitmap bitmap2 = BitmapFactory . decodeResource (getResources(), R.drawable.logo);
        //
        //        Bitmap resultingImage = Bitmap . createBitmap (320, 480, bitmap1.getConfig());
        //
        //        Canvas canvas = new Canvas(resultingImage);
        //
        //        Paint paint = new Paint();
        //        paint.setAntiAlias(true);
        //        Path path = new Path();
        //        path.lineTo(150, 0);
        //        path.lineTo(230, 120);
        //        path.lineTo(70, 120);
        //        path.lineTo(150, 0);
        //
        //        canvas.drawPath(path, paint);
        //
        //        paint.setXfermode(new PorterDuffXfermode (Mode.SRC_IN));
        //        canvas.drawBitmap(bitmap2, 0, 0, paint);
        //
        //        compositeImageView.setImageBitmap(resultingImage);
    }

    fun drawBitmapByPoints(bitmap: Bitmap, array: FloatArray): Bitmap {
        if (array.isEmpty() || array.size != 8) throw IllegalArgumentException("float array must contain 8 elements")

        val resultingImage = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        //
        val canvas = Canvas(resultingImage)
        //
        val paint = Paint()
        paint.strokeWidth = 3f
        paint.pathEffect = null
        paint.color = Color.YELLOW
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true

        // val path = Path()
        // path.lineTo(array[0], array[1])
        // path.lineTo(array[2], array[3])
        // path.lineTo(array[4], array[5])
        // path.lineTo(array[6], array[7])

        // canvas.drawPath(path, paint)
        val arrayToDraw = FloatArray(16)
        // top left to top right
        arrayToDraw[0] = array[0]
        arrayToDraw[1] = array[1]
        arrayToDraw[2] = array[2]
        arrayToDraw[3] = array[3]
        // top right to bottom right
        arrayToDraw[4] = array[2]
        arrayToDraw[5] = array[3]
        arrayToDraw[6] = array[6]
        arrayToDraw[7] = array[7]
        // bottom right to bottom left
        arrayToDraw[8] = array[6]
        arrayToDraw[9] = array[7]
        arrayToDraw[10] = array[4]
        arrayToDraw[11] = array[5]
        // bottom left to top right
        arrayToDraw[12] = array[4]
        arrayToDraw[13] = array[5]
        arrayToDraw[14] = array[0]
        arrayToDraw[15] = array[1]

        canvas.drawLines(arrayToDraw, paint)

//        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
//        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return resultingImage
    }

    fun drawMergedBitmap(source: Bitmap, overlay: Bitmap): Bitmap {
        if (overlay.width > source.width || overlay.height > source.height) throw IllegalArgumentException(
            "overlay bitmap must be smaller than source bitmap"
        )

        val resultingImage = Bitmap.createBitmap(source.width, source.height, source.config)
        //
        val canvas = Canvas(resultingImage)
        //
        // val paint = Paint()
        // paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawBitmap(overlay, 0f, 0f, null)

        return resultingImage
    }

    @Synchronized
    @Throws(IOException::class)
    fun generateThumbnail(filePath: String): Bitmap? {
        return ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MINI_KIND)
    }

    fun saveCompressedBitmap(bitmap: Bitmap, quality: Int, path: String): File {
        val imageFile = File(path)

        val os: OutputStream
        try {
            os = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)
            os.flush()
            os.close()
        } catch (e: Exception) {
            Log.e(BitmapHelper::class.java.simpleName, "Error writing bitmap", e)
        }

        return imageFile
    }

//    fun findOrientation(file: File): Int {
//        var orientation = 0
//        val ei: ExifInterface?
//        try {
//            ei = ExifInterface(file.absolutePath)
//            orientation =
//                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
//            Log.e("Orientation", orientation.toString() + "")
//        } catch (e: IOException) {
//            e.printStackTrace()
//            orientation = -1
//            Log.e(BitmapHelper::class.java.simpleName, "Photo does not exists", e)
//        }
//
//        return orientation
//    }
//
//    fun rotateBitmap(source: Bitmap?, orientation: Int): Bitmap? {
//        when (orientation) {
//            ExifInterface.ORIENTATION_ROTATE_90 -> {
//                if (source != null)
//                    return rotateBitmap(source, 90f)
//                return source
//            }
//            ExifInterface.ORIENTATION_ROTATE_180 -> {
//                if (source != null)
//                    return rotateBitmap(source, 180f)
//                return source
//            }
//            ExifInterface.ORIENTATION_ROTATE_270 -> {
//                return if (source != null) rotateBitmap(source, 270f) else source
//            }
//            else -> return source
//        }
//    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        try {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(BitmapHelper::class.java.simpleName, "OOM..please try with smaller image", e)
        }

        return source
    }

    /**
     * @param bitmap  The source bitmap.
     * @param opacity a value between 0 (completely transparent) and 255 (completely opaque).
     * @return The opacity-adjusted bitmap.  If the source bitmap is mutable it will be
     * adjusted and returned, otherwise a new bitmap is created.
     */
    fun adjustOpacity(bitmap: Bitmap, opacity: Int): Bitmap {
        val mutableBitmap = if (bitmap.isMutable)
            bitmap
        else
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val colour = opacity and 0xFF shl 24
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN)
        return mutableBitmap
    }

    fun adjustAlpha(bitmap: Bitmap, alpha: Int): Bitmap {
        val alphaBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(alphaBitmap)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = Paint()
        paint.alpha = alpha                             //you can set your transparent value here
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return alphaBitmap
    }

    fun getUriFromDrawableResId(context: Context, drawableResId: Int): Uri {
        val builder = ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" +
                context.resources.getResourcePackageName(drawableResId) +
                "/" +
                context.resources.getResourceTypeName(drawableResId) +
                "/" +
                context.resources.getResourceEntryName(drawableResId)
        return Uri.parse(builder)
    }
}
