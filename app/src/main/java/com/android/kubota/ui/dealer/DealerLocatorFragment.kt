package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.extensions.isLocationEnabled
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.equipment.ModelManualFragment
import com.android.kubota.utility.*
import com.android.kubota.utility.Utils.createMustLogInDialog
import com.android.kubota.viewmodel.dealers.DealerViewModel
import com.android.kubota.viewmodel.dealers.SearchDealer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.Dealer
import java.lang.ref.WeakReference
import java.util.*

const val DEFAULT_LAT= 32.9792895
const val DEFAULT_LONG = -97.0315917
const val DEFAULT_ZOOM = 8f

class DealerLocatorFragment : BaseFragment(), DealerLocator {

    override val layoutResId: Int = R.layout.fragment_dealer_locator

    private val viewModel: DealerViewModel by lazy {
        DealerViewModel.instance(
            owner = this.requireActivity(),
            application = requireActivity().application,
            signInHandler = WeakReference { this.signInAsync() }
        )
    }

    private var dialog: AlertDialog? = null

    private lateinit var listContainer: ViewGroup
    private lateinit var highlightedDealerContainer: ViewGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: View
    private lateinit var fragmentPane: View
    private lateinit var selectedDealerView: View
    private lateinit var dealerView: DealerView
    private var googleMap: GoogleMap? = null
    private var lastClickedMarker: Marker? = null

    private var canAddDealer: Boolean = false
    private var dealersList: List<SearchDealer> = emptyList()
    private val viewModeStack = Stack<LatLng>()
    private var controller: DealerLocatorController? = null

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this.requireActivity())
    }

    private var selectedDealerLiveData: LiveData<Boolean>? = null

    private val selectedDealerObserver = Observer<Boolean> { isFavorited ->
        (lastClickedMarker?.tag as? SearchDealer)?.let {
            if (viewModel.isFavorited(it.toDealer()) != isFavorited) {
                //update the tag
                lastClickedMarker?.tag = it
                dealerView.onBind(it)
            }
        }
    }

    private val listener by lazy {
        DealerViewListener(this,  viewModel)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        controller = when {
            parentFragment is DealerLocatorController -> parentFragment as DealerLocatorController
            context is DealerLocatorController -> context
            else -> null
        }
    }

    override fun initUi(view: View) {
        selectedDealerView = view.findViewById(R.id.selectedDealerView)
        fragmentPane = view.findViewById(R.id.mapFragmentPane)
        dealerView = DealerView(selectedDealerView, viewModel, listener)
        listContainer = view.findViewById(R.id.bottomSheetList)
        listContainer.hideableBehavior(false)
        listContainer.setPeekHeight(resources.getDimensionPixelSize(R.dimen.locator_dealer_list_peek_height))
        highlightedDealerContainer = view.findViewById(R.id.highlightedDealerContainer)
        highlightedDealerContainer.hideableBehavior(true)
        highlightedDealerContainer.hide()
        fab = view.findViewById(R.id.locationButton)

        viewModel.canAddToFavorite.observe(viewLifecycleOwner, Observer {
            this.canAddDealer = it ?: false
        })

        fab.setOnClickListener {
            if (!viewModeStack.empty()) {
                if (viewModeStack.size == 1) {
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLng(viewModeStack.peek()))
                } else {
                    do {
                        viewModeStack.pop()
                    } while (viewModeStack.size > 1)
                    controller?.onMyLocationButtonClicked()
                    onViewStateStackChanged()
                }
            }
        }
        recyclerView = view.findViewById<RecyclerView>(R.id.dealersList).apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        val fragmentTransaction = childFragmentManager.beginTransaction()

        (childFragmentManager.findFragmentById(R.id.mapFragmentPane) as? SupportMapFragment)?.let {childFragment ->
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

    override fun loadData() {
        viewModel.favoriteDealers.observe(viewLifecycleOwner, Observer {
            recyclerView.adapter?.notifyDataSetChanged()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            requireActivity()
                .onBackPressedDispatcher
                .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        val behavior = BottomSheetBehavior.from(listContainer)

                        if (highlightedDealerContainer.visibility == View.VISIBLE) {
                            onExitSelectedDealerMode()
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(viewModeStack.peek()))
                        } else if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        } else if (viewModeStack.size > 1) {
                            viewModeStack.pop()
                            onViewStateStackChanged()
                        }
                    }
                })
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        if (childFragment is SupportMapFragment) {
            getMapAsync(childFragment)
        }
    }

    private fun getMapAsync(supportMapFragment: SupportMapFragment) {
        supportMapFragment.getMapAsync {
            this.googleMap = it

            if (viewModeStack.isEmpty()) {
                loadLocation()
            } else {
                onViewStateStackChanged()
            }
        }
    }

    private fun loadLocation() {
        if (!requireContext().isLocationEnabled()) {
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
            if (!this@DealerLocatorFragment.isVisible) {
                return@addOnSuccessListener
            }
            if (lastLocation != null) {
                viewModeStack.push(LatLng(lastLocation.latitude, lastLocation.longitude))
                onViewStateStackChanged()
                googleMap?.isMyLocationEnabled = true
            } else {
                loadDefaultLocation()
            }
        }
    }

    private fun enterSearchMode(latLng: LatLng) {
        googleMap?.clear()
        lastClickedMarker = null

        viewModel.searchNearestDealers(latLng).observe(viewLifecycleOwner, Observer {
            dealersList = it
            enterListMode(latLng, it)
        })
    }

    private fun enterListMode(latLng: LatLng, dealers: List<SearchDealer>) {
        if (highlightedDealerContainer.visibility != View.VISIBLE) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,
                DEFAULT_ZOOM
            ))

            val markerTag = lastClickedMarker?.tag
            lastClickedMarker = null
            dealers.forEach {
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                BitmapUtils.createFromSvg(
                                    requireContext(),
                                    R.drawable.ic_map_marker
                                )
                            )
                        )
                        .position(LatLng(it.location.latitude, it.location.longitude))
                        .draggable(false)
                )
                marker?.tag = it

                // Reset the marker since we cleared the map
                if (markerTag == it) {
                    lastClickedMarker = marker
                }
            }

            googleMap?.uiSettings?.isRotateGesturesEnabled = false
            googleMap?.uiSettings?.isMapToolbarEnabled = false
            googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            googleMap?.setOnMarkerClickListener { marker ->
                enterSelectedDealerMode(marker)
                return@setOnMarkerClickListener true
            }
            googleMap?.setOnMapClickListener { onMapClicked(it) }

            recyclerView.adapter =
                DealerLocatorListAdapter(
                    dealers.toMutableList(),
                    viewModel,
                    listener
                )

            showDealerList()
        }
    }

    private fun enterSelectedDealerMode(marker: Marker) {
        (lastClickedMarker?.tag as? Dealer)?.let {
            if (it != marker.tag) {
                selectedDealerLiveData?.removeObserver(selectedDealerObserver)
                selectedDealerLiveData = null
                this.setSmallIconForLastClickedMarker()
            }
        }

        (marker.tag as? SearchDealer)?.let {
            if (this.lastClickedMarker === marker) {
                return@let
            }

            // reset the marker icon
            setSmallIconForLastClickedMarker()
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker_large)))
            this.lastClickedMarker = marker
            showSelectedDealer()
            dealerView.onBind(it)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            selectedDealerLiveData = MutableLiveData(viewModel.isFavorited(it.toDealer())).apply {
                observe(viewLifecycleOwner, selectedDealerObserver)
            }
        }
    }

    private fun onExitSelectedDealerMode() {
        (lastClickedMarker?.tag as? Dealer)?.let {
            selectedDealerLiveData?.removeObserver(selectedDealerObserver)
        }
        setSmallIconForLastClickedMarker()
        this.lastClickedMarker = null
        recyclerView.adapter =
            DealerLocatorListAdapter(
                dealersList.toMutableList(),
                viewModel,
                listener
            )
        showDealerList()
    }

    private fun loadDefaultLocation() {
        viewModeStack.push(LatLng(
            DEFAULT_LAT,
            DEFAULT_LONG
        ))
        onViewStateStackChanged()
    }

    private fun searchArea(latLng: LatLng) {
        viewModel.searchNearestDealers(latLng).observe(viewLifecycleOwner, Observer {
            dealersList = it
            enterListMode(latLng, it)
        })
    }

    private fun onMapClicked(latLng: LatLng) {
        if (this.lastClickedMarker?.position?.equals(latLng) == false) {
            onExitSelectedDealerMode()
        }
    }

    private fun setSmallIconForLastClickedMarker() {
        lastClickedMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker)))
    }

    private fun showDealerList() {
        highlightedDealerContainer.hideableBehavior(true)
        highlightedDealerContainer.hide()

        listContainer.hideableBehavior(false)
        listContainer.show()

        fragmentPane.switchAnchorTo(listContainer.id)
        fab.switchAnchorTo(listContainer.id)
    }

    private fun showSelectedDealer() {
        listContainer.hideableBehavior(true)
        listContainer.hide()

        highlightedDealerContainer.show()
        highlightedDealerContainer.hideableBehavior(false)

        fragmentPane.switchAnchorTo(highlightedDealerContainer.id)
        fab.switchAnchorTo(highlightedDealerContainer.id)
    }

    private fun resetDialog() {
        dialog?.dismiss()
        dialog = null
    }

    private fun onViewStateStackChanged() {
        val behavior = BottomSheetBehavior.from(listContainer)
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        if (viewModeStack.isEmpty()) {
            loadLocation()
        } else {
            setSmallIconForLastClickedMarker()
            this.lastClickedMarker = null
            if (highlightedDealerContainer.visibility == View.VISIBLE) {
                showDealerList()
                googleMap?.clear()
            }
            val latLng = viewModeStack.peek()
            if (viewModeStack.size > 1) {
                enterSearchMode(latLng)
            } else {
                searchArea(latLng)
            }
        }
    }

    override fun onSearchResult(latLng: LatLng) {
        viewModeStack.push(latLng)
        onViewStateStackChanged()
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

private fun View.show() {
    this.visibility = View.VISIBLE
}

private fun View.hide() {
    this.visibility = View.GONE
}

private fun View.hideableBehavior(boolean: Boolean) {
    val behavior = BottomSheetBehavior.from(this)
    behavior.isHideable = boolean
}

interface DealerLocator: LifecycleOwner {
    fun onSearchResult(latLng: LatLng)
}

interface DealerLocatorController {
    fun onMyLocationButtonClicked()
}