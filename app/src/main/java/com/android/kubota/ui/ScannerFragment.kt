package com.android.kubota.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.kubota.R
import com.android.kubota.barcode.BarcodeScanningProcessor
import com.android.kubota.camera.CameraSource
import com.android.kubota.camera.CameraSourcePreview
import com.android.kubota.camera.GraphicOverlay
import java.io.IOException

class ScannerFragment: BaseFragment() {
    companion object {
        private val TAG = "ScannerFragment"
    }

    private lateinit var overlay: GraphicOverlay
    private lateinit var preview: CameraSourcePreview

    private var cameraSource: CameraSource? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scanner, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        overlay = view.findViewById(R.id.fireFaceOverlay)
        preview = view.findViewById(R.id.firePreview)

        createCameraSource()
    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()

        preview.stop()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startCameraSource()
    }

    override fun onStop() {
        cameraSource?.release()
        super.onStop()
    }

    override fun onDestroy() {
        cameraSource?.release()
        super.onDestroy()
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), overlay)
            cameraSource?.setMachineLearningFrameProcessor(BarcodeScanningProcessor())
            cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        cameraSource?.let {
            try {
                preview.start(it, overlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

}