package com.android.kubota.ui

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import android.text.style.StyleSpan
import android.view.*
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.crashlytics.android.Crashlytics
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.compat.AutocompleteFilter
import com.google.android.libraries.places.compat.AutocompletePrediction
import com.google.android.libraries.places.compat.Places
import com.google.android.libraries.places.widget.AutocompleteActivity
import kotlinx.android.parcel.Parcelize

private const val USA_COUNTRY_FILTER = "US"

class SearchActivity : AppCompatActivity() {
    companion object {
        const val KEY_SEARCH_RESULT = "SEARCH_RESULT"

        private const val RENDER_HINTS_AFTER_LENGTH = 3
    }

    private lateinit var searchView: SearchView
    private lateinit var closeButton: View
    private lateinit var hintRecyclerView: RecyclerView

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        hintRecyclerView = findViewById<RecyclerView>(R.id.kubotaSearchHintRecyclerView).apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_activity_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        menu.findItem(R.id.search).let {
            it.expandActionView()
            searchView = it.actionView as SearchView
            searchView.apply {
                setSearchableInfo(searchManager.getSearchableInfo(componentName))
                isIconified = false
                queryHint = getString(R.string.dealers_search_hint)

                findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                    setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                }
                findViewById<TextView>(androidx.appcompat.R.id.search_src_text).apply {
                    setHintTextColor(ContextCompat.getColor(context, R.color.dealers_search_hint_text_color))
                }
                closeButton = findViewById<View>(androidx.appcompat.R.id.search_close_btn)
                closeButton.visibility = View.GONE

                setOnQueryTextListener(queryListener)

                setOnCloseListener {
                    finish()
                    return@setOnCloseListener true
                }
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home -> {
                hintRecyclerView.hideKeyboard()
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onHintAdapterFor(query: String) {
        val geoClient = Places.getGeoDataClient(this)
        val task = geoClient.getAutocompletePredictions(query, null, AutocompleteFilter.Builder()
            .setTypeFilter(AutocompleteFilter.TYPE_FILTER_REGIONS.and(AutocompleteFilter.TYPE_FILTER_CITIES))
            .setCountry(USA_COUNTRY_FILTER)
            .build())

        task.addOnSuccessListener {
            val list = ArrayList<AutocompletePrediction>(it.count)
            it.forEach { list.add(it) }
            hintRecyclerView.adapter = DealersSearchHintListAdapter(list) {
                val intent = Intent()
                geoClient.getPlaceById(it.placeId)
                    .addOnSuccessListener {
                        hintRecyclerView.hideKeyboard()
                        it.get(0)?.let {
                            intent.putExtra(KEY_SEARCH_RESULT, SearchResults(it.name.toString(), it.latLng))
                            this.setResult(Activity.RESULT_OK, intent)
                            this.finish()
                        }
                    }
                    .addOnFailureListener {
                        Crashlytics.logException(it)
                        this.setResult(AutocompleteActivity.RESULT_ERROR, intent)
                        this.finish()
                    }
            }
        }.addOnFailureListener {
            Crashlytics.logException(it)
        }
    }

    private fun query(query: String?) {
        if (query != null && query.length >= RENDER_HINTS_AFTER_LENGTH) {
            onHintAdapterFor(query)
        } else {
            hintRecyclerView.adapter = null
        }
    }

    private val queryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            query?.let { searchView.clearFocus() }
            query(query)

            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            closeButton.visibility = if (newText.isNullOrEmpty()) View.GONE else View.VISIBLE
            return true
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                query(query)
            }
        }
    }
}

//
// Search - Dealer Selectable Results
//
class SelectableDealerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val line1: TextView = itemView.findViewById(R.id.line1)
    private val line2: TextView = itemView.findViewById(R.id.line2)

    var prediction: AutocompletePrediction? = null
        set(value) {
            value?.let { prediction ->
                line1.text = prediction.getPrimaryText(StyleSpan(Typeface.NORMAL))
                line2.text = prediction.getSecondaryText(StyleSpan(Typeface.NORMAL))
            }
            field = value
        }

    var onItemSelected: (() -> Unit)? = null
        set(value) {
            field = value
            itemView.setOnClickListener { value?.invoke() }
        }
}

class DealersSearchHintListAdapter(
    private val data: List<AutocompletePrediction>,
    private val onSelect: ((city: AutocompletePrediction) -> Unit)
) : RecyclerView.Adapter<SelectableDealerViewHolder>() {

    private var onItemSelected: ((query: String) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SelectableDealerViewHolder {
        val view = LayoutInflater
            .from(viewGroup.context)
            .inflate(
                R.layout.view_dealer_search_hint,
                viewGroup, false
            )
        return SelectableDealerViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    private fun onBind(viewHolder: SelectableDealerViewHolder, position: Int) {
        viewHolder.prediction = data[position]
        viewHolder.onItemSelected = { onSelect.invoke(data[viewHolder.adapterPosition]) }
    }

    private fun getSearchQueryAt(position: Int): String? = data[position].toString()

    override fun onBindViewHolder(viewHolder: SelectableDealerViewHolder, position: Int) {
        viewHolder.itemView.setOnClickListener {
            val query = getSearchQueryAt(viewHolder.adapterPosition)
            onItemSelected?.invoke(query ?: "")
        }
        onBind(viewHolder, position)
    }
}

@Parcelize
data class SearchResults(val name: String, val latLng: LatLng) : Parcelable