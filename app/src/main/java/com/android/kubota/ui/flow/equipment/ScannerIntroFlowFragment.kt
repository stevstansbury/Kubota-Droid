package com.android.kubota.ui.flow.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.android.kubota.R
import com.inmotionsoftware.flowkit.android.FlowFragment

class ScannerIntroFlowFragment: FlowFragment<Unit, Unit>() {

    override fun onInputAttached(input: Unit) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_machine_pin, container, false)
        view.findViewById<ImageView>(R.id.btn_dismiss_dialog).setOnClickListener {
            this.cancel()
        }
        return view
    }

}
