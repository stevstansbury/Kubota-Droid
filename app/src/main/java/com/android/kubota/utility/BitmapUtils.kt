package com.android.kubota.utility

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import android.hardware.Camera.CameraInfo
import android.graphics.ImageFormat.NV21
import android.util.Log
import com.android.kubota.camera.FrameMetadata
import com.google.android.gms.vision.Frame.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


object BitmapUtils {
    fun createFromSvg(context: Context, @DrawableRes drawableResId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableResId) as Drawable
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    // Convert NV21 format byte buffer to bitmap.
    fun getBitmap(data: ByteBuffer, metadata: FrameMetadata): Bitmap? {
        data.rewind()
        val imageInBuffer = ByteArray(data.limit())
        data.get(imageInBuffer, 0, imageInBuffer.size)
        try {
            val image = YuvImage(
                imageInBuffer,
                NV21,
                metadata.width,
                metadata.height,
                null
            )
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, metadata.width, metadata.height), 80, stream)

            val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())

            stream.close()
            return rotateBitmap(bmp, metadata.rotation, metadata.cameraFacing)
        } catch (e: Exception) {
            Log.e("VisionProcessorBase", "Error: " + e.message)
        }

        return null
    }

    // Rotates a bitmap if it is converted from a bytebuffer.
    private fun rotateBitmap(bitmap: Bitmap, rotation: Int, facing: Int): Bitmap {
        val matrix = Matrix()
        val rotationDegree = when (rotation) {
            ROTATION_90 -> 90f
            ROTATION_180 -> 180f
            ROTATION_270 -> 270f
            else -> 0f
        }

        // Rotate the image back to straight.}
        matrix.postRotate(rotationDegree)
        if (facing == CameraInfo.CAMERA_FACING_BACK) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            // Mirror the image along X axis for front-facing camera image.
            matrix.postScale(-1.0f, 1.0f)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}