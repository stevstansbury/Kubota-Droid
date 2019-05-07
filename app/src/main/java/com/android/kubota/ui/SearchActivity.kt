package com.android.kubota.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.ui.ChooseEquipmentFragment.Companion.KEY_SEARCH_RESULT
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ChooseEquipmentViewModel
import com.android.kubota.viewmodel.EquipmentUIModel
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
    private lateinit var viewModel: ViewModel
    private lateinit var searchView: SearchView
    private lateinit var backIcon: ImageView
    private lateinit var hintRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

        extractMode()

        backIcon = findViewById<ImageView>(R.id.backIcon).apply {
            setOnClickListener { onBackPressed() }
        }

        hintRecyclerView = findViewById<RecyclerView>(R.id.kubotaSearchHintRecyclerView).apply {
            layoutManager = LinearLayoutManager(context)
        }

        searchView = findViewById<SearchView>(R.id.searchView).apply {
            isIconified = false
            // Set SearchView underlined view color
            findViewById<View>(android.support.v7.appcompat.R.id.search_plate).apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            }
            setOnCloseListener {
                onBackPressed()
                true
            }
            setOnQueryTextListener(queryListener)
        }
    }

    private fun extractMode() {
        searchMode = intent.getIntExtra(KEY_MODE, UNKNOWN_MODE)

        when (searchMode) {
            ADD_EQUIPMENT_MODE -> {
                val factory = InjectorUtils.provideChooseEquipmentViewModel()
                viewModel = ViewModelProviders.of(this, factory).get(ChooseEquipmentViewModel::class.java).apply {
                    categories.observe(this@SearchActivity, Observer {
                        // Do nothing, this invokes the loading of categories
                    })
                }

            }
            DEALERS_LOCATOR_MODE -> TODO("Missing DEALERS_LOCATOR_MODE implementation.")
            else -> throw IllegalStateException("No valid search searchMode provided!")
        }
    }

    private fun onHintAdapterFor(query: String): HintAdapter<*> {
        return when (searchMode) {
            ADD_EQUIPMENT_MODE -> {
                val models = (viewModel as ChooseEquipmentViewModel).search(query = query)
                EquipmentSearchHintListAdapter(models) {
                    val intent = Intent()
                    intent.putExtra(KEY_SEARCH_RESULT, it)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
            DEALERS_LOCATOR_MODE -> TODO("Missing DEALERS_LOCATOR_MODE implementation.")
            else -> throw IllegalStateException("No valid search searchMode provided!")
        }
    }

    private val queryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            query?.let { searchView.clearFocus() }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            newText?.let {
                if (newText.length >= RENDER_HINTS_AFTER_LENGTH) {
                    val adapter = onHintAdapterFor(newText).apply {
                        onItemSelected = { searchView.setQuery(it, true) }
                    }
                    hintRecyclerView.adapter = adapter
                } else if (newText.isEmpty()) {
                    hintRecyclerView.adapter = null
                }
            }
            return true
        }
    }

    abstract class HintAdapter<VH: RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
        var onItemSelected: ((query: String) -> Unit)? = null

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
private class SelectableEquipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

private class EquipmentSearchHintListAdapter(
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
