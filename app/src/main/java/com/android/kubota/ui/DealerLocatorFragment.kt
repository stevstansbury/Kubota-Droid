package com.android.kubota.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.*
import com.android.kubota.R
import com.android.kubota.ui.ChooseEquipmentFragment.Companion.KEY_SEARCH_RESULT
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

private const val DEFAULT_LAT= 32.9792895
private const val DEFAULT_LONG = -97.0315917
private const val SEARCH_REQUEST_CODE = 100
private const val DEFAULT_ZOOM = 8f

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
    private var lastClickedMarker: Marker? = null

    private var canAddDealer: Boolean = false
    private var isSearchMode: Boolean = false
    private var searchDealersList: List<SearchDealer> = emptyList()
    private var location = LatLng(DEFAULT_LAT, DEFAULT_LONG)

    private val listener = object: DealerView.OnClickListener {

        override fun onClick(dealer: SearchDealer) {
            val detailFragment = DealerDetailFragment.createIntance(UIDealer(id = -1, name = dealer.name,
                address = dealer.streetAddress, city = dealer.city, state = dealer.stateCode, postalCode = dealer.postalCode,
                phone = dealer.phone, website = dealer.webAddress, dealerNumber = dealer.dealerNumber))

            if (isSearchMode) isSearchMode = false

            flowActivity?.addFragmentToBackStack(detailFragment)

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

        activity?.title = getString(R.string.dealer_locator_title)
        setHasOptionsMenu(true)
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

            loadLocation()
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.dealersList).apply {
            setHasFixedSize(true)
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.search_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.search -> {
                val intent = Intent(this.activity, SearchActivity::class.java)
                    .putExtra(SearchActivity.KEY_MODE, SearchActivity.DEALERS_LOCATOR_MODE)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

                startActivityForResult(intent, SEARCH_REQUEST_CODE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getParcelableExtra<SearchDealer>(KEY_SEARCH_RESULT)?.let {
                enterSearchMode(it)
            }

            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed(): Boolean {
        val behavior = BottomSheetBehavior.from(highlightedDealerContainer)
        if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {

            if (isSearchMode) {
                exitSearchMode(location)
            } else {
                setSmallIconForLastClickedMarker()
                showDealerList()
            }
            return true
        }

        return false
    }

    override fun onStart() {
        super.onStart()
        this.mapView.onStart()
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

    private fun loadLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fab.visibility = View.GONE
            loadDefaultLocation()
        } else {
            // Permission has already been granted
            loadLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation : Location? ->
            if (lastLocation != null) {
                location = LatLng(lastLocation.latitude, lastLocation.longitude)
                searchArea(location)

                fab.setOnClickListener {
                    if (isSearchMode) {
                        exitSearchMode(location)
                    } else {
                        enterListMode(location, searchDealersList)
                        setSmallIconForLastClickedMarker()
                    }
                }

                googleMap?.isMyLocationEnabled = true
            } else {
                loadDefaultLocation()
            }
        }
    }

    private fun enterSearchMode(searchDealer: SearchDealer) {
        googleMap?.clear()
        val marker = googleMap?.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker_large)))
            .position(LatLng(searchDealer.latitude, searchDealer.longitude)))
        marker?.tag = searchDealer
        isSearchMode = true

        googleMap?.setOnMapClickListener {  }

        zoomToLatLng(latLng = LatLng(searchDealer.latitude, searchDealer.longitude), animate = false)

        lastClickedMarker = marker
        showSelectedDealer()
        val dealerView = DealerView(selectedDealerView, listener)
        dealerView.onBind(searchDealer)
    }

    private fun exitSearchMode(latLng: LatLng) {
        googleMap?.clear()
        isSearchMode = false
        lastClickedMarker = null

        enterListMode(latLng, searchDealersList)
        lastClickedMarker = null
    }

    private fun enterListMode(latLng: LatLng, dealerList: List<SearchDealer>) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))

        dealerList.forEach {
            val marker = googleMap?.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker)))
                .position(LatLng(it.latitude, it.longitude))
                .draggable(false))
            marker?.tag = it
        }

        googleMap?.uiSettings?.isRotateGesturesEnabled = false
        googleMap?.uiSettings?.isMapToolbarEnabled = false
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        googleMap?.setOnMarkerClickListener{ marker ->
            enterSelectedDealerMode(marker)
            return@setOnMarkerClickListener true
        }
        googleMap?.setOnMapClickListener { onMapClicked(it) }

        recyclerView.adapter = DealerLocatorListAdapter(dealerList.toMutableList(), listener)
        recyclerView.addItemDecoration(ItemDivider(requireContext(), R.drawable.divider))

        showDealerList()
    }

    private fun enterSelectedDealerMode(marker: Marker) {
        if (this.lastClickedMarker?.tag?.equals(marker.tag) == false) {
            this.setSmallIconForLastClickedMarker()
        }

        (marker.tag as? SearchDealer)?.let {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker_large)))
            this.lastClickedMarker = marker
            showSelectedDealer()
            val dealerView = DealerView(selectedDealerView, listener)
            dealerView.onBind(it)
        }
    }

    private fun loadDefaultLocation() = searchArea(LatLng(DEFAULT_LAT, DEFAULT_LONG))

    private fun searchArea(latLng: LatLng) {
        viewModel.searchDealer(latLng).observe(this, Observer {results ->
            searchDealersList = results ?: emptyList()

            if (isSearchMode) return@Observer

            enterListMode(latLng, searchDealersList)
        })
    }

    private fun onMapClicked(latLng: LatLng) {
        if (this.lastClickedMarker?.position?.equals(latLng) == false) {
            this.setSmallIconForLastClickedMarker()

            this.lastClickedMarker = null
            showDealerList()
        }
    }

    private fun setSmallIconForLastClickedMarker() {
        lastClickedMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker)))
    }

    private fun zoomToLatLng(latLng: LatLng, zoomLevel: Int = 15, animate: Boolean = true) {
        val center = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel.toFloat())

        when(animate) {
            true -> this.googleMap?.animateCamera(center)
            else -> this.googleMap?.moveCamera(center)
        }
    }

    private fun showDealerList() {
        val behavior = BottomSheetBehavior.from(listContainer)
        if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            listContainer.hideableBehavior(false)
            listContainer.collapse()
            highlightedDealerContainer.hideableBehavior(true)
            highlightedDealerContainer.hide()
            mapView.switchAnchorTo(listContainer.id)
            fab.switchAnchorTo(listContainer.id)
        }
    }

    private fun showSelectedDealer() {
        listContainer.hideableBehavior(true)
        listContainer.hide()

        highlightedDealerContainer.collapse()
        highlightedDealerContainer.hideableBehavior(false)

        mapView.switchAnchorTo(highlightedDealerContainer.id)
        fab.switchAnchorTo(highlightedDealerContainer.id)
    }

    private fun resetDialog() {
        dialog?.dismiss()
        dialog = null
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