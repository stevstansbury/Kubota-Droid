package com.android.kubota.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.graphics.Typeface
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import android.text.style.StyleSpan
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.Constants
import com.android.kubota.utility.Constants.VIEW_MODE_DEALERS_SEARCH
import com.android.kubota.utility.Constants.VIEW_MODE_EQUIPMENT_SEARCH
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.*
import com.google.android.libraries.places.compat.AutocompletePrediction
import java.lang.IllegalStateException

class SearchActivity : AppCompatActivity() {
    companion object {
        const val KEY_MODE = "searchMode"
        private const val UNKNOWN_MODE = 0
        const val ADD_EQUIPMENT_MODE = 1
        const val DEALERS_LOCATOR_MODE = 2

        private const val RENDER_HINTS_AFTER_LENGTH = 3
    }

    private var searchMode: Int = UNKNOWN_MODE
    private lateinit var viewModel: SearchViewModel
    private lateinit var searchView: SearchView
    private lateinit var hintRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        hintRecyclerView = findViewById<RecyclerView>(R.id.kubotaSearchHintRecyclerView).apply {
            layoutManager = LinearLayoutManager(context)
        }

        extractMode()
        when(searchMode) {
            ADD_EQUIPMENT_MODE -> Constants.Analytics.setViewMode(VIEW_MODE_EQUIPMENT_SEARCH)
            DEALERS_LOCATOR_MODE -> Constants.Analytics.setViewMode(VIEW_MODE_DEALERS_SEARCH)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_activity_menu, menu)

        menu.findItem(R.id.search).let {
            it.expandActionView()
            searchView = it.actionView as SearchView
            searchView.apply {
                isIconified = false
                queryHint = when(searchMode) {
                    ADD_EQUIPMENT_MODE -> getString(R.string.equipment_search_hint)
                    else -> getString(R.string.dealers_search_hint)
                }

                findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                    setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                }

                setOnQueryTextListener(queryListener)

                setOnCloseListener {
                    finish()
                    return@setOnCloseListener true
                }
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (item.itemId){
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun extractMode() {
        searchMode = intent.getIntExtra(KEY_MODE, UNKNOWN_MODE)

        when (searchMode) {
            ADD_EQUIPMENT_MODE -> {
                val factory = InjectorUtils.provideSearchEquipmentViewModel()
                viewModel = ViewModelProviders.of(this, factory).get(SearchEquipmentViewModel::class.java).apply {
                    categories.observe(this@SearchActivity, Observer {
                        // Do nothing, this invokes the loading of categories
                    })
                }

            }
            DEALERS_LOCATOR_MODE -> {
                val factory = InjectorUtils.provideSearchDealerViewModel()
                viewModel = ViewModelProviders.of(this, factory).get(SearchDealersViewModel::class.java)
            }
            else -> throw IllegalStateException("No valid search searchMode provided!")
        }
    }

    private fun onHintAdapterFor(query: String) {
        viewModel.search(activity = this, recyclerView = hintRecyclerView, query = query)
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
            when (searchMode) {
                DEALERS_LOCATOR_MODE -> query(query)
            }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            when (searchMode) {
                ADD_EQUIPMENT_MODE -> query(newText)
            }
            return true
        }
    }

    abstract class HintAdapter<VH: RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
        private var onItemSelected: ((query: String) -> Unit)? = null

        @CallSuper
        override fun onBindViewHolder(viewHolder: VH, position: Int) {
            viewHolder.itemView.setOnClickListener {
                val query = getSearchQueryAt(viewHolder.adapterPosition)
                onItemSelected?.invoke(query ?: "")
            }
            onBind(viewHolder, position)
        }

        abstract fun onBind(viewHolder: VH, position: Int)
        abstract fun getSearchQueryAt(position: Int): String?
    }
}

//
// Search - Equipment Selectable Results
//
class SelectableEquipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageView: ImageView = itemView.findViewById(R.id.imageView)
    private val textView: TextView = itemView.findViewById(R.id.equipmentTextView)

    var model: EquipmentUIModel? = null
        set(value) {
            value?.let { equipment ->
                textView.text = equipment.name
                imageView.setImageResource(equipment.imageResId)
            }
            field = value
        }

    var onItemSelected: (() -> Unit)? = null
        set(value) {
            field = value
            itemView.setOnClickListener { value?.invoke() }
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

class EquipmentSearchHintListAdapter(
    private val data: List<EquipmentUIModel>,
    private val onSelect: ((equipment: EquipmentUIModel) -> Unit)
) : SearchActivity.HintAdapter<SelectableEquipmentViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SelectableEquipmentViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.search_equipment_item_view, viewGroup, false)
        return SelectableEquipmentViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBind(viewHolder: SelectableEquipmentViewHolder, position: Int) {
        viewHolder.model = data[position]
        viewHolder.onItemSelected = { onSelect.invoke(data[viewHolder.adapterPosition]) }
    }

    override fun getSearchQueryAt(position: Int): String? = data[position].name
}

class DealersSearchHintListAdapter(private val data: List<AutocompletePrediction>, private val onSelect: ((city: AutocompletePrediction) -> Unit))
    : SearchActivity.HintAdapter<SelectableDealerViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SelectableDealerViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_dealer_search_hint, viewGroup, false)
        return SelectableDealerViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBind(viewHolder: SelectableDealerViewHolder, position: Int) {
        viewHolder.prediction = data[position]
        viewHolder.onItemSelected = { onSelect.invoke(data[viewHolder.adapterPosition]) }
    }

    override fun getSearchQueryAt(position: Int): String? = data[position].toString()
}