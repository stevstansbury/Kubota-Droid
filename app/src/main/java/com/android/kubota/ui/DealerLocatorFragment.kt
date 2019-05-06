package com.android.kubota.ui

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.kubota.R
import com.android.kubota.utility.BitmapUtils
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.utility.Utils
import com.android.kubota.utility.Utils.createMustLogInDialog
import com.android.kubota.viewmodel.DealerLocatorViewModel
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.viewmodel.UIDealer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

private const val DEFAULT_LAT= 32.9792895
private const val DEFAULT_LONG = -97.0315917

class DealerLocatorFragment() : BaseFragment(), BackableFragment {

    private var dialog: AlertDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: DealerLocatorViewModel

    private lateinit var listContainer: ViewGroup
    private lateinit var highlightedDealerContainer: ViewGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: View
    private lateinit var mapView: MapView
    private lateinit var selectedDealerView: View
    private var googleMap: GoogleMap? = null
    private var clusterManager: ClusterManager<SearchDealer>? = null
    private var lastClickedMarker: SearchDealer? = null

    private var canAddDealer: Boolean = false

    private val listener = object: DealerView.OnClickListener {

        override fun onClick(dealer: SearchDealer) {
            val detailFragment = DealerDetailFragment.createIntance(UIDealer(id = -1, name = dealer.name,
                address = dealer.streetAddress, city = dealer.city, state = dealer.stateCode, postalCode = dealer.postalCode,
                phone = dealer.phone, website = dealer.webAddress, dealerNumber = dealer.dealerNumber))

            fragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentPane, detailFragment)
                ?.addToBackStack(null)
                ?.commit()

            (flowActivity as TabbedControlledActivity?)?.hideActionBar()
        }

        override fun onStarClicked(dealer: SearchDealer) {
            if (dealer.isFavorited) {
                viewModel.deleteFavoriteDealer(dealer)
            } else if (canAddDealer) {
                viewModel.insertFavorite(dealer)
            } else {
                resetDialog()

                dialog = createMustLogInDialog(requireContext(), Utils.LogInDialogMode.DEALER_MESSAGE)
                dialog?.setOnCancelListener { resetDialog() }
                dialog?.show()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val factory = InjectorUtils.provideDealerLocatorViewModel(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(DealerLocatorViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dealer_locator, null)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        selectedDealerView = view.findViewById(R.id.selectedDealerView)
        listContainer = view.findViewById(R.id.bottomSheetList)
        listContainer.hideableBehavior(false)
        listContainer.setPeekHeight(resources.getDimensionPixelSize(R.dimen.locator_dealer_list_peek_height))
        highlightedDealerContainer = view.findViewById(R.id.highlightedDealerContainer)
        highlightedDealerContainer.hideableBehavior(true)
        highlightedDealerContainer.hide()
        fab = view.findViewById(R.id.locationButton)

        viewModel.canAddDealer.observe(this, Observer {
            this.canAddDealer = it ?: false
        })

        mapView.getMapAsync { googleMap: GoogleMap? ->
            this.googleMap = googleMap
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.dealersList).apply {
            setHasFixedSize(true)
        }

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fab.visibility = View.GONE
            loadDefaultLocation()
        } else {
            // Permission has already been granted
            loadLastLocation()
        }

        return view
    }

    override fun onBackPressed(): Boolean {
        val behavior = BottomSheetBehavior.from(highlightedDealerContainer)
        if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            this.setSmallIconForLastClickedMarker(this.lastClickedMarker)

            this.lastClickedMarker = null
            showDealerList()

            return true
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun loadLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            if (location != null) {
                moveMapCamera(LatLng(location.latitude, location.longitude))
                fab.setOnClickListener {
                    setSmallIconForLastClickedMarker(lastClickedMarker)
                    lastClickedMarker = null
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
                    showDealerList()
                }
                googleMap?.isMyLocationEnabled = true
                googleMap?.uiSettings?.isMyLocationButtonEnabled
            } else {
                loadDefaultLocation()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        this.mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        this.mapView.onResume()
    }

    override fun onPause() {
        this.mapView.onPause()
        resetDialog()
        super.onPause()
    }

    override fun onDestroy() {
        this.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        this.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.mapView.onSaveInstanceState(outState)
    }

    private fun loadDefaultLocation() = moveMapCamera(LatLng(DEFAULT_LAT, DEFAULT_LONG))

    private fun moveMapCamera(latLng: LatLng) {
        viewModel.searchDealer(latLng).observe(this, Observer {results ->
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))

            clusterManager = ClusterManager<SearchDealer>(requireContext(), googleMap)
            clusterManager?.renderer = CustomIconRender(requireContext(), googleMap, clusterManager)
            clusterManager?.addItems(results)
            clusterManager?.setOnClusterClickListener { onClusterClicked(it) }
            clusterManager?.setOnClusterItemClickListener { onMarkerClicked(it) }
            clusterManager?.cluster()

            googleMap?.setOnCameraIdleListener(clusterManager)
            googleMap?.uiSettings?.isRotateGesturesEnabled = false
            googleMap?.uiSettings?.isMapToolbarEnabled = false
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            googleMap?.setOnCameraIdleListener { onCameraIdle() }
            googleMap?.setOnMarkerClickListener(clusterManager)
            googleMap?.setOnMapClickListener { onMapClicked(it) }

            recyclerView.adapter = DealerLocatorListAdapter(results?.toMutableList() ?: mutableListOf(), listener)
            recyclerView.addItemDecoration(ItemDivider(requireContext(), R.drawable.divider))
            showDealerList()
        })
    }

    private fun onClusterClicked(cluster: Cluster<SearchDealer>): Boolean {
        val zoom: Float = this.googleMap?.cameraPosition?.zoom ?: 15.toFloat()
        val zoomLvl = (zoom + 1.0).toInt()

        this.zoomToLatLng(latLng = cluster.position, zoomLevel = zoomLvl, animate = true)

        return true
    }

    private fun onMarkerClicked(dealerItem: SearchDealer): Boolean {
        if (this.lastClickedMarker?.equals(dealerItem) == false) {
            this.setSmallIconForLastClickedMarker(this.lastClickedMarker)
        }

        val renderer = this.clusterManager?.renderer as CustomIconRender?
        renderer?.getMarker(dealerItem)?.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker_large)))
        this.lastClickedMarker = dealerItem
        showSelectedDealer()
        val dealerView = DealerView(selectedDealerView, listener)
        dealerView.onBind(dealerItem)

        return false
    }

    private fun onCameraIdle() {
        clusterManager?.onCameraIdle()
    }

    private fun onMapClicked(latLng: LatLng) {
        if (this.lastClickedMarker?.position?.equals(latLng) == false) {
            this.setSmallIconForLastClickedMarker(this.lastClickedMarker)

            this.lastClickedMarker = null
            showDealerList()
        }
    }

    private fun setSmallIconForLastClickedMarker(dealerItem: SearchDealer?) {
        val renderer = this.clusterManager?.renderer as CustomIconRender?
        renderer?.getMarker(this.lastClickedMarker)?.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker)))

    }

    private fun zoomToLatLng(latLng: LatLng, zoomLevel: Int = 15, animate: Boolean = true) {
        val center = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel.toFloat())

        when(animate) {
            true -> this.googleMap?.animateCamera(center)
            else -> this.googleMap?.moveCamera(center)
        }
    }

    private fun showDealerList() {
        listContainer.hideableBehavior(false)
        listContainer.collapse()
        highlightedDealerContainer.hideableBehavior(true)
        highlightedDealerContainer.hide()
        mapView.switchAnchorTo(listContainer.id)
        fab.switchAnchorTo(listContainer.id)
    }

    private fun showSelectedDealer() {
        listContainer.hideableBehavior(true)
        listContainer.hide()
        highlightedDealerContainer.collapse()
        highlightedDealerContainer.hideableBehavior(false)
        mapView.switchAnchorTo(highlightedDealerContainer.id)
        fab.switchAnchorTo(highlightedDealerContainer.id)

        if (mapView.height != highlightedDealerContainer.top) {
            val slideAnim = ValueAnimator.ofInt(mapView.height, highlightedDealerContainer.top)
                .setDuration(300)
            slideAnim.addUpdateListener {
                val layoutParams = mapView.layoutParams as CoordinatorLayout.LayoutParams
                layoutParams.height = it.animatedValue as Int

                mapView.requestLayout()
            }

        }

    }

    private fun resetDialog() {
        dialog?.dismiss()
        dialog = null
    }
}

private class CustomIconRender(context: Context, map: GoogleMap?, clusterManager: ClusterManager<SearchDealer>?)
    : DefaultClusterRenderer<SearchDealer>(context, map, clusterManager) {

    private val clusterIcon: Bitmap = BitmapUtils.createFromSvg(context, R.drawable.ic_map_marker)

    override fun onBeforeClusterItemRendered(item: SearchDealer?, markerOptions: MarkerOptions?) {
        markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(clusterIcon))
    }
}

private fun View.setPeekHeight(height: Int) {
    val behavior = BottomSheetBehavior.from(this)
    behavior.peekHeight = height
}

private fun View.switchAnchorTo(id: Int) {
    val layoutParams = this.layoutParams as CoordinatorLayout.LayoutParams
    layoutParams.anchorId = id
    this.layoutParams = layoutParams
}

private fun View.collapse() {
    val behavior = BottomSheetBehavior.from(this)
    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
}

private fun View.hide() {
    val behavior = BottomSheetBehavior.from(this)
    behavior.state = BottomSheetBehavior.STATE_HIDDEN
}

private fun View.hideableBehavior(boolean: Boolean) {
    val behavior = BottomSheetBehavior.from(this)
    behavior.isHideable = boolean
}

interface BackableFragment {
    fun onBackPressed(): Boolean
}