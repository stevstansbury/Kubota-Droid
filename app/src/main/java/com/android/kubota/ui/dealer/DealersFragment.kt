package com.android.kubota.ui.dealer

import android.app.Activity
import android.content.Intent
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.kubota.R
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.SearchActivity
import com.android.kubota.ui.SearchResults
import com.android.kubota.viewmodel.dealers.DealerViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.lang.ref.WeakReference
import androidx.lifecycle.Observer

private const val SEARCH_REQUEST_CODE = 100

class DealersFragment: BaseFragment(), DealerLocatorController {

    override val layoutResId: Int = R.layout.fragment_dealers

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var searchView: TextView
    private lateinit var progressBar: ProgressBar

    private var dealerLocator: DealerLocator? = null

    private val searchHintTextColor: Int
            by lazy { ContextCompat.getColor(requireContext(), R.color.dealers_search_hint_text_color) }
    private val searchTextColor: Int
            by lazy { ContextCompat.getColor(requireContext(), R.color.dealers_search_text_color) }

    private val viewModel: DealerViewModel by lazy {
        DealerViewModel.instance(
            owner = this,
            application = requireActivity().application,
            signInHandler = WeakReference { this.signInAsync() }
        )
    }

    override fun initUi(view: View) {
        view.findViewById<View>(R.id.dealersToolbar).setOnClickListener {
            val intent = Intent(activity, SearchActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivityForResult(
                intent,
                SEARCH_REQUEST_CODE
            )
        }
        progressBar = view.findViewById(R.id.toolbarProgressBar)
        searchView = view.findViewById(R.id.searchView)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.pager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = DealersAdapter(
            childFragmentManager,
            lifecycle
        )

        val locatorTabTitle = getString(R.string.locator_tab)
        val favoritedTabTitle = getString(R.string.my_dealers_tab)
        val locatorTabDrawable = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_map_marker_white
        )
        val favoritedTabDrawable = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_star_filled_white
        )
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> locatorTabTitle
                else -> favoritedTabTitle
            }
            tab.icon = when(position) {
                0 -> locatorTabDrawable
                else -> favoritedTabDrawable
            }

        }.attach()
    }

    override fun loadData() {
        this.viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            when (loading) {
                true -> this.showProgressBar()
                else -> this.hideProgressBar()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getParcelableExtra<SearchResults>(SearchActivity.KEY_SEARCH_RESULT)?.let {searchResults ->
                // Pass the Latitude and Longitude to the fragment
                dealerLocator?.lifecycleScope?.launchWhenStarted {
                    dealerLocator?.onSearchResult(searchResults.latLng)
                    searchView.setTextColor(searchTextColor)
                    searchView.text = searchResults.name
                }
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                }
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment is DealerLocator) {
            dealerLocator = childFragment
        }
    }

    override fun onMyLocationButtonClicked() {
        searchView.setTextColor(searchHintTextColor)
        searchView.setText(R.string.dealers_search_hint)
    }

    override fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }
}

class DealersAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
       return when(position) {
           0 -> DealerLocatorFragment()
           else -> MyDealersListFragment()
       }
    }
}