package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.utility.PermissionRequestManager
import com.android.kubota.utility.Utils
import com.android.kubota.utility.showMessage
import com.android.kubota.viewmodel.dealers.DealerViewModel
import com.google.android.gms.maps.model.LatLng
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.Dealer

class DealerViewListener(val fragment: Fragment, val viewModel: DealerViewModel): DealerView.OnClickListener {

    override fun onStarClicked(dealer: Dealer) {
        when {
            viewModel.isFavorited(dealer) -> {
                viewModel.removeFromFavorite(dealer)
            }
            viewModel.canAddToFavorite.value ?: false -> {
                viewModel.addToFavorite(dealer)
            }
            else -> {
                val dialog = Utils.createMustLogInDialog(
                    fragment.requireContext(),
                    Utils.LogInDialogMode.DEALER_MESSAGE
                )
                dialog.setOnCancelListener { dialog.dismiss() }
                dialog.show()
            }
        }
    }

    override fun onWebClicked(url: String) {
        val addr = if (!url.startsWith("http", ignoreCase = true)) {
            "https://www.kubotausa.com/dealers/${url}"
        } else {
            url
        }

        fragment.showMessage(titleId=R.string.leave_app_dialog_title, messageId=R.string.leave_app_dealer_website_msg)
            .map { idx ->
                if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(addr))
                fragment.startActivity(intent)
            }
    }

    @SuppressLint("MissingPermission")
    override fun onCallClicked(number: String) {
        PermissionRequestManager
            .requestPermission(fragment.requireActivity(), Manifest.permission.CALL_PHONE, R.string.accept_phone_permission)
            .done {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${number}"))
                fragment.requireActivity().startActivity(intent)
            }
    }

    override fun onDirClicked(addr: String) {
        val uri: Uri = Uri.parse("google.navigation:q=$addr")
        handleDirections(uri)
    }

    override fun onDirClicked(loc: LatLng) {
        val uri: Uri = Uri.parse("google.navigation:q=${loc.latitude},${loc.longitude}")
        handleDirections(uri)
    }

    private fun handleDirections(uri: Uri) {
        fragment.showMessage(titleId = R.string.leave_app_dialog_title, messageId=R.string.leave_app_dealer_directions_msg)
            .map { idx ->
                if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                fragment.startActivity(intent)
            }
    }
}
