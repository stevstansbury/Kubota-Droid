package com.android.kubota.ui.equipment

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.databinding.FragmentTelematicsBinding
import com.android.kubota.R
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.utility.BitmapUtils
import com.android.kubota.utility.Constants
import com.android.kubota.viewmodel.equipment.TelematicsViewModel
import com.android.kubota.viewmodel.equipment.TelematicsViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kubota.service.api.KubotaServiceError
import java.lang.ref.WeakReference
import java.util.UUID


class TelematicsFragment: BaseBindingFragment<FragmentTelematicsBinding, TelematicsViewModel>() {

    private val equipmentId: UUID by lazy {
        UUID.fromString(arguments?.getString(EQUIPMENT_ID) ?: "")
    }

    override val layoutResId: Int = R.layout.fragment_telematics

    override val viewModel: TelematicsViewModel by lazy {
        ViewModelProvider(
            this,
            TelematicsViewModelFactory(
                application = requireApplication(),
                equipmentUnitId = equipmentId
            )
        )
            .get(TelematicsViewModel::class.java)
    }

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  super.onCreateView(inflater, container, savedInstanceState)
        binding.viewModel = viewModel

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentTransaction = childFragmentManager.beginTransaction()

        (childFragmentManager.findFragmentById(R.id.mapFragmentPane) as? SupportMapFragment)?.let { childFragment ->
            fragmentTransaction.remove(childFragment)
        }

        val mapOptions = GoogleMapOptions().apply {
            val latLng = LatLng(Constants.DEFAULT_MAP_LATITUDE, Constants.DEFAULT_MAP_LONGITUDE)
            camera(CameraPosition.fromLatLngZoom(latLng, Constants.DEFAULT_MAP_ZOOM))
        }

        fragmentTransaction
            .replace(R.id.mapFragmentPane, SupportMapFragment.newInstance(mapOptions))
            .commit()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        if (childFragment is SupportMapFragment) {
            getMapAsync(childFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.polling = true
    }

    override fun onPause() {
        super.onPause()
        viewModel.polling = false
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            viewModel.unitNickname.value?.let { activity?.title = it }
        }
    }

    override fun loadData() {
        viewModel.unitNickname.observe(this, Observer {
            activity?.title = it
        })

        viewModel.isLoading.observe(this, Observer {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(this, Observer { this.showError(it) })
    }

    private fun getMapAsync(supportMapFragment: SupportMapFragment) {
        supportMapFragment.getMapAsync {
            this.googleMap = it
            it.uiSettings.setAllGesturesEnabled(false)

            viewModel.unitLocation.observe(this, Observer {location ->
                val newLatLng = LatLng(
                    location?.location?.latitude ?: Constants.DEFAULT_MAP_LATITUDE,
                    location?.location?.longitude ?: Constants.DEFAULT_MAP_LONGITUDE
                )

                val bmp = BitmapUtils.createFromSvg(requireContext(), location.mapMarkerResId)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLng(newLatLng))
                googleMap?.addMarker(
                    MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                        .position(LatLng(newLatLng.latitude, newLatLng.longitude))
                        .draggable(false)
                )
            })
        }
    }

    companion object {
        fun createInstance(equipmentId: UUID): TelematicsFragment {
            return TelematicsFragment().apply {
                arguments = Bundle(1).apply {
                    putString(EQUIPMENT_ID, equipmentId.toString())
                }
            }
        }
    }
}

fun Fragment.requireApplication(): Application {
    return requireContext().applicationContext as Application
}