package com.android.kubota.ui.geofence

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.dealer.toCoordinate
import com.android.kubota.utility.Constants
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.Geofence

private fun calculateRadius(map: GoogleMap): Double {
    // get 2 of the visible diagonal corners of the map (could also use farRight and nearLeft)
    val topLeft = map.projection.visibleRegion.farLeft
    val bottomRight = map.projection.visibleRegion.nearRight

    val diagonal = FloatArray(1)
    Location.distanceBetween(topLeft.latitude, topLeft.longitude, bottomRight.latitude, bottomRight.longitude, diagonal)
    // use a fraction of the view
    return diagonal.first() * 0.006
}

class GeofenceEditFragment : BaseFragment(), GoogleMap.OnCircleClickListener, GoogleMap.OnMapClickListener {

    companion object {
        private const val KEY_GEOFENCE = "geofence"

        fun createInstance(geofence: Geofence) =
            GeofenceEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_GEOFENCE, geofence)
                }
            }
    }

    override val layoutResId: Int = R.layout.fragment_geofence_edit

    private lateinit var progressBar: ProgressBar
    private lateinit var googleMap: GoogleMap
    private lateinit var saveButton: Button
    private lateinit var editIcon: ImageView
    private lateinit var geofenceName: TextView

    class GeofenceViewModel: ViewModel() {
        val points = MutableLiveData<List<LatLng>>()
        val polygon = MutableLiveData<Polygon?>()

        val geofence = MutableLiveData<Geofence>(Geofence())

        fun addPoint(latln: LatLng) {
            points.value = points.value?.plus(latln) ?: listOf(latln)
        }

        fun finish(lambda: (List<LatLng>) -> Polygon)  {
            val pnts = this.points.value
            if (pnts == null) return
            if (pnts.isEmpty()) return

            this.polygon.value?.remove()
            this.polygon.value = lambda(pnts)
            points.value = emptyList()
        }
    }

    private var circles = mutableListOf<Circle>()
    private var lines = mutableListOf<Polyline>()

    val viewModel: GeofenceViewModel by lazy { ViewModelProvider(requireActivity()).get(GeofenceViewModel::class.java) }

    override fun onMapClick(latln: LatLng?) {
        latln?.let { viewModel.addPoint(it) }
    }

    override fun onCircleClick(circle: Circle?) {
        if (circle == circles.first()) {
            viewModel.finish() {
                val pnts = it
                googleMap.addPolygon(PolygonOptions().apply {
                    addAll(pnts)
                    fillColor(ContextCompat.getColor(requireContext(), R.color.geofence_color_selected))
                    strokeWidth(0.0f)
                })
            }
        }
    }

    private fun setupMap(view: View) {

        val fragmentTransaction = childFragmentManager.beginTransaction()
        (childFragmentManager.findFragmentById(R.id.mapFragmentPane) as? SupportMapFragment)?.let { childFragment ->
            fragmentTransaction.remove(childFragment)
        }

        val mapOptions = GoogleMapOptions().apply {
            val latLng = LatLng(Constants.DEFAULT_MAP_LATITUDE, Constants.DEFAULT_MAP_LONGITUDE)
            camera(CameraPosition.fromLatLngZoom(latLng, Constants.DEFAULT_MAP_ZOOM))
        }

        val fragment = SupportMapFragment.newInstance(mapOptions)

        fragment.getMapAsync {
            this.googleMap = it.apply {
                uiSettings?.isRotateGesturesEnabled = false
                uiSettings?.isMapToolbarEnabled = false
                uiSettings?.isMyLocationButtonEnabled = false
                setOnMapClickListener(this@GeofenceEditFragment)
                setOnCircleClickListener(this@GeofenceEditFragment)
                setOnCameraMoveListener {
                    val radius = calculateRadius(map=googleMap)
                    circles.forEach { it.radius = radius }
                }
            }
        }

        fragmentTransaction
            .replace(R.id.mapFragmentPane, fragment)
            .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val name = GeofenceNameActivity.handleResult(requestCode, resultCode, data)
        this.viewModel.geofence.value?.let {
            this.viewModel.geofence.value = Geofence(
                uuid=it.uuid,
                name=name,
                points = it.points
            )
        }
    }

    override fun initUi(view: View) {
        progressBar = view.findViewById(R.id.toolbarProgressBar)
        saveButton = view.findViewById(R.id.saveButton)
        editIcon = view.findViewById(R.id.editIcon)
        geofenceName = view.findViewById(R.id.geofenceName)

        setupMap(view)

        this.editIcon.setOnClickListener {
            viewModel.geofence.value?.let {
                GeofenceNameActivity.launch(this, it.name)
            }
        }

        this.saveButton.isEnabled = false
        this.saveButton.setOnClickListener {
            val geofence = this.viewModel.geofence.value
            if (geofence == null) return@setOnClickListener

            this.showProgressBar()
            AppProxy.proxy.serviceManager.userPreferenceService
                .updateGeofence(geofence=geofence)
                .done {
                    // TODO:
                    parentFragmentManager.popBackStack()
                }
                .catch { this.showError(it) }
                .finally { this.hideProgressBar() }

        }

        this.viewModel.polygon.observe(viewLifecycleOwner, Observer {
            this.saveButton.isEnabled = (it != null)

            this.viewModel.geofence.value?.let {
                val points = this.viewModel.points.value?.map { it.toCoordinate() }
                this.viewModel.geofence.value = Geofence(
                    uuid=it.uuid,
                    name=it.name,
                    points = points?.toMutableList() ?: mutableListOf()
                )
            }
        })
    }

    override fun loadData() {
        this.viewModel.geofence.value = this.arguments?.getParcelable(KEY_GEOFENCE)

        this.viewModel.geofence.observe(viewLifecycleOwner, Observer {
            this.geofenceName.text = it.name
        })

        this.viewModel.points.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                circles.forEach { it.remove() }
                circles.clear()

                lines.forEach { it.remove() }
                lines.clear()
            } else {
                val radius = calculateRadius(map=googleMap)
                val latln = it.last()
                val circle = googleMap.addCircle(CircleOptions().apply {
                    center(latln)
                    clickable(circles.isEmpty())
                    strokeWidth(0.0f)
                    fillColor(ContextCompat.getColor(requireContext(), R.color.geofence_color_selected))
                    radius(radius)
                })
                circles.add(circle)

                if (it.size > 1) {
                    val start = it[it.size-2]
                    val line = googleMap.addPolyline(PolylineOptions().apply {
                        add(start)
                        add(latln)
                        width(5.0f)
                        visible(true)
                        color(ContextCompat.getColor(requireContext(), R.color.geofence_color_selected))
                    })
                    lines.add(line)
                }
            }
        })
    }

    override fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }
}