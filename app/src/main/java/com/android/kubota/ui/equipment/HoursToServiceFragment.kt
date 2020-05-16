package com.android.kubota.ui.equipment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.kubota.repository.service.ServiceInterval
import com.kubota.service.domain.EquipmentUnit
import java.lang.StringBuilder
import java.util.*
import androidx.lifecycle.Observer

class HoursToServiceFragment: BaseEquipmentUnitFragment() {

    companion object {
        fun createInstance(equipmentId: UUID): HoursToServiceFragment {
            return HoursToServiceFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_hours_to_service

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHours: TextView
    private lateinit var recyclerView: RecyclerView

    override fun initUi(view: View) {
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        engineHours = view.findViewById(R.id.engineHoursText)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    override fun loadData() {
        super.loadData()

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let { this.onBindData(it) }
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        engineHours.text = display.engineHours
        modelImageView.setImageResource(display.imageResId)
        serialNumberTextView.text = display.serialNumber
        modelTextView.text = display.modelName
        equipmentNicknameTextView.text = display.nickname

        // TODO: Handle maintenanceService
        // recyclerView.adapter = Adapter(it.maintenanceService)
    }
}

private class ServiceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val hoursTextView: TextView = itemView.findViewById(R.id.serviceHours)
    private val viewMoreTextView: TextView = itemView.findViewById(R.id.viewMore)
    private val serviceTextView: TextView = itemView.findViewById(R.id.equipmentService)

    fun onBind(service: ServiceInterval) {
        hoursTextView.text = "${service.engineHours}"
        val sb = StringBuilder()

        viewMoreTextView.visibility = if (service.maintenance.size > 3) {
            View.VISIBLE
        } else {
            View.GONE
        }

        for(item in service.maintenance) {
            if (sb.isNotEmpty()) sb.append("\n")

            sb.append(item)
        }

        serviceTextView.text = sb.toString()
    }
}

private class Adapter(private val data: List<ServiceInterval>): RecyclerView.Adapter<ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        return ServiceViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.view_equipment_service, parent, false)
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.onBind(data[position])
    }

}