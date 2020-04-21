package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.kubota.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

private const val SEARCH_REQUEST_CODE = 100

class DealersFragment: BaseFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.title = getString(R.string.dealer_title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_dealers, null)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = DealersAdapter(parentFragmentManager, lifecycle)

        val locatorTabTitle = getString(R.string.locator_tab)
        val favoritedTabTitle = getString(R.string.my_dealers_tab)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> locatorTabTitle
                else -> favoritedTabTitle
            }
        }.attach()

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                val intent = Intent(this.activity, SearchActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivityForResult(intent, SEARCH_REQUEST_CODE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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