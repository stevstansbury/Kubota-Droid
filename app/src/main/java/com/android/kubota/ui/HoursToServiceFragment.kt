package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.HoursToServiceViewModel
import com.kubota.repository.service.ServiceInterval
import com.kubota.repository.service.ServiceResponse
import java.lang.StringBuilder

private const val EQUIPMENT_KEY = "equipment"

class HoursToServiceFragment: BaseFragment() {

    companion object {
        fun createInstance(equipmentId: Int): HoursToServiceFragment {
            val data = Bundle(1)
            data.putInt(EQUIPMENT_KEY, equipmentId)

            return HoursToServiceFragment().apply {
                arguments = data
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_hours_to_service

    private lateinit var viewModel: HoursToServiceViewModel

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHours: TextView
    private lateinit var recyclerView: RecyclerView

    override fun hasRequiredArgumentData(): Boolean {
        val equipmentId = arguments?.getInt(EQUIPMENT_KEY) ?: 0
        val factory = InjectorUtils.provideHoursToService(requireContext(), equipmentId)
        viewModel = ViewModelProvider(this, factory)
            .get(HoursToServiceViewModel::class.java)

        return equipmentId > 0
    }

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
        viewModel.equipmentEngineHours.observe(this, Observer {
            engineHours.text = "$it"
        })

        viewModel.equipmentImage.observe(this, Observer {
            if (it != 0) modelImageView.setImageResource(it)
        })

        viewModel.equipmentSerial.observe(this, Observer {
            serialNumberTextView.text = if (it != null) {
                getString(R.string.equipment_serial_number_fmt, it)
            } else {
                getString(R.string.equipment_serial_number)
            }
        })

        viewModel.equipmentModel.observe(this, Observer {
            modelTextView.text = it
        })

        viewModel.equipmentNickname.observe(this, Observer {
            equipmentNicknameTextView.text =
                if (it.isNullOrBlank())
                    getString(R.string.no_equipment_name_fmt, viewModel.equipmentModel.value)
                else
                    it
        })

        viewModel.equipmentServiceLiveData.observe(this, Observer {
            when (it) {
                is ServiceResponse.Success -> {
                    recyclerView.adapter = Adapter(it.maintenanceService)
                }
                is ServiceResponse.IOError -> {
                    flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
                }
                is ServiceResponse.GenericError -> {
                    flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
                }
            }
        })
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