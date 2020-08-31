package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.flow.FlowCoordinatorActivity
import com.android.kubota.ui.AuthBaseFragment
import com.android.kubota.utility.PermissionRequestManager
import com.android.kubota.utility.Utils
import com.android.kubota.utility.showMessage
import com.android.kubota.viewmodel.dealers.DealerViewModel
import com.google.android.gms.maps.model.LatLng
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.Dealer

abstract class DealerViewListener(val fragment: AuthBaseFragment, val viewModel: DealerViewModel): DealerView.OnClickListener {

    override fun onStarClicked(dealer: Dealer) {
        when {
            viewModel.isFavorited(dealer) -> {
                viewModel.removeFromFavorite(fragment.authDelegate, dealer)
            }
            viewModel.canAddToFavorite.value ?: false -> {
                viewModel.addToFavorite(fragment.authDelegate, dealer)
            }
            else -> {
                val flowCoordinator = fragment.requireActivity() as? FlowCoordinatorActivity
                flowCoordinator?.let {
                    it.startFavoriteDealerOnboardUserFlow().done { authenticated ->
                        if (authenticated) {
                            viewModel.addToFavorite(fragment.authDelegate, dealer)
                        }
                    }
                }
                ?:
                {
                    val dialog = Utils.createMustLogInDialog(
                        fragment.requireContext(),
                        Utils.LogInDialogMode.DEALER_MESSAGE
                    )
                    dialog.setOnCancelListener { dialog.dismiss() }
                    dialog.show()
                }()
            }
        }
    }

    override fun onWebClicked(url: String) {
        fragment.showMessage(titleId=R.string.leave_app_dialog_title, messageId=R.string.leave_app_dealer_website_msg)
            .map { idx ->
                if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fragment.startActivity(intent)
            }
    }

    @SuppressLint("MissingPermission")
    override fun onCallClicked(number: String) {
        fragment
            .requestPermission(Manifest.permission.CALL_PHONE, R.string.accept_phone_permission)
            .done {
                fragment.showMessage(title = fragment.requireActivity().getString(R.string.outgoing_call_title), message = fragment.requireActivity().getString(R.string.outgoing_call_msg, number))
                    .map {idx ->
                        if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${number}"))
                        fragment.requireActivity().startActivity(intent)
                    }
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
