package com.android.kubota.ui.geofence

import android.app.AlertDialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.geofence.toCoordinate
import com.android.kubota.ui.geofence.toLatLng
import com.android.kubota.utility.BitmapUtils
import com.android.kubota.utility.Constants
import com.android.kubota.utility.showMessage
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.GeoCoordinate
import com.kubota.service.domain.Geofence
import mil.nga.sf.Point
import mil.nga.sf.util.GeometryUtils
import mil.nga.sf.util.sweep.ShamosHoey
import java.util.*
import kotlin.error

private fun calculateRadius(map: GoogleMap): Double {
    // get 2 of the visible diagonal corners of the map (could also use farRight and nearLeft)
    val topLeft = map.projection.visibleRegion.farLeft
    val bottomRight = map.projection.visibleRegion.nearRight

    val diagonal = FloatArray(1)
    Location.distanceBetween(topLeft.latitude, topLeft.longitude, bottomRight.latitude, bottomRight.longitude, diagonal)
    // use a fraction of the view
    return diagonal.first() * 0.006
}

private fun validateLocation(polygon: List<LatLng>): Boolean {
    if (polygon.size < 3) return false
    if (polygon.first() != polygon.last()) return false
    val poly = polygon.map { Point(it.longitude, it.latitude) }
    return ShamosHoey.simplePolygonPoints(poly) && ShamosHoey.simplePolygonPoints(poly.reversed())
}

private fun validatePolygon(polygon: List<Point>): Boolean {
    if (polygon.size < 3) return false
    if (polygon.first() != polygon.last()) return false
    return ShamosHoey.simplePolygonPoints(polygon) && ShamosHoey.simplePolygonPoints(polygon.reversed())
}

class GeofenceEditFragment : BaseFragment(), GoogleMap.OnCircleClickListener, GoogleMap.OnMapClickListener {

    companion object {
        private const val KEY_GEOFENCE = "geofence"
        private const val KEY_EQUIPMENT = "equipment"

        fun createInstance(geofence: Geofence, equipment: List<GeoCoordinate>) =
            GeofenceEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_GEOFENCE, geofence)
                    putParcelableArrayList(KEY_EQUIPMENT, ArrayList(equipment))
                }
            }
    }

    override val layoutResId: Int = R.layout.fragment_geofence_edit

    private lateinit var googleMap: GoogleMap
    private lateinit var saveButton: Button
    private lateinit var editIcon: ImageView
    private lateinit var geofenceName: TextView

    class GeofenceViewModel: ViewModel() {
        val points = MutableLiveData<List<LatLng>>()
        val uuid = MutableLiveData<UUID>()
        val name = MutableLiveData<String>()
        val loading = MutableLiveData(0)
        val error = MutableLiveData<String?>()
        val equipment = MutableLiveData<List<LatLng>>()
        val closedPolygon = MutableLiveData<Boolean>(false)

        fun addPoint(latln: LatLng) {
            points.value = points.value?.plus(latln) ?: listOf(latln)
        }

        fun finish(): Boolean {
            val points = this.points.value
            if (points == null || points.size < 2) return false
            this.points.value = points.plus(points.first())
            this.closedPolygon.value = true
            return true
        }

        fun removeLastPoint() {
            val list = points.value
            val cp = list?.dropLast(1)
            points.value = cp
            closedPolygon.value = false
        }


        private fun pushLoading() {
            loading.value = (loading.value ?: 0) + 1
        }

        private fun popLoading() {
            loading.value = loading.value?.let { Math.max(it-1, 0) } ?: 0
        }

        fun updateGeofence(geofence: Geofence): Promise<Unit> {
            pushLoading()
            return AppProxy.proxy.serviceManager.userPreferenceService
                .updateGeofence(geofence=geofence)
                .asVoid()
                .recover { this.error.value = it.message; throw it }
                .ensure { this.popLoading() }
        }

        fun loadMarkers() {
            pushLoading()
            AppProxy.proxy.serviceManager.userPreferenceService
                .getUserPreference()
                .map { it.equipment ?: listOf() }
                .map { equipment.value = it.mapNotNull { it.telematics?.location?.toLatLng() } }
                .recover { error.value = it.message; throw it }
                .ensure { popLoading() }
        }

        fun validatePolygon(): Boolean =
            points.value?.let { com.android.kubota.ui.geofence.validateLocation(it) } ?: false

        fun toGeofence(): Geofence {
            val name = name.value ?: "Geofence"
            val points = points.value ?: emptyList()
            val uuid = uuid.value ?: UUID.randomUUID()
            return Geofence(uuid, name, points.map { it.toCoordinate() })
        }
    }

    private var circles = mutableListOf<Circle>()
    private var lines = mutableListOf<Polyline>()
    private var markers = mutableListOf<Marker>()
    private var polygon: Polygon? = null

    val viewModel: GeofenceViewModel by lazy { ViewModelProvider(requireActivity()).get(GeofenceViewModel::class.java) }

    override fun onMapClick(latln: LatLng?) {
        latln?.let { viewModel.addPoint(it) }
    }

    override fun onCircleClick(circle: Circle?) {
        if (circle == circles.first()) {
            viewModel.finish()
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

        fragment.getMapAsync { initMap(it) }
        fragmentTransaction
            .replace(R.id.mapFragmentPane, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.geofenceUndo -> viewModel.removeLastPoint()
            R.id.geofenceInfo -> {
                AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                    .setView(R.layout.dialog_geofence_instructions)
                    .setCancelable(true)
                    .create().let {
                        val dialog = it
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
                        dialog?.findViewById<ImageView>(R.id.btn_dismiss_dialog)?.setOnClickListener {
                            dialog.dismiss()
                        }
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.geofence_edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = GeofenceNameActivity.handleResult(requestCode, resultCode, data)
        when (result) {
            is Result.fulfilled -> this.viewModel.name.value = result.value
            is Result.rejected -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val geofence: Geofence? = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(KEY_GEOFENCE)
        } else {
            this.arguments?.getParcelable(KEY_GEOFENCE)
        }

        this.arguments?.getParcelableArrayList<GeoCoordinate>(KEY_EQUIPMENT)?.let {
            this.viewModel.equipment.value = it.map { LatLng(it.latitude, it.longitude) }
        }

        this.viewModel.uuid.value = geofence?.uuid ?: UUID.randomUUID()
        this.viewModel.name.value = geofence?.name ?: ""
        this.viewModel.points.value = geofence?.points?.map { it.toLatLng() } ?: emptyList()
    }

    private fun loadMarkers(list: List<LatLng>) {
        markers.forEach { it.remove() }
        markers.clear()

        val poly = viewModel.points.value?.map { Point(it.longitude, it.latitude) }
        val validPolygon = poly != null && validatePolygon(poly)

        val factoryInside = BitmapUtils.createFromSvg(requireContext(),R.drawable.ic_inside_geofence).let {
            BitmapDescriptorFactory.fromBitmap(it)
        }

        val factoryOutside = BitmapUtils.createFromSvg(requireContext(),R.drawable.ic_outside_geofence).let {
            BitmapDescriptorFactory.fromBitmap(it)
        }

        list.map { coord ->
            val pnt = Point(coord.longitude, coord.latitude)
            val inside = poly == null || !validPolygon || GeometryUtils.pointInPolygon(pnt, poly)

            MarkerOptions()
                .icon(if (inside) factoryInside else factoryOutside)
                .position(LatLng(coord.latitude, coord.longitude))
                .draggable(false)
                .zIndex(0.0f)
                .alpha(0.65f) // transparent to not overwhelm the polygons
        }
            .forEach { googleMap.addMarker(it).let { markers.add(it) } }
    }

    fun initMap(map: GoogleMap) {
        this.googleMap = map.apply {
            uiSettings?.isRotateGesturesEnabled = false
            uiSettings?.isMapToolbarEnabled = false
            uiSettings?.isMyLocationButtonEnabled = false
            setOnMapClickListener(this@GeofenceEditFragment)
            setOnCircleClickListener(this@GeofenceEditFragment)
            setOnMarkerClickListener { true }
            setOnCameraMoveListener {
                val radius = calculateRadius(map=this)
                circles.forEach { it.radius = radius }
            }
        }
        viewModel.points.value?.let { buildPolygon(it) }

        viewModel.equipment.observe(viewLifecycleOwner, Observer {
            loadMarkers(it)
        })

        viewModel.closedPolygon.observe(viewLifecycleOwner, Observer {
            this.viewModel.equipment.value?.let { loadMarkers(it) }
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it > 0) showProgressBar() else hideProgressBar()
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })

        this.viewModel.name.observe(viewLifecycleOwner, Observer {
            this.geofenceName.text = it
        })

        this.viewModel.points.observe(viewLifecycleOwner, Observer {
            buildPolygon(it)
        })

        // load the markers
        if (viewModel.equipment.value.isNullOrEmpty()) {
            this.viewModel.loadMarkers()
        }
    }

    override fun initUi(view: View) {
        this.setHasOptionsMenu(true)

        saveButton = view.findViewById(R.id.saveButton)
        editIcon = view.findViewById(R.id.editIcon)
        geofenceName = view.findViewById(R.id.geofenceName)
        view.findViewById<View>(R.id.appbar).setOnClickListener {
            GeofenceNameActivity.launch(this, viewModel.name.value ?: "")
        }

        setupMap(view)

        // This callback will only be called when MyFragment is at least Started.
        requireActivity().onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            private var busy = false

            override fun handleOnBackPressed() {
                isEnabled = true
                if (busy) return

                // user can safely back out
                if (viewModel.points.value.isNullOrEmpty()) {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }

                busy = true

                // we have unsaved changes, don't allow the user to back out
                this@GeofenceEditFragment
                    .showMessage(R.string.discard_changes, R.string.discard_geofence)
                    .map {
                        when (it) {
                            AlertDialog.BUTTON_POSITIVE -> {
                                isEnabled = false
                                requireActivity().onBackPressed()
                            }
                            AlertDialog.BUTTON_NEGATIVE -> {}
                            AlertDialog.BUTTON_NEUTRAL -> {}
                        }
                    }
                    .ensure { busy = false }
            }
        })

        this.saveButton.isEnabled = false
        this.saveButton.setOnClickListener {

            if (!this.viewModel.validatePolygon()) {
                showMessage(R.string.try_again, R.string.lines_intersect)
                return@setOnClickListener
            }

            val geofence = this.viewModel.toGeofence()
            this.viewModel
                .updateGeofence(geofence)
                .done { parentFragmentManager.popBackStack() }
        }
    }

    private fun buildPolygon(list: List<LatLng>) {
        this.saveButton.isEnabled = false

        circles.forEach { it.remove() }
        circles.clear()

        lines.forEach { it.remove() }
        lines.clear()

        this.polygon?.remove()

        val map = googleMap

        if (list.size > 3 && list.last() == list.first()) { // close polygon
            this.saveButton.isEnabled = true
            if (this.viewModel.validatePolygon()) {
                this.polygon = map.addPolygon(PolygonOptions().apply {
                    addAll(list.dropLast(1))
                    fillColor(ContextCompat.getColor(requireContext(), R.color.geofence_color_selected))
                    strokeWidth(0.0f)
                })
                // reload the equipment
                return
            }
            this.viewModel.equipment.value = this.viewModel.equipment.value ?: emptyList()
        }

        val radius = calculateRadius(map=map)
        list.forEach {
            val latln = it
            val circle = map.addCircle(CircleOptions().apply {
                center(latln)
                clickable(circles.isEmpty())
                strokeWidth(0.0f)
                fillColor(ContextCompat.getColor(requireContext(), R.color.geofence_color_selected))
                radius(radius)
            })
            circles.add(circle)
            circle.tag = latln
        }

        if (circles.isEmpty()) return

        circles.reduce { prev, curr ->
            val line = map.addPolyline(PolylineOptions().apply {
                add(prev.center)
                add(curr.center)
                width(5.0f)
                visible(true)
                color(ContextCompat.getColor(requireContext(), R.color.geofence_color_selected))
            })
            lines.add(line)

            curr
        }
    }

    override fun loadData() {

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val geofence = this.viewModel.toGeofence()
        outState.putParcelable(KEY_GEOFENCE, geofence)
    }
}