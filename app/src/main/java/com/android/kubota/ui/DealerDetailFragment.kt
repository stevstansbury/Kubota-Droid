package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    private var isFavoritedDealer = false

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

        view.findViewById<ImageView>(R.id.back).setOnClickListener { activity?.onBackPressed() }
        val favoriteButton = view.findViewById<ImageView>(R.id.star)
        favoriteButton.setOnClickListener {
            dealer?.let {
                if (isFavoritedDealer) {
                    favoriteButton.setImageResource(R.drawable.ic_star_filled)
                    viewModel.deleteFavoriteDealer(it)
                } else {
                    favoriteButton.setImageResource(R.drawable.ic_star_unfilled)
                    viewModel.insertFavorite(it)
                }

                isFavoritedDealer = !isFavoritedDealer
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
            val mapUri = Uri.parse("geo:0,0?q=${dealer?.name}, ${dealer?.address}, ${dealer?.city}, ${dealer?.state}")
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, mapUri))
        }

        view.findViewById<LinearLayout>(R.id.phoneNumberRow).setOnClickListener {
            requireContext().startActivity(Intent(Intent.ACTION_DEFAULT, Uri.parse("tel:" + dealer?.phone)))
        }

        view.findViewById<LinearLayout>(R.id.websiteRow).setOnClickListener {
            leaveAppDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.leave_app_dialog_title)
                .setMessage(R.string.leave_app_dealer_website_msg)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.kubotausa.com/dealers/${dealer?.website}"))
                    requireContext().startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
                .setOnCancelListener {
                    leaveAppDialog = null
                }
                .show()
        }

        view.findViewById<TextView>(R.id.name).text = dealer?.name

        view.findViewById<TextView>(R.id.address).text = getString(R.string.dealer_address_fmt, dealer?.address, dealer?.city, dealer?.state, dealer?.postalCode)

        view.findViewById<TextView>(R.id.phoneNumber).text = dealer?.phone

        return view
    }

    override fun onPause() {
        super.onPause()

        leaveAppDialog?.let {
            it.dismiss()
            leaveAppDialog = null
        }
    }
}