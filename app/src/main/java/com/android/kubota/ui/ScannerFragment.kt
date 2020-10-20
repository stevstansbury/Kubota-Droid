package com.android.kubota.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.barcode.BarcodeScanningProcessor
import com.android.kubota.camera.CameraSource
import com.android.kubota.camera.CameraSourcePreview
import com.android.kubota.camera.GraphicOverlay
import com.android.kubota.coordinator.AddEquipmentFlowCoordinator
import com.android.kubota.coordinator.OnboardUserFlowCoordinator
import com.android.kubota.databinding.FragmentScannerBinding
import com.android.kubota.ui.equipment.AddEquipmentFlow
import com.android.kubota.ui.equipment.AddEquipmentFragment
import com.android.kubota.utility.PermissionRequestManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import java.io.IOException
import java.util.UUID

class ScannerFragment : Fragment(), AddEquipmentFragment {
    companion object {
        private val TAG = "ScannerFragment"
        private const val SCANNER = "com.android.kubota.ui.ScannerFragment.SCANNER"
        private const val FIRST_TIME_SCAN = "com.android.kubota.ui.ScannerFragment.FIRST_TIME_SCAN"
        const val CAMERA_PERMISSION = 0
    }

    private val addEquipmentFlowActivity: AddEquipmentFlow by lazy { requireActivity() as AddEquipmentFlow }
    private var dialog: AlertDialog? = null
    private var b: FragmentScannerBinding? = null
    private val binding get() = b!!
    private lateinit var overlay: GraphicOverlay
    private lateinit var preview: CameraSourcePreview
    private val listener: BarcodeScanningProcessor.BarcodeListener = object : BarcodeScanningProcessor.BarcodeListener {
        override fun onBarcodeDetected(barcodes: List<FirebaseVisionBarcode>) {
            addEquipment(barcodes)
        }

    }

    private var cameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.add_equipment)
        b = FragmentScannerBinding.inflate(inflater)
        b?.btnManualEntry?.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentPane, EquipmentSearchFragment.newInstance())
                ?.addToBackStack(null)
                ?.commit()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        overlay = view.findViewById(R.id.fireFaceOverlay)
        preview = view.findViewById(R.id.firePreview)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.scanner_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.info -> {
                displayFirstTimeDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        showConditionalFirstTimeDialog()
    }

    /** Stops the camera.  */
    override fun onPause() {
        super.onPause()
        preview.stop()
        dialog?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (cameraSource != null) {
            checkPermissionsAndStartCameraSource()
        }
    }

    override fun onStop() {
        cameraSource?.release()
        super.onStop()
    }

    override fun onDestroyView() {
        cameraSource?.release()
        preview.release()
        b = null
        super.onDestroyView()
    }

    private fun showConditionalFirstTimeDialog() {
        val firstTimeScan =
            activity?.getSharedPreferences(SCANNER, MODE_PRIVATE)?.getBoolean(FIRST_TIME_SCAN, true)
                ?: true

        when (firstTimeScan) {
            true -> {
                activity?.getSharedPreferences(SCANNER, MODE_PRIVATE)?.edit()
                    ?.putBoolean(FIRST_TIME_SCAN, false)
                    ?.apply()

                displayFirstTimeDialog()
            }
            else -> checkPermissionsAndCreateCameraSource()
        }
    }

    private fun displayFirstTimeDialog() {
        AlertDialog.Builder(
            context,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
        )
            .setView(R.layout.dialog_machine_pin)
            .setCancelable(true)
            .setOnDismissListener {
                checkPermissionsAndCreateCameraSource()
            }
            .create().let {
                dialog = it
                dialog?.show()
                // This is required to get a fullscreen AlertDialog due to a platform bug
                activity?.window?.findViewById<View>(android.R.id.content)?.let { content ->
                    activity?.window?.findViewById<View>(android.R.id.statusBarBackground)?.let { statusBar ->
                        dialog?.findViewById<View>(android.R.id.custom)?.let { parentPanel ->
                            (parentPanel as View).layoutParams = FrameLayout.LayoutParams(
                                content.measuredWidth,
                                content.measuredHeight + statusBar.measuredHeight
                            )
                        }
                    }
                }
                dialog?.findViewById<ImageView>(R.id.btn_dismiss_dialog)
                    ?.setOnClickListener {
                        dialog?.dismiss()
                    }
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkPermissionsAndCreateCameraSource() {
        PermissionRequestManager.requestPermission(requireActivity(), Manifest.permission.CAMERA, R.string.accept_camera_permission)
            .done {
                createCameraSource()
            }
            .catch {
                // TODO...
            }
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), overlay)
            val processor = BarcodeScanningProcessor(listener)
            cameraSource?.setMachineLearningFrameProcessor(processor)
            cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
        }
    }

    @Synchronized
    private fun addEquipment(barcodes: List<FirebaseVisionBarcode>) {
        cameraSource?.setMachineLearningFrameProcessor(null)
        preview.stop()
        var addedEquipment = false

        for (barcode in barcodes) {
            val barcodeData = barcode.rawValue
            if (barcodeData != null && barcodeData.isBarcodeDataValid()) {
                val model = barcodeData.getModel()
                val equipmentUnitIdentifier = if (barcodeData.containsPin())
                    EquipmentUnitIdentifier.Pin
                else
                    EquipmentUnitIdentifier.Serial

                val pinOrSerial = barcodeData.getPinOrSerialNumber()
                val equipmentUnit = createEquipmentUnit(
                    identifierType = equipmentUnitIdentifier.name,
                    pinOrSerial = pinOrSerial,
                    model = model
                )

                addedEquipment = true
                addEquipmentFlowActivity.addEquipment(unit = equipmentUnit)
                break
            }
        }

        if (!addedEquipment) cameraSource?.setMachineLearningFrameProcessor(BarcodeScanningProcessor(listener))
    }

    private fun checkPermissionsAndStartCameraSource() {
        PermissionRequestManager.requestPermission(requireActivity(), Manifest.permission.CAMERA, R.string.accept_camera_permission)
            .done {
                startCameraSource()
            }
            .catch {
                // TODO...
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

    private fun createEquipmentUnit(identifierType: String, pinOrSerial: String?, model: String): EquipmentUnit {
        return EquipmentUnit(
            id = UUID.randomUUID(),
            model = model,
            category = null,
            identifierType = identifierType,
            pinOrSerial = pinOrSerial,
            pin = "",
            serial = null,
            nickName = null,
            userEnteredEngineHours = null,
            telematics = null,
            modelFullUrl = null,
            modelHeroUrl = null,
            modelIconUrl = null,
            guideUrl = null,
            manualInfo = emptyList(),
            warrantyUrl = null,
            hasFaultCodes = false,
            hasMaintenanceSchedules = false
        )
    }

    override fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE

    }

    override fun showError(throwable: Throwable) {
        val stringId = when (throwable) {
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                R.string.connectivity_error_message
            else ->
                R.string.server_error_message
        }

        Snackbar.make(preview, stringId, BaseTransientBottomBar.LENGTH_SHORT).show()
    }
}

private fun String.isBarcodeDataValid(): Boolean {
    return containsModel() && containsPinOrSerialNumber()
}

private fun String.getPinOrSerialNumber(): String {
    return if (this.containsPin()) {
        this
            .substringAfter("PIN")
            .substringBefore("\t")
    } else {
        this.substringAfter("\tSN")
            .substringBefore("\t")
    }
}

private fun String.getModel(): String {
    return this.substringAfter("\tWGN")
        .substringBefore("\t")
}

private fun String.containsPinOrSerialNumber() = containsPin() || containsSerialNumber()

private fun String.containsPin(): Boolean = this.contains("PIN")

private fun String.containsSerialNumber() = this.contains("\tSN")

private fun String.containsModel(): Boolean {
    return this.contains("\tWGN")
}
