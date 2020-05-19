package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.extensions.isLocationEnabled
import com.android.kubota.extensions.showDialog
import com.android.kubota.utility.*
import com.android.kubota.utility.Utils.createMustLogInDialog
import com.android.kubota.viewmodel.DealerLocatorViewModel
import com.android.kubota.viewmodel.SearchDealer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.inmotionsoftware.promisekt.*
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.util.*
import kotlin.random.Random

private const val DEFAULT_LAT= 32.9792895
private const val DEFAULT_LONG = -97.0315917
private const val DEFAULT_ZOOM = 8f

class DealerLocatorFragment : Fragment(), DealerLocator {
    private var dialog: AlertDialog? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: DealerLocatorViewModel

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

    private var selectedDealerLiveData: LiveData<Boolean>? = null
    private val selectedDealerObserver = Observer<Boolean> {isFavorited ->
        (lastClickedMarker?.tag as? SearchDealer)?.let {
            if (it.isFavorited != isFavorited) {
                val newDealerVal = SearchDealer(it.serverId, it.name, it.streetAddress, it.city,
                    it.stateCode, it.postalCode, it.countryCode, it.phone,
                    it.webAddress, it.dealerNumber, it.latitude, it.longitude,
                    it.distance, isFavorited)

                //update the tag
                lastClickedMarker?.tag = newDealerVal

                dealerView.onBind(newDealerVal)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val listener = object: DealerView.OnClickListener {

        override fun onStarClicked(dealer: SearchDealer) {
            when {
                dealer.isFavorited -> {
                    viewModel.deleteFavoriteDealer(dealer)
                }
                canAddDealer -> {
                    viewModel.insertFavorite(dealer)
                }
                else -> {
                    resetDialog()

                    dialog = createMustLogInDialog(requireContext(), Utils.LogInDialogMode.DEALER_MESSAGE)
                    dialog?.setOnCancelListener { resetDialog() }
                    dialog?.show()
                }
            }
        }

        override fun onWebClicked(url: String) {
            val addr = if (!url.startsWith("http", ignoreCase = true)) {
                "https://www.kubotausa.com/dealers/${url}"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(addr))
            startActivity(intent)
        }

        @SuppressLint("MissingPermission")
        override fun onCallClicked(number: String) {
            PermissionRequestManager
                .requestPermission(requireActivity(), Manifest.permission.CALL_PHONE)
                .map {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${number}"))
                    requireActivity().startActivity(intent)
                }
                .recover {
                    showDialog(message="Permission to make phone was not granted", positiveButton="Ok", cancelable=false)
                }
        }

        override fun onDirClicked(addr: String) {
            val uri: Uri = Uri.parse("google.navigation:q=$addr")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }

        override fun onDirClicked(loc: LatLng) {
            val uri: Uri = Uri.parse("google.navigation:q=${loc.latitude},${loc.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        controller = when {
            parentFragment is DealerLocatorController -> parentFragment as DealerLocatorController
            context is DealerLocatorController -> context
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val factory = InjectorUtils.provideDealerLocatorViewModel(requireContext())
        viewModel = ViewModelProvider(this, factory)
            .get(DealerLocatorViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dealer_locator, null)
        selectedDealerView = view.findViewById(R.id.selectedDealerView)
        fragmentPane = view.findViewById(R.id.mapFragmentPane)
        dealerView = DealerView(selectedDealerView, listener)
        listContainer = view.findViewById(R.id.bottomSheetList)
        listContainer.hideableBehavior(false)
        listContainer.setPeekHeight(resources.getDimensionPixelSize(R.dimen.locator_dealer_list_peek_height))
        highlightedDealerContainer = view.findViewById(R.id.highlightedDealerContainer)
        highlightedDealerContainer.hideableBehavior(true)
        highlightedDealerContainer.hide()
        fab = view.findViewById(R.id.locationButton)
        viewModel.canAddPreference.observe(viewLifecycleOwner, Observer {
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

        return view
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

        viewModel.searchDealer(latLng).observe(viewLifecycleOwner, Observer {results ->
            dealersList = results ?: emptyList()

            enterListMode(latLng, dealersList)
        })
    }

    private fun enterListMode(latLng: LatLng, dealerList: List<SearchDealer>) {
        if (highlightedDealerContainer.visibility != View.VISIBLE) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,
                DEFAULT_ZOOM
            ))

            val markerTag = lastClickedMarker?.tag
            lastClickedMarker = null
            dealerList.forEach {
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
                        .position(LatLng(it.latitude, it.longitude))
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
                    dealerList.toMutableList(),
                    listener
                )

            showDealerList()
        }
    }

    private fun enterSelectedDealerMode(marker: Marker) {
        (lastClickedMarker?.tag as? SearchDealer)?.let {
            if (it != marker.tag) {
                selectedDealerLiveData?.removeObserver(selectedDealerObserver)
                selectedDealerLiveData = null
                this.setSmallIconForLastClickedMarker()
            }
        }

        (marker.tag as? SearchDealer)?.let {
            this.lastClickedMarker?.isVisible = false
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapUtils.createFromSvg(requireContext(), R.drawable.ic_map_marker_large)))
            this.lastClickedMarker = marker
            showSelectedDealer()
            dealerView.onBind(it)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            selectedDealerLiveData = viewModel.isFavoritedDealer(it.dealerNumber).apply {
                observe(viewLifecycleOwner, selectedDealerObserver)
            }
        }
    }

    private fun onExitSelectedDealerMode() {
        (lastClickedMarker?.tag as? SearchDealer)?.let {
            selectedDealerLiveData?.removeObserver(selectedDealerObserver)
        }
        setSmallIconForLastClickedMarker()
        this.lastClickedMarker = null
        recyclerView.adapter =
            DealerLocatorListAdapter(
                dealersList.toMutableList(),
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
        viewModel.searchDealer(latLng).observe(viewLifecycleOwner, Observer {results ->
            dealersList = results ?: emptyList()

            enterListMode(latLng, dealersList)
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