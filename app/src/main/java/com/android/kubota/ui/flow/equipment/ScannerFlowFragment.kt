package com.android.kubota.ui.flow.equipment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import com.android.kubota.R
import com.android.kubota.barcode.BarcodeScanningProcessor
import com.android.kubota.camera.CameraSource
import com.android.kubota.camera.CameraSourcePreview
import com.android.kubota.camera.GraphicOverlay
import com.android.kubota.databinding.FragmentScannerBinding
import com.android.kubota.extensions.hasCameraPermissions
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.inmotionsoftware.flowkit.android.FlowFragment
import java.io.IOException

class ScannerFlowFragment: FlowFragment<Unit, ScannerFlowFragment.Result>() {

    companion object {
        private val TAG = "ScannerFlowFragment"
    }

    sealed class Result {
        object ManualEntry : Result()
        object Info : Result()
        object CameraPermission : Result()
        class ScannedBarcode(val code: Barcode) : Result()
    }

    private lateinit var overlay: GraphicOverlay
    private lateinit var preview: CameraSourcePreview

    private var b: FragmentScannerBinding? = null
    private val binding get() = b!!

    private val listener: BarcodeScanningProcessor.BarcodeListener =
        object : BarcodeScanningProcessor.BarcodeListener {
            override fun onBarcodeDetected(barcodes: List<FirebaseVisionBarcode>) {
                onScannedBarcodes(barcodes)
            }
        }

    private var cameraSource: CameraSource? = null
    private var toast: Toast? = null

    override fun onInputAttached(input: Unit) {
        if (this.cameraSource != null) {
            this.cameraSource?.release()
            this.cameraSource = null

            this.createCameraSource()
            if (this.requireActivity().hasCameraPermissions()) {
                this.startCameraSource()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        this.activity?.setTitle(R.string.add_equipment)
        this.b = FragmentScannerBinding.inflate(inflater)
        this.b?.btnManualEntry?.setOnClickListener {
            this.resolve(Result.ManualEntry)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.overlay = view.findViewById(R.id.fireFaceOverlay)
        this.preview = view.findViewById(R.id.firePreview)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.scanner_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.info -> {
                this.resolve(Result.Info)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        this.createCameraSource()
    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()
        this.preview.stop()
    }

    override fun onResume() {
        super.onResume()

        if (this.requireActivity().hasCameraPermissions()) {
            this.startCameraSource()
        } else {
            this.resolve(Result.CameraPermission)
        }
    }

    override fun onStop() {
        this.cameraSource?.release()
        this.cameraSource = null
        super.onStop()
    }

    override fun onDestroyView() {
        this.cameraSource?.release()
        this.preview.release()
        b = null
        super.onDestroyView()
    }

    private fun createCameraSource() {
        if (this.cameraSource != null) return

        this.cameraSource = CameraSource(requireActivity(), this.overlay)
        val processor = BarcodeScanningProcessor(this.listener)
        this.cameraSource?.setMachineLearningFrameProcessor(processor)
        this.cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
    }

    @Synchronized
    private fun onScannedBarcodes(barcodes: List<FirebaseVisionBarcode>) {
        for (barcode in barcodes) {
            val barcodeData = barcode.rawValue
            val qrCode = Barcode.QR(barcodeData ?: "")
            if (qrCode.isValidEquipmentBarcode) {
                this.cameraSource?.setMachineLearningFrameProcessor(null)
                this.preview.stop()

                this.resolve(Result.ScannedBarcode(qrCode))
                return
            }
        }

        this.showToast(this.getString(R.string.invalid_equipment_barcode))
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        this.cameraSource?.let {
            try {
                this.preview.start(it, this.overlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                it.release()
                this.cameraSource = null
            }
        }
    }

    private fun showToast(text: String) {
        this.toast?.cancel()
        this.toast = Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.CENTER, 0, 0)
        }
        this.toast?.show()
    }

}
