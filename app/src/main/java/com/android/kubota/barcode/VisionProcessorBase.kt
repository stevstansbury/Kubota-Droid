package com.android.kubota.barcode

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.GuardedBy
import com.android.kubota.camera.FrameMetadata
import com.android.kubota.camera.GraphicOverlay
import com.android.kubota.utility.BitmapUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/** An interface to process the images with different ML Kit detectors and custom image models.  */
interface VisionImageProcessor {

    /** Processes the images with the underlying machine learning models.  */
    @Throws(MlKitException::class)
    fun process(data: ByteBuffer?, frameMetadata: FrameMetadata?, graphicOverlay: GraphicOverlay)

    /** Stops the underlying machine learning model and release resources.  */
    fun stop()
}

abstract class VisionProcessorBase<T>: VisionImageProcessor {
    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    @Synchronized
    override fun process(
        data: ByteBuffer?,
        frameMetadata: FrameMetadata?,
        graphicOverlay: GraphicOverlay
    ) {
        latestImage = data
        latestImageMetaData = frameMetadata
        if (processingImage == null && processingMetaData == null) {
            processLatestImage(graphicOverlay)
        }
    }

    @Synchronized
    private fun processLatestImage(graphicOverlay: GraphicOverlay) {
        processingImage = latestImage
        processingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        if (processingImage != null && processingMetaData != null) {
            processImage(processingImage!!, processingMetaData!!, graphicOverlay)
        }
    }

    private fun processImage(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay
    ) {
        val bitmap = BitmapUtils.getBitmap(data, frameMetadata)
        val image = InputImage.fromByteBuffer(
            data,
            frameMetadata.width,
            frameMetadata.height,
            0,
            ImageFormat.NV21
        )
        detectInVisionImage(
            bitmap,
            image,
            frameMetadata,
            graphicOverlay
        )
    }

    private fun detectInVisionImage(
        originalCameraImage: Bitmap?,
        image: InputImage,
        metadata: FrameMetadata?,
        graphicOverlay: GraphicOverlay
    ) {
        detectInImage(image)
            .addOnSuccessListener { results ->
                onSuccess(
                    originalCameraImage,
                    results,
                    metadata!!,
                    graphicOverlay
                )
                processLatestImage(graphicOverlay)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    override fun stop() {}

    protected abstract fun detectInImage(image: InputImage): Task<T>

    /**
     * Callback that executes with a successful detection result.
     *
     * @param originalCameraImage hold the original image from camera, used to draw the background
     * image.
     */
    protected abstract fun onSuccess(
        originalCameraImage: Bitmap?,
        results: T,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay
    )

    protected abstract fun onFailure(e: Exception)

}