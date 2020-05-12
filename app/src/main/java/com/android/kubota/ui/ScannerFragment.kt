package com.android.kubota.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.barcode.BarcodeScanningProcessor
import com.android.kubota.camera.CameraSource
import com.android.kubota.camera.CameraSourcePreview
import com.android.kubota.camera.GraphicOverlay
import com.android.kubota.databinding.FragmentScannerBinding
import com.android.kubota.utility.Utils
import java.io.IOException

class ScannerFragment: Fragment() {
    companion object {
        private val TAG = "ScannerFragment"
        private const val SCANNER = "com.android.kubota.ui.ScannerFragment.SCANNER"
        private const val FIRST_TIME_SCAN = "com.android.kubota.ui.ScannerFragment.FIRST_TIME_SCAN"
        const val CAMERA_PERMISSION = 0
    }

    private var b: FragmentScannerBinding? = null
    private val binding get() = b!!
    private lateinit var overlay: GraphicOverlay
    private lateinit var preview: CameraSourcePreview

    private var cameraSource: CameraSource? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        b = FragmentScannerBinding.inflate(inflater)
        b?.btnManualEntry?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentPane, EquipmentSearchFragment.newInstance())
                ?.addToBackStack(null)
                ?.commit()
        }
        return b?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        overlay = view.findViewById(R.id.fireFaceOverlay)
        preview = view.findViewById(R.id.firePreview)
    }

    override fun onStart() {
        super.onStart()
        showConditionalFirstTimeDialog()
    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()
        preview.stop()
        Utils.showBottomNavigation(activity)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        if (cameraSource != null) {
            checkPermissionsAndStartCameraSource()
        }
        Utils.hideBottomNavigation(activity)
    }

    override fun onStop() {
        cameraSource?.release()
        super.onStop()
    }

    override fun onDestroy() {
        cameraSource?.release()
        b = null
        super.onDestroy()
    }

    private fun showConditionalFirstTimeDialog() {
        val firstTimeScan =
            activity?.getSharedPreferences(SCANNER, MODE_PRIVATE)?.getBoolean(FIRST_TIME_SCAN, true) ?: true

        when (firstTimeScan) {
            true -> {
                activity?.getSharedPreferences(SCANNER, MODE_PRIVATE)?.edit()
                ?.putBoolean(FIRST_TIME_SCAN, false)
                ?.apply()

                val dialog = AlertDialog.Builder(
                    context,
                    android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
                )
                    .setView(R.layout.dialog_machine_pin)
                    .setCancelable(true)
                    .setOnDismissListener {
                        checkPermissionsAndCreateCameraSource()
                    }
                    .create()
                dialog.show()
                dialog.findViewById<ImageView>(R.id.btn_dismiss_dialog)
                    .setOnClickListener {
                        dialog.dismiss()
                    }
            }
            else -> checkPermissionsAndCreateCameraSource()
        }
    }

    private fun checkPermissionsAndCreateCameraSource() {
        activity?.let {
            if (ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermissions()
            } else {
                createCameraSource()
            }
        }
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), overlay)
            cameraSource?.setMachineLearningFrameProcessor(BarcodeScanningProcessor())
            cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
        }
    }

    private fun checkPermissionsAndStartCameraSource() {
        activity?.let {
            if (ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermissions()
            } else {
                startCameraSource()
            }
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

    private fun requestCameraPermissions() {
        activity?.let {
            ActivityCompat.requestPermissions(
                it, arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION
            )
            it.supportFragmentManager.popBackStack()
        }
    }
}
