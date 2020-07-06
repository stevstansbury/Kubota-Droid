package com.android.kubota.coordinator.flow.util

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.android.kubota.R

class BlockingActivityIndicator: DialogFragment() {

    companion object {
        const val TAG = "BlockingActivityIndicator"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, R.style.AccountSetUpTheme_LoadingDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fragment_loading, null)
    }

    override fun onCancel(dialog: DialogInterface) {
        activity?.onBackPressed()
    }
}