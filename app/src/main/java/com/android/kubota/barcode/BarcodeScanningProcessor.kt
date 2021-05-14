package com.android.kubota.barcode

import android.graphics.Bitmap
import android.util.Log
import com.android.kubota.camera.CameraImageGraphic
import com.android.kubota.camera.FrameMetadata
import com.android.kubota.camera.GraphicOverlay
import com.android.kubota.ui.flow.equipment.Barcode
import com.android.kubota.ui.flow.equipment.isValidEquipmentBarcode
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.IOException
import com.google.mlkit.vision.barcode.Barcode as MLBarcode

private const val TAG = "BarcodeScanProc"

/** Barcode Detector Demo.  */
class BarcodeScanningProcessor(
    private val barcodeListener: BarcodeListener
) : VisionProcessorBase<List<MLBarcode>>() {

    // Note that if you know which format of barcode your app is dealing with, detection will be
    // faster to specify the supported barcode formats one by one, e.g.
    // FirebaseVisionBarcodeDetectorOptions.Builder()
    //     .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
    //     .build()
    private val detector: BarcodeScanner by lazy {
        BarcodeScanning.getClient()
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Barcode Detector: $e")
        }
    }

    override fun detectInImage(image: InputImage): Task<List<MLBarcode>> {
        return detector.process(image)
    }

    override fun onSuccess(
        originalCameraImage: Bitmap?,
        barcodes: List<MLBarcode>,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay
    ) {
        val barcodeData = barcodes.map { Barcode.QR(it.rawValue ?: "") }
        val validBarCodes = barcodeData.filter { it.isValidEquipmentBarcode }

        if (validBarCodes.isNotEmpty()) {
            graphicOverlay.clear()

            originalCameraImage?.let {
                val imageGraphic = CameraImageGraphic(graphicOverlay, it)
                graphicOverlay.add(imageGraphic)
            }

            barcodes.forEach {
                val barcodeGraphic = BarcodeGraphic(graphicOverlay, it)
                graphicOverlay.add(barcodeGraphic)
            }
            graphicOverlay.postInvalidate()
            barcodeListener.onBarcodeDetected(barcodes = validBarCodes)
        } else if (barcodeData.size - validBarCodes.size > 0) {
            barcodeListener.onInvalidBarcode()
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Barcode detection failed $e")
    }

    interface BarcodeListener {
        fun onBarcodeDetected(barcodes: List<Barcode>)
        fun onInvalidBarcode()
    }
}