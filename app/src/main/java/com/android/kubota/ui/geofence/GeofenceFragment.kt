package com.android.kubota.ui.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.combineAndCompute
import com.android.kubota.extensions.hasTelematics
import com.android.kubota.ui.AuthBaseFragment
import com.android.kubota.ui.SwipeAction
import com.android.kubota.ui.SwipeActionCallback
import com.android.kubota.ui.dealer.DEFAULT_LAT
import com.android.kubota.ui.dealer.DEFAULT_LONG
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.BitmapUtils
import com.android.kubota.utility.Constants
import com.android.kubota.viewmodel.equipment.getString
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.features.race
import com.kubota.service.api.caseInsensitiveSort
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.GeoCoordinate
import com.kubota.service.domain.Geofence
import com.kubota.service.domain.displayName
import com.kubota.service.domain.preference.MeasurementUnitType
import com.kubota.service.manager.SettingsRepo
import com.kubota.service.manager.SettingsRepoFactory
import kotlinx.android.parcel.Parcelize
import mil.nga.sf.Point
import mil.nga.sf.util.GeometryUtils
import java.text.DecimalFormat

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

val Address.addressCityState: String
    get() {
        var addr = this.thoroughfare
        val cityState = this.cityState
                if (!cityState.isEmpty()) {
                    if (addr.isNullOrEmpty()) {
                        addr = cityState
                    } else {
                        addr = "${addr}\n${cityState}"

                    }
                }
        return addr ?: ""
    }

val Address.cityState: String
    get() {
        var addr = ""
        this.locality?.let {
            addr += it
        }
        this.adminArea?.let {
            if (!addr.isEmpty()) { addr += ", " }
            addr += it
        }
        return addr
    }

//fun Address.addressLine1(): String {
//    val number = this.subThoroughfare ?: ""
//    val locality = this.locality ?: ""
//    val state = this.adminArea ?: ""
//    val str = this.thoroughfare.let { "$number $it\n$locality, $state" } ?: "$locality $state"
//    return str.trim()
//}

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

fun <V: View> BottomSheetBehavior<V>.setStateAsync(state: Int, timeout: Double = 1.0): Promise<Unit> {
    val pending = Promise.pending<Unit>()

    val callback = object: BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == state) {
                if (pending.first.isPending) pending.second.fulfill(Unit)
            }
        }
    }

    this.addBottomSheetCallback(callback)
    this.state = state

    // set a timeout in case the bottom sheet gets updated
    val timer = after(seconds=timeout).then { throw PMKError.cancelled() }
    return race(timer, pending.first)
        .ensure {
            // always remove our listener
            this.removeBottomSheetCallback(callback)
        }
}

@Parcelize
data class UIGeofence (
    val index: Int,
    val geofence: Geofence,
    val address: String,
    val distance: String
): Parcelable

data class UIEquipmentUnit (
    val index: Int,
    val equipment: EquipmentUnit,
    val address: String,
    val distance: String
)

class GeofenceFragment: AuthBaseFragment(), GeoView.OnClickListener, GeofenceView.OnClickListener {
    companion object {
        private const val GEOFENCE = "com.android.kubota.ui.geofence.GeofenceFragment.GEOFENCE"
        private const val FIRST_TIME_GEOFENCE = "com.android.kubota.ui.geofence.GeofenceFragment.FIRST_TIME_GEOFENCE"
        private const val KEY_LOCATION = "com.android.kubota.ui.geofence.GeofenceFragment.FIRST_TIME_GEOFENCE"

        fun createInstance(location: GeoCoordinate?): GeofenceFragment =
            GeofenceFragment().apply {
                arguments = Bundle(1).apply { putParcelable(KEY_LOCATION, location) }
            }
    }

    enum class State {
        EQUIPMENT,
        GEOFENCES,
        EDIT,
    }

    override val layoutResId: Int = R.layout.fragment_geofence

    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var googleMap: GoogleMap
    private lateinit var addGeofence: Button
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var locationButton: FloatingActionButton
    private val markers = mutableListOf<Marker>()

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this.requireActivity())
    }

    class MyViewModel(application: Application): AndroidViewModel(application), SettingsRepo.Observer {
        private val mSettingsRepo = SettingsRepoFactory.getSettingsRepo(application)
        private val mMeasurementUnits = MutableLiveData<MeasurementUnitType>(mSettingsRepo.getCurrentUnitsOfMeasurement())
        private val mEquipment = MutableLiveData<List<EquipmentUnit>>()
        private val mGeofences = MutableLiveData<List<Geofence>>()
        val editingGeofence = MutableLiveData<Geofence?>()

        val equipment: LiveData<List<UIEquipmentUnit>> = mEquipment.combineAndCompute(mMeasurementUnits) {equipmentList, measurementUnit ->

            var idx = 0
            equipmentList
                .caseInsensitiveSort { it.displayName }
                .map { it ->
                val location = it.telematics?.location
                UIEquipmentUnit(
                    index= ++idx,
                    equipment= it,
                    address= location?.let {geocode(it)?.addressCityState } ?: getString(R.string.location_unavailable),
                    distance= location?.let { distance(it, measurementUnit) } ?: ""
                )
            }
        }
        val geofences: LiveData<List<UIGeofence>> = mGeofences.combineAndCompute(mMeasurementUnits) {geofenceList, measurementUnit ->
            var idx = 0
            geofenceList
                .caseInsensitiveSort { it.description }
                .map { geo ->
                    val lastLocation = geo.points.firstOrNull()
                    UIGeofence(
                        index= ++idx,
                        geofence= geo,
                        address= lastLocation?.let { geocode(it)?.cityState } ?: getString(R.string.location_unavailable),
                        distance= lastLocation?.let { distance(it, measurementUnit) } ?: ""
                    )
            }
        }
        val lastLocation = MutableLiveData<LatLng?>()
        val loading = MutableLiveData(0)
        val state = MutableLiveData<State>()
        val error = MutableLiveData<String?>()
        private val df = DecimalFormat("#.#")

        init {
            mSettingsRepo.addObserver(this)
        }

        override fun onChange() {
            mMeasurementUnits.postValue(mSettingsRepo.getCurrentUnitsOfMeasurement())
        }

        fun loadData(delegate: AuthDelegate?) {
            loadGeofences(delegate)
            loadEquipment(delegate)
        }

        private fun geocode(loc: GeoCoordinate): Address? =
            Geocoder(this.getApplication())
                .getFromLocation(loc.latitude, loc.longitude, 1)
                .firstOrNull()

        private fun distance(loc: GeoCoordinate, units: MeasurementUnitType): String {
            val meters = this.lastLocation.value?.let {
                val results = FloatArray(1)
                Location.distanceBetween(it.latitude, it.longitude, loc.latitude, loc.longitude, results)
                results.firstOrNull()
            }

            when (units) {
                MeasurementUnitType.METRIC -> {
                    val km = meters?.let { it*0.001 }
                    return km?.let { "${df.format(km)} km" } ?: ""
                }
                MeasurementUnitType.US -> {
                    val miles = meters?.let { it*0.000621371192 }
                    return miles?.let { "${df.format(miles)} mi" } ?: ""
                }
            }
        }

        private fun loadEquipment(delegate: AuthDelegate?): Promise<Unit> {
            pushLoading()
            return AuthPromise(delegate)
                .then {
                    AppProxy.proxy.serviceManager.userPreferenceService.getEquipment()
                }
                .done { mEquipment.postValue(it.filter { it.hasTelematics }) }
                .recover { error.value = it.message; throw it }
                .ensure { popLoading() }
        }

        private fun loadGeofences(delegate: AuthDelegate?): Promise<Unit> {
            pushLoading()
            return AuthPromise(delegate)
                .then {
                    AppProxy.proxy.serviceManager.userPreferenceService.getGeofences()
                }
                .done { mGeofences.postValue(it) }
                .recover { error.value = it.message; throw it }
                .ensure { popLoading() }
        }

        private fun pushLoading() {
            loading.value = (loading.value ?: 0) + 1
        }

        private fun popLoading() {
            loading.value = loading.value?.let { Math.max(it-1, 0) }
        }

        fun updateGeofence(delegate: AuthDelegate?, geofence: Geofence) {
            this.pushLoading()
            AuthPromise(delegate)
                .then {
                    AppProxy.proxy.serviceManager.userPreferenceService.updateGeofence(geofence)
                }
                .done { mGeofences.postValue(it) }
                .catch { error.value = it.message; throw it }
                .finally { this.popLoading() }
        }

        fun removeGeofence(delegate: AuthDelegate?, geofence: Geofence) {
            this.pushLoading()
            AuthPromise(delegate)
                .then {
                    AppProxy.proxy.serviceManager.userPreferenceService.removeGeofence(geofence.id)
                }
                .done { mGeofences.postValue(it) }
                .catch { error.value = it.message }
                .finally { this.popLoading() }
        }
    }

    private val viewModel: MyViewModel by lazy { ViewModelProvider(requireActivity()).get(MyViewModel::class.java) }

    private fun collapseAndScrollToPosition(index: Int) {
        bottomSheetBehavior.setStateAsync(BottomSheetBehavior.STATE_COLLAPSED)
            .done {
                val mgr = this.recyclerView.layoutManager as LinearLayoutManager
                mgr.startSmoothScroll(object: LinearSmoothScroller(context) {
                    init { this.targetPosition = index }
                    override fun getVerticalSnapPreference(): Int = LinearSmoothScroller.SNAP_TO_START
                })
            }
    }

    override fun onClicked(item: UIEquipmentUnit) {
        collapseAndScrollToPosition(item.index-1)
        // select the marker...
        this.selectedMarker = markers.find {
            (it.tag as? UIEquipmentUnit)?.let {
                return@find it.equipment.id == item.equipment.id
            }
            return@find false
        }
        item.equipment.telematics?.location?.let {
            val camera = CameraUpdateFactory.newLatLng(it.toLatLng())
            googleMap.animateCamera(camera)
        }
    }

    override fun onClicked(item: UIGeofence) {
        collapseAndScrollToPosition(item.index-1)

        item.geofence.bounds()?.let {
            val camera = CameraUpdateFactory.newLatLngBounds(it, 100)
            googleMap.animateCameraAsync(camera)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = GeofenceNameActivity.handleResult(requestCode, resultCode, data)
        when (result) {
            is Result.fulfilled -> {
                val geofence = this.viewModel.editingGeofence.value
                geofence?.let {
                    val cp = Geofence(
                        id=it.id,
                        description=result.value.trim(),
                        points = it.points
                    )
                    viewModel.updateGeofence(this.authDelegate, geofence=cp)
                }
            }
            is Result.rejected -> {}
        }
    }

    override fun onEditClicked(geofence: UIGeofence) {
        this.viewModel.editingGeofence.value = geofence.geofence
        GeofenceNameActivity.launch(this, geofence.geofence.description)
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
        fragment.getMapAsync { onMapInit(it) }
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
                viewModel.state.value = when (tab?.position) {
                    0 ->  State.EQUIPMENT
                    else -> State.GEOFENCES
                }
            }
        })
    }

    private fun setupList(view: View) {
        val listener = this
        recyclerView = view.findViewById<RecyclerView>(R.id.geoList).apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = GeofenceEquipmentListFragment(viewModel.equipment.value ?: emptyList(), listener)
        }

        val swipeAction = SwipeAction(
            requireContext().getDrawable(R.drawable.ic_action_delete)!!,
            ContextCompat.getColor(requireContext(), R.color.delete_swipe_action_color)
        )
        val callback = object : SwipeActionCallback(swipeAction, swipeAction) {
            override fun isItemViewSwipeEnabled(): Boolean = recyclerView.adapter is GeofenceListFragment
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, p1: Int) {
                val adapter = recyclerView.adapter
                when (adapter) {
                    is GeofenceEquipmentListFragment -> {}
                    is GeofenceListFragment -> {
                        val idx = viewHolder.adapterPosition
                        val item = adapter.getItem(idx)
                        viewModel.removeGeofence(authDelegate, item.geofence)
                    }
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun onEquipment(equipment: List<UIEquipmentUnit>) {
        addGeofence.visibility = View.GONE

        googleMap.clear()
        markers.clear()

        this.selectedMarker = null
        recyclerView.adapter = GeofenceEquipmentListFragment(equipment, this)

        val polys = this.viewModel.geofences.value?.map {
            it.geofence.points.map { Point(it.longitude, it.latitude) }
        }

        equipment
            .forEachIndexed { idx, it ->
                val loc = it.equipment.telematics?.location
                if (loc == null) return@forEachIndexed

                val inside = insideGeofence(polys, loc.toLatLng())
                val icon = equipmentIcon(unit=it, loc=loc.toLatLng(), inside=inside, scale=1.0f)

                val marker = googleMap.addMarker(MarkerOptions().apply {
                    icon( icon )
                    position(LatLng(loc.latitude, loc.longitude))
                    draggable(false)
                    zIndex(1.0f)
                })
                marker.tag = it
                markers.add(marker)
            }

        val color = ContextCompat.getColor(requireContext(), R.color.geofence_color_unselected)

        viewModel.geofences.value?.let {
            it.filter { it.geofence.points.size > 2 }
            .map {
                PolygonOptions().apply {
                    fillColor(color)
                    clickable(false)
                    strokeWidth(0.0f)
                    addAll(it.geofence.points)
                    zIndex(0.0f)
                }
            }
            .forEach { googleMap.addPolygon(it) }
        }
    }

    private fun onGeofences(geofences: List<UIGeofence>) {

        addGeofence.visibility = View.VISIBLE
        googleMap.clear()
        markers.clear()
        this.selectedMarker = null

        recyclerView.adapter = GeofenceListFragment(geofences, this)

        val paint = Paint()
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.setShadowLayer(5.0f, 0.0f, 5.0f, Color.DKGRAY)
        paint.textSize = 36.0f * getResources().getDisplayMetrics().density
        paint.isAntiAlias = true

        val color = ContextCompat.getColor(requireContext(), R.color.geofence_color_selected)

        geofences
            .forEachIndexed { idx, it ->
                if (it.geofence.points.isEmpty()) return@forEachIndexed

                val tag = it

                val z = idx + 1.0f
                val poly = googleMap.addPolygon(PolygonOptions().apply {
                    fillColor(color)
                    strokeWidth(0.0f)
                    clickable(true)
                    addAll(it.geofence.points)
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
                        position((it.geofence.centroid() ?: it.geofence.points.first()).toLatLng())
                        anchor(0.5f, 0.5f)
                        flat(false)
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

        val factory_inside = BitmapUtils.createFromSvg(requireContext(),R.drawable.ic_inside_geofence).let {
            BitmapDescriptorFactory.fromBitmap(it)
        }

        val factory_outside = BitmapUtils.createFromSvg(requireContext(),R.drawable.ic_outside_geofence).let {
            BitmapDescriptorFactory.fromBitmap(it)
        }

        val polys = this.viewModel.geofences.value?.map {
            it.geofence.points.map { Point(it.longitude, it.latitude) }
        }

        this.viewModel.equipment.value?.let {
            it.mapNotNull { it.equipment.telematics?.location }
            .map { coord ->

                val inside = insideGeofence(polys, coord.toLatLng())
                MarkerOptions()
                    .icon(if (inside) factory_inside else factory_outside)
                    .position(LatLng(coord.latitude, coord.longitude))
                    .draggable(false)
                    .zIndex(0.0f)
                    .alpha(0.85f) // transparent to not overwhelm the polygons
            }
            .forEach {
                val marker = googleMap.addMarker(it)
                markers.add(marker)
            }
        }
    }

    private fun onFirstTime(callback: () -> Unit ) {
        val prefs = activity?.getSharedPreferences(GEOFENCE, Context.MODE_PRIVATE)
        if (prefs == null) return callback()

        if (prefs.getBoolean(FIRST_TIME_GEOFENCE, true)) {
            callback()
            prefs.edit().putBoolean(FIRST_TIME_GEOFENCE, false).apply()
        }
    }

    private fun onEditGeofence(geofence: Geofence) {
        onFirstTime {
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
        val equipment = this.viewModel.equipment.value?.mapNotNull { it.equipment.telematics?.location } ?: emptyList()
        val pos = googleMap.cameraPosition.target
        val zoom = googleMap.cameraPosition.zoom
        flowActivity?.addFragmentToBackStack(GeofenceEditFragment.createInstance(geofence=geofence, equipment=equipment, location=pos.toCoordinate(), zoom=zoom))
        this.viewModel.state.value = State.GEOFENCES
    }

    override fun initUi(view: View) {
        progressBar = view.findViewById(R.id.toolbarProgressBar)
        addGeofence = view.findViewById(R.id.addGeofence)
        locationButton = view.findViewById(R.id.locationButton)
        val bottom: LinearLayout = view.findViewById(R.id.bottomSheetList)
        this.bottomSheetBehavior = BottomSheetBehavior.from(bottom)

        this.requireActivity().setTitle(R.string.geofences)

        setupTabs(view)
        setupMap(view)
        setupList(view)
    }

    private fun equipmentIcon(unit: UIEquipmentUnit, loc: LatLng, inside: Boolean, scale: Float = 1.0f): BitmapDescriptor {
        val paint = Paint()
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.textSize = 14.0f * getResources().getDisplayMetrics().density * scale
        paint.isAntiAlias = true

        val lbl = (unit.index).toString()

        val resId = if (inside) R.drawable.ic_numbered_geofence else R.drawable.ic_numbered_geofence_outside
        val drawable = ContextCompat.getDrawable(requireContext(), resId)!!

        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight

        val dim = Size((w * scale).toInt(), (h * scale).toInt() )

        val bitmap = Bitmap.createBitmap(dim.width, dim.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val x = canvas.width * 0.5f
        val y = (canvas.height * 0.35f) - (paint.descent() + paint.ascent())
        canvas.drawText(lbl, x, y, paint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun insideGeofence(loc: LatLng): Boolean {
        val polys = this.viewModel.geofences.value?.map {
            it.geofence.points.map { Point(it.longitude, it.latitude) }
        }
        return insideGeofence(polys, loc)
    }

    private fun insideGeofence(polys: List<List<Point>>?, loc: LatLng): Boolean {
        val pnt = Point(loc.longitude, loc.latitude)
        return polys?.find { GeometryUtils.pointInPolygon(pnt, it) } != null
    }

    var selectedMarker: Marker? = null
        set(value) {
            if (field === value) return

            val polys = this.viewModel.geofences.value?.map {
                it.geofence.points.map { Point(it.longitude, it.latitude) }
            }

            field?.let {marker ->
                val loc = marker.position
                (marker.tag as? UIEquipmentUnit)?.let { unit ->
                    val inside = insideGeofence(polys, loc)
                    val icon = equipmentIcon(unit=unit, loc=loc, inside=inside, scale=1.0f)
                    marker.setIcon(icon)
                }
            }

            value?.let { marker ->
                val loc = marker.position
                (marker.tag as? UIEquipmentUnit)?.let { unit ->
                    val inside = insideGeofence(polys, loc)
                    val icon = equipmentIcon(unit=unit, loc=loc, inside=inside, scale=1.5f)
                    marker.setIcon(icon)
                }
            }
            field = value
        }

    private fun onMapInit(map: GoogleMap) {
        val coord = this.arguments?.getParcelable<GeoCoordinate?>(KEY_LOCATION)

        this.googleMap = map.apply {
            uiSettings?.isRotateGesturesEnabled = false
            uiSettings?.isMapToolbarEnabled = false
            uiSettings?.isMyLocationButtonEnabled = false
            coord?.let {
                val camera = CameraUpdateFactory.newLatLng(it.toLatLng())
                this.moveCamera(camera)
            }
            mapType = GoogleMap.MAP_TYPE_HYBRID
            setOnMarkerClickListener {
                val tag = it.tag
                it.zIndex = it.zIndex + 1.0f
                this@GeofenceFragment.selectedMarker = it
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
                if (tag is UIGeofence) {
                    recyclerView.adapter = GeofenceListFragment(listOf(tag), this@GeofenceFragment)
                }
            }
            setOnMapClickListener {
                this@GeofenceFragment.selectedMarker = null

                // deselect...
                if (recyclerView.adapter?.itemCount != 1) return@setOnMapClickListener

                // force a refresh
                viewModel.state.value = viewModel.state.value
            }
        }

        addGeofence.setOnClickListener {
            val idx = this.viewModel.geofences.value?.size ?: 0
            val geofence = Geofence(description="Geofence ${idx+1}")
            this.viewModel.editingGeofence.value = geofence
            this.viewModel.state.value = State.EDIT
        }

        this.viewModel.equipment.observe(viewLifecycleOwner, Observer {
            if (viewModel.state.value == State.EQUIPMENT) {
                onEquipment(viewModel.equipment.value ?: emptyList())
            }
        })

        this.viewModel.geofences.observe(viewLifecycleOwner, Observer {
            if (viewModel.state.value == State.GEOFENCES) {
                onGeofences(viewModel.geofences.value ?: emptyList())
            }
        })

        this.locationButton.setOnClickListener {
            viewModel.lastLocation.value?.let {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(it))
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (it) {
                State.EQUIPMENT -> onEquipment(viewModel.equipment.value ?: emptyList())
                State.GEOFENCES -> onGeofences(viewModel.geofences.value ?: emptyList())
                State.EDIT -> {
                    val idx = this.viewModel.geofences.value?.size ?: 0
                    onEditGeofence(viewModel.editingGeofence.value ?: Geofence(description="Geofence ${idx+1}"))
                }
                else -> {}
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it > 0) showProgressBar() else hideProgressBar()
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })

        viewModel.state.value = State.EQUIPMENT
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

    @SuppressLint("MissingPermission")
    private fun loadLastLocation() {
        this.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, R.string.accept_location_permission)
            .done {
                this.locationButton.visibility = View.VISIBLE
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
            .catch {
                this.locationButton.visibility = View.GONE
            }
    }

    override fun showProgressBar() {
        this.progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        this.progressBar.visibility = View.INVISIBLE
    }

    override fun loadData() {
        viewModel.loadData(this.authDelegate)
        loadLastLocation()
    }
}
