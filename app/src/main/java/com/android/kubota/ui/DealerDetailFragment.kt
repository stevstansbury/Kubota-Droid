package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.utility.Utils
import com.android.kubota.viewmodel.DealerDetailViewModel
import com.android.kubota.viewmodel.UIDealer
import com.android.kubota.viewmodel.UIDealerDetailModel

private const val KEY_DEALER = "dealer"
class DealerDetailFragment: BaseFragment() {

    companion object {
        private fun createInstance(dealer: UIDealerDetailModel): DealerDetailFragment {
            val fragment = DealerDetailFragment()
            val args = Bundle(1)
            args.putParcelable(KEY_DEALER, dealer)
            fragment.arguments = args

            return fragment
        }

        fun createIntance(dealer: UIDealer): DealerDetailFragment {
            val dealerDetail = UIDealerDetailModel(dealerNumber = dealer.dealerNumber, name = dealer.name, address = dealer.address,
                city = dealer.city, state = dealer.state, postalCode = dealer.postalCode, phone = dealer.phone, website = dealer.website)

            return createInstance(dealer = dealerDetail)
        }
    }

    private lateinit var viewModel: DealerDetailViewModel
    private var dealer: UIDealerDetailModel? = null
    private var leaveAppDialog: AlertDialog? = null
    private var signInDialog: AlertDialog? = null
    private var isFavoritedDealer = false
    private var canAddDealer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils.provideDealerDetailsViewModelFactory(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(DealerDetailViewModel::class.java)

        dealer = arguments?.getParcelable(KEY_DEALER)
        if (dealer == null) {
            activity?.onBackPressed()
        }

        if (savedInstanceState == null) {
            activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.dealer_detail_status_bar_color)
            flowActivity?.hideProgressBar()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.fragment_dealer_detail, null)

        viewModel.canAddDealer.observe(this, Observer {
            this.canAddDealer = it ?: false
        })

        view.findViewById<ImageView>(R.id.back).setOnClickListener { activity?.onBackPressed() }
        val favoriteButton = view.findViewById<ImageView>(R.id.star)
        favoriteButton.setOnClickListener {
            dealer?.let {
                if (isFavoritedDealer) {
                    favoriteButton.setImageResource(R.drawable.ic_star_filled)
                    viewModel.deleteFavoriteDealer(it)
                    isFavoritedDealer = !isFavoritedDealer
                } else if (canAddDealer) {
                    favoriteButton.setImageResource(R.drawable.ic_star_unfilled)
                    viewModel.insertFavorite(it)
                    isFavoritedDealer = !isFavoritedDealer
                } else {
                    resetDialogs()

                    signInDialog = Utils.createMustLogInDialog(requireContext(), Utils.LogInDialogMode.DEALER_MESSAGE)
                    signInDialog?.setOnCancelListener { resetDialogs() }
                    signInDialog?.show()
                }
            }
        }

        viewModel.isFavoritedDealer(dealerNumber = dealer?.dealerNumber!!).observe(this, Observer {
            isFavoritedDealer = it ?: false
            if (it == true) {
                favoriteButton.setImageResource(R.drawable.ic_star_filled)
            } else {
                favoriteButton.setImageResource(R.drawable.ic_star_unfilled)
            }
        })

        view.findViewById<LinearLayout>(R.id.addressRow).setOnClickListener {
            val mapUri = Uri.parse("geo:0,0?q=" + Uri.encode("${dealer?.name}, ${dealer?.address}, ${dealer?.city}, ${dealer?.state}"))
            showLeavingAppDialog(R.string.leave_app_dealer_address_msg, Intent(Intent.ACTION_VIEW, mapUri))
        }

        view.findViewById<LinearLayout>(R.id.phoneNumberRow).setOnClickListener {
            requireContext().startActivity(Intent(Intent.ACTION_DEFAULT, Uri.parse("tel:" + dealer?.phone)))
        }

        view.findViewById<LinearLayout>(R.id.websiteRow).setOnClickListener {
            showLeavingAppDialog(R.string.leave_app_dealer_website_msg,
                Intent(Intent.ACTION_VIEW, Uri.parse("http://www.kubotausa.com/dealers/${dealer?.website}")))
        }

        view.findViewById<TextView>(R.id.name).text = dealer?.name

        view.findViewById<TextView>(R.id.address).text = getString(R.string.dealer_address_fmt, dealer?.address, dealer?.city, dealer?.state, dealer?.postalCode)

        view.findViewById<TextView>(R.id.phoneNumber).text = dealer?.phone

        return view
    }

    override fun onPause() {
        super.onPause()

        resetDialogs()
    }

    private fun resetDialogs() {
        signInDialog?.dismiss()
        signInDialog = null

        leaveAppDialog?.dismiss()
        leaveAppDialog = null
    }

    private fun showLeavingAppDialog(@StringRes messageResId: Int, intent: Intent) {
        leaveAppDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_app_dialog_title)
            .setMessage(messageResId)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requireContext().startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                leaveAppDialog = null
            }
            .show()
    }
}