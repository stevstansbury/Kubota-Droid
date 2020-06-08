package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.geofence.GeofenceEditFragment
import com.android.kubota.ui.geofence.GeofenceNameActivity
import com.android.kubota.utility.BitmapUtils
import com.android.kubota.utility.Constants
import com.android.kubota.utility.PermissionRequestManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.inmotionsoftware.promisekt.features.whenResolved
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.GeoCoordinate
import com.kubota.service.domain.Geofence
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_geofence.*
import java.util.*

private fun LatLngBounds.size(): PointF {
    val ne = this.northeast
    val sw = this.southwest
    val results = FloatArray(1) { 0.0f }
    Location.distanceBetween(ne.latitude, ne.longitude, sw.latitude, ne.longitude, results)
    val y = results[0]
    Location.distanceBetween(ne.latitude, ne.longitude, ne.latitude, sw.longitude, results)
    val x = results[0]
    return PointF(x, y)
}

fun PolygonOptions.addAll(coordinates: List<GeoCoordinate>) {
    coordinates.forEach { add(it.toLatLng()) }
}

fun LatLng.toCoordinate() = GeoCoordinate(latitude=latitude, longitude=longitude)
fun GeoCoordinate.toLatLng() = LatLng(latitude, longitude)

fun Geofence.bounds(): LatLngBounds? {
    if (points.isEmpty()) return null
    return points
        .fold(LatLngBounds.Builder()) { sum, it -> sum.include(it.toLatLng()) }
        .build()
}

fun Geofence.centroid(): GeoCoordinate? = this.bounds()?.center?.toCoordinate()

fun GoogleMap.animateCameraAsync(update: CameraUpdate): Promise<Unit> {
    val pending = Promise.pending<Unit>()
    this.animateCamera(update, object: GoogleMap.CancelableCallback {
        override fun onFinish() {
            pending.second.fulfill(Unit)
        }

        override fun onCancel() {
            pending.second.reject(PMKError.cancelled())
        }
    })

    return pending.first
}

private fun EquipmentUnit.toUI(index: Int) =
    UIEquipmentUnit(
        index = index,
        equipment = this,
        address1 = "1901 Kubota Dr",
        address2 = "Grapevine, TX 76051",
        distance = "123.45 miles"
    )

private fun Geofence.toUI(index: Int) =
    UIGeofence(
        index = index,
        geofence = this,
        address1 = "1901 Kubota Dr",
        address2 = "Grapevine, TX 76051",
        distance = "123.45 miles"
    )

@Parcelize
data class UIGeofence (
    val index: Int,
    val geofence: Geofence,
    val address1: String,
    val address2: String,
    val distance: String
): Parcelable

data class UIEquipmentUnit (
    val index: Int,
    val equipment: EquipmentUnit,
    val address1: String,
    val address2: String,
    val distance: String
)

class GeofenceFragment: BaseFragment(), GeoView.OnClickListener, GeofenceView.OnClickListener {
    sealed class State {
        class None(): State()
        class Equipment(val items: List<EquipmentUnit>): State()
        class Geofences(val items: List<Geofence>): State()
        class Edit(val geofence: Geofence): State()
    }

    override val layoutResId: Int = R.layout.fragment_geofence

    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var googleMap: GoogleMap
    private lateinit var addGeofence: Button
    private lateinit var locationButton: FloatingActionButton

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this.requireActivity())
    }

    class MyViewModel: ViewModel() {
        val editingGeofence = MutableLiveData<Geofence?>()
        val equipment = MutableLiveData<List<EquipmentUnit>>()
        val geofences = MutableLiveData<List<Geofence>>()
        val lastLocation = MutableLiveData<LatLng?>()
    }

    private val viewModel: MyViewModel by lazy { ViewModelProvider(requireActivity()).get(MyViewModel::class.java) }

    override fun onClicked(item: UIEquipmentUnit) {
        item.equipment.location?.let {
            val camera = CameraUpdateFactory.newLatLng(it.toLatLng())
            googleMap.animateCamera(camera)
        }
    }

    override fun onClicked(item: UIGeofence) {
        item.geofence.bounds()?.let {
            val camera = CameraUpdateFactory.newLatLngBounds(it, 100)
            googleMap.animateCameraAsync(camera)
                .map {
                    // TODO
                    println("")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val name = GeofenceNameActivity.handleResult(requestCode, resultCode, data)
        val geofence = this.viewModel.editingGeofence.value
        geofence?.let {
            val cp = Geofence(
                uuid=it.uuid,
                name=name,
                points = it.points
            )
            this.showProgressBar()
            AppProxy.proxy.serviceManager.userPreferenceService
                .updateGeofence(cp)
                .thenMap { AppProxy.proxy.serviceManager.userPreferenceService.getGeofences() }
                .done { viewModel.geofences.value = it }
                .ensure { this.hideProgressBar() }
        }
    }

    override fun onEditClicked(item: UIGeofence) {
        this.viewModel.editingGeofence.value = item.geofence
        GeofenceNameActivity.launch(this, item.geofence.name)
    }

    private var state: State = State.None()
        set(value) {
            // TODO: change stuff
            when (value) {
                is State.Equipment -> onEquipment(value.items)
                is State.Geofences -> onGeofences(value.items)
                is State.Edit -> onEditGeofence(value.geofence)
            }
            field = value
        }

    private fun setupMap(view: View) {

        val fragmentTransaction = childFragmentManager.beginTransaction()
        (childFragmentManager.findFragmentById(R.id.mapFragmentPane) as? SupportMapFragment)?.let {childFragment ->
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
                setOnMarkerClickListener {
                    val tag = it.tag
                    when (tag) {
                        is UIEquipmentUnit -> {
                            recyclerView.adapter = GeofenceEquipmentListFragment(listOf(tag), this@GeofenceFragment)
                            true
                        }
                        is UIGeofence -> {
                            recyclerView.adapter = GeofenceListFragment(listOf(tag), this@GeofenceFragment)
                            true
                        }
                        else -> false
                    }
                }

                setOnPolygonClickListener {
                    val tag = it.tag
                    when (tag) {
                        is UIGeofence -> {
                            recyclerView.adapter = GeofenceListFragment(listOf(tag), this@GeofenceFragment)
                            true
                        }
                        else -> false
                    }
                }

                setOnMapClickListener {
                    // deselect...
                    if (recyclerView.adapter?.itemCount != 1) return@setOnMapClickListener

                    val state = this@GeofenceFragment.state
                    when (state) {
                        is State.Equipment -> {
                            val list = state.items.mapIndexed { idx, it -> it.toUI(idx+1) }
                            recyclerView.adapter = GeofenceEquipmentListFragment(list, this@GeofenceFragment)
                        }
                        is State.Geofences -> {
                            val list = state.items.mapIndexed { idx, it -> it.toUI(idx+1) }
                            recyclerView.adapter = GeofenceListFragment(list, this@GeofenceFragment)
                        }
                    }
                }
            }
            this.state = State.Equipment(emptyList())
        }

        fragmentTransaction
            .replace(R.id.mapFragmentPane, fragment)
            .commit()
    }

    private fun setupTabs(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().apply {
            text = getString(R.string.equipment_tab)
        })

        tabLayout.addTab(tabLayout.newTab().apply {
            text = getString(R.string.geofence_tab)
        })

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                state = when (tab?.position) {
                    0 ->  State.Equipment(viewModel.equipment.value ?: emptyList())
                    else -> State.Geofences(viewModel.geofences.value ?: emptyList())
                }
            }
        })
    }

    private fun setupList(view: View) {
        val listener = this
        recyclerView = view.findViewById<RecyclerView>(R.id.geoList).apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            val list = viewModel.equipment.value?.mapIndexed { idx, it -> it.toUI(idx+1) }
            adapter = GeofenceEquipmentListFragment(list ?: emptyList(), listener)
        }

        this.showProgressBar()
        whenFulfilled(
            AppProxy.proxy.serviceManager.userPreferenceService
                .getUserPreference()
                .map { it.equipment ?: listOf<EquipmentUnit>() }
                .done {
                    this.viewModel.equipment.value = it
                    if (state is State.Equipment) onEquipment(it)
                },
           AppProxy.proxy.serviceManager.userPreferenceService
               .getGeofences()
               .done {
                   this.viewModel.geofences.value = it
                    if (state is State.Geofences) onGeofences(it)
                }
        )
        .catch { this.showError(it) }
        .finally { this.hideProgressBar() }
    }

    private fun onEquipment(equipment: List<EquipmentUnit>) {
        addGeofence.visibility = View.GONE

        googleMap.clear()

        val list = equipment.mapIndexed { idx, it -> it.toUI(idx+1) }
        recyclerView.adapter = GeofenceEquipmentListFragment(list, this)

        val paint = Paint()
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.textSize = 14.0f * getResources().getDisplayMetrics().density
        paint.isAntiAlias = true

        equipment
            .forEachIndexed { idx, it ->
                val loc = it.location
                if (loc == null) return@forEachIndexed

                val lbl = (idx+1).toString()

                val bmp = BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_numbered_geofence)
                val canvas = Canvas(bmp)

                val x = canvas.width * 0.5f
                val y = (canvas.height * 0.35f) - (paint.descent() + paint.ascent())
                canvas.drawText(lbl, x, y, paint)

                val marker = googleMap.addMarker(MarkerOptions().apply {
                    icon( BitmapDescriptorFactory.fromBitmap(bmp) )
                    position(LatLng(loc.latitude, loc.longitude))
                    draggable(false)
                    zIndex(1.0f)
                })
                marker.tag = it.toUI(idx+1)
            }

        val color = ContextCompat.getColor(requireContext(), R.color.geofence_color_unselected)

        viewModel.geofences.value?.let {
            it.filter { it.points.size > 2 }
            .map {
                PolygonOptions().apply {
                    fillColor(color)
                    clickable(false)
                    strokeWidth(0.0f)
                    addAll(it.points)
                    zIndex(0.0f)
                }
            }
            .forEach { googleMap.addPolygon(it) }
        }
    }

    private fun onGeofences(geofences: List<Geofence>) {

        addGeofence.visibility = View.VISIBLE
        googleMap.clear()

        val list = geofences.mapIndexed { idx, it -> it.toUI(idx+1) }
        recyclerView.adapter = GeofenceListFragment(list, this)

        val paint = Paint()
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.textSize = 36.0f * getResources().getDisplayMetrics().density
        paint.isAntiAlias = true

        val color = ContextCompat.getColor(requireContext(), R.color.geofence_color_selected)

        geofences
            .forEachIndexed { idx, it ->
                if (it.points.isEmpty()) return@forEachIndexed

                val tag = it.toUI(idx+1)

                val z = idx + 1.0f
                val poly = googleMap.addPolygon(PolygonOptions().apply {
                    fillColor(color)
                    strokeWidth(0.0f)
                    clickable(true)
                    addAll(it.points)
                    zIndex(z)
                })
                poly.tag = tag

                val size = (75 * getResources().getDisplayMetrics().density).toInt()
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val lbl = (idx+1).toString()
                val x = canvas.width * 0.5f
                val y = (canvas.height * 0.5f) - (paint.descent() + paint.ascent())
                canvas.drawText(lbl, x, y, paint)

                val marker = googleMap.addMarker(MarkerOptions().apply {
                    icon(BitmapDescriptorFactory.fromBitmap(bmp))
                        position((it.centroid() ?: it.points.first()).toLatLng())
                        anchor(0.5f, 0.5f)
                        flat(true)
                        draggable(false)
                        zIndex(z + 0.5f)
                })
                marker.tag = tag

//                googleMap.addGroundOverlay(GroundOverlayOptions().apply {
//                    val bounds = it.points.fold(LatLngBounds.Builder()) { sum, it -> sum.include(it) }.build()
//                    image(BitmapDescriptorFactory.fromBitmap(bmp))
//                    val size = bounds.size()
//                    position(bounds.center, size.x * 0.5f, size.y * 0.5f)
//                    clickable(false)
//                    zIndex(z + 0.5f)
//                })
            }

        val bmp = BitmapUtils.createFromSvg(requireContext(),R.drawable.ic_inside_geofence)
        val factory = BitmapDescriptorFactory.fromBitmap(bmp)

        this.viewModel.equipment.value?.let {
            it.mapNotNull { it.location }
            .map {
                MarkerOptions()
                    .icon(factory)
                    .position(LatLng(it.latitude, it.longitude))
                    .draggable(false)
                    .zIndex(0.0f)
                    .alpha(0.35f) // transparent to not overwhelm the polygons
            }
            .forEach { googleMap.addMarker(it) }
        }
    }

    private fun onEditGeofence(geofence: Geofence) {
        addGeofence.visibility = View.GONE
        flowActivity?.addFragmentToBackStack(GeofenceEditFragment.createInstance(geofence))
    }

    override fun initUi(view: View) {
        progressBar = view.findViewById(R.id.toolbarProgressBar)
        addGeofence = view.findViewById(R.id.addGeofence)
        locationButton = view.findViewById(R.id.locationButton)

        addGeofence.setOnClickListener {
            val geofence = Geofence(name="Geofence 1")
            flowActivity?.addFragmentToBackStack(GeofenceEditFragment.createInstance(geofence))
        }

        this.viewModel.equipment.observe(viewLifecycleOwner, Observer {
            val state = this.state
            if (state is State.Equipment) onEquipment(state.items)
        })

        this.viewModel.geofences.observe(viewLifecycleOwner, Observer {
            val state = this.state
            if (state is State.Geofences) onGeofences(state.items)
        })

        this.locationButton.setOnClickListener {
            viewModel.lastLocation.value?.let {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(it))
            }
        }

        setupTabs(view)
        setupMap(view)
        setupList(view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            val geofence: Geofence? = it.getParcelable("editingGeofence")
            this.viewModel.editingGeofence.value = geofence
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.viewModel.editingGeofence.value?.let {
            outState.putParcelable("editingGeofence", it)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    private fun loadLastLocation() {
        whenResolved(
            PermissionRequestManager.requestPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION, R.string.location),
            PermissionRequestManager.requestPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION, R.string.location)
        )
        .done {

            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                if (!this@GeofenceFragment.isVisible) {
                    return@addOnSuccessListener
                }
                viewModel.lastLocation.value = if (lastLocation != null) {
                    googleMap.isMyLocationEnabled = true
                    LatLng(lastLocation.latitude, lastLocation.longitude)
                } else {
                    LatLng(DEFAULT_LAT, DEFAULT_LONG)
                }
            }
        }
    }

    override fun loadData() {
//        this.viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
//            if (loading) this.showProgressBar() else  this.hideProgressBar()
//        })
//
//        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
//            error?.let { this.showError(it) }
//        })

        loadLastLocation()
    }

    override fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }
}
