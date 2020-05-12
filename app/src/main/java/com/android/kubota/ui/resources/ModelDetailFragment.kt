package com.android.kubota.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.kubota.databinding.FragmentModelDetailBinding
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.resources.ModelDetailViewModel
import com.kubota.repository.uimodel.KubotaModel
import kotlinx.coroutines.launch

class ModelDetailFragment : Fragment() {

    private var binding: FragmentModelDetailBinding? = null

    private lateinit var viewModel: ModelDetailViewModel
    private lateinit var model: KubotaModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils
            .provideModelDetailViewModel(requireContext())
        viewModel = ViewModelProvider(this, factory)
            .get(ModelDetailViewModel::class.java)

        arguments
            ?.getParcelable<KubotaModel>(MODEL_KEY)
            ?.let { model = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentModelDetailBinding
            .inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                viewModel.saveRecentlyViewed(model)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    private fun setupUI() {
        activity?.title = model.modelName

        binding?.guidesButton?.visibility =
            if (model.guidesUrl.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        private const val MODEL_KEY = "model"

        fun createInstance(model: KubotaModel): ModelDetailFragment {
            val args = Bundle(1).apply {
                putParcelable(MODEL_KEY, model)
            }
            return ModelDetailFragment().apply {
                arguments = args
            }
        }
    }
}