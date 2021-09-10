package com.android.kubota.ui.flow.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.kubota.R
import com.android.kubota.databinding.FragmentAddEquipmentTypeBinding
import com.inmotionsoftware.flowkit.android.FlowFragment
import com.kubota.service.domain.EquipmentModel

class AddEquipmentTypeFragment : FlowFragment<Unit, EquipmentModel.Type>() {

    private lateinit var binding: FragmentAddEquipmentTypeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.activity?.setTitle(R.string.add_equipment)
        binding = FragmentAddEquipmentTypeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMachine.setOnClickListener { resolve(EquipmentModel.Type.Machine) }
        binding.btnAttachment.setOnClickListener { resolve(EquipmentModel.Type.Attachment) }
    }

    override fun onInputAttached(input: Unit) = Unit
}