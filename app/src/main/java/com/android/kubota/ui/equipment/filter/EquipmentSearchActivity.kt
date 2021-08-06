package com.android.kubota.ui.equipment.filter

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.kubota.service.api.EquipmentModelTree

class EquipmentSearchActivity : AppCompatActivity() {

    companion object {
        const val KEY_SEARCH_RESULT = "SEARCH_RESULT"

        private const val RENDER_HINTS_AFTER_LENGTH = 2
    }

    private lateinit var searchView: SearchView
    private lateinit var closeButton: View
    private lateinit var hintRecyclerView: RecyclerView

    private val viewModel: EquipmentSearchViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        hintRecyclerView = findViewById<RecyclerView>(R.id.kubotaSearchHintRecyclerView).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        val compatibleWithMachine =
            intent.getStringExtra(EquipmentTreeFilterFragment.EQUIPMENT_MODEL)
                ?.let { listOf(EquipmentTreeFilter.AttachmentsCompatibleWith(it)) }
                ?: emptyList()

        val categoryFilters =
            intent.getStringArrayListExtra(EquipmentTreeFilterFragment.SELECTED_CATEGORIES)
                ?: emptyList()

        val filters = compatibleWithMachine +
            categoryFilters.map { EquipmentTreeFilter.Category(it) }

        viewModel.init(filters)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_activity_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        menu.findItem(R.id.search).let {
            it.expandActionView()
            searchView = it.actionView as SearchView
            searchView.apply {
                maxWidth = Integer.MAX_VALUE
                setSearchableInfo(searchManager.getSearchableInfo(componentName))
                isIconified = false
                queryHint = getString(R.string.search_hint, viewModel.viewData.value?.title)

                findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                    setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                }
                findViewById<TextView>(androidx.appcompat.R.id.search_src_text).apply {
                    setHintTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.dealers_search_hint_text_color
                        )
                    )
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
        return when (item.itemId) {
            android.R.id.home -> {
                hintRecyclerView.hideKeyboard()
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onHintAdapterFor(query: String) {
        val suggestions =
            viewModel.viewData.value?.modelTree
                ?.map { it.getSuggestions(query) }
                ?.flatten()
                ?.sortedBy { it is EquipmentModelTree.Category }

        suggestions?.let {
            hintRecyclerView.adapter =
                EquipmentSuggestionsAdapter(suggestions = it, onSuggestionClicked = {
                    val intent = Intent().apply {
                        when (it) {
                            is EquipmentModelTree.Category -> putExtra(
                                KEY_SEARCH_RESULT,
                                it.category
                            )
                            is EquipmentModelTree.Model -> putExtra(KEY_SEARCH_RESULT, it.model)
                        }
                    }

                    setResult(Activity.RESULT_OK, intent)
                    finish()
                })
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

            query(newText)
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

class EquipmentSuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvSuggestion: TextView = itemView.findViewById(R.id.tv_suggestion)

    fun bind(item: EquipmentModelTree, onItemSelected: (item: EquipmentModelTree) -> Unit) {
        when (item) {
            is EquipmentModelTree.Model -> tvSuggestion.text = item.model.model
            is EquipmentModelTree.Category -> tvSuggestion.text = item.category.category
        }

        itemView.setOnClickListener {
            onItemSelected(item)
        }
    }
}

class EquipmentSuggestionsAdapter(
    private val suggestions: List<EquipmentModelTree>,
    private val onSuggestionClicked: (item: EquipmentModelTree) -> Unit
) : RecyclerView.Adapter<EquipmentSuggestionViewHolder>() {

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): EquipmentSuggestionViewHolder {
        val view = LayoutInflater
            .from(viewGroup.context)
            .inflate(
                R.layout.view_equipment_suggestion,
                viewGroup, false
            )
        return EquipmentSuggestionViewHolder(view)
    }

    override fun getItemCount(): Int = suggestions.size

    override fun onBindViewHolder(viewHolder: EquipmentSuggestionViewHolder, position: Int) {
        viewHolder.bind(suggestions[position]) {
            onSuggestionClicked(it)
        }
    }
}