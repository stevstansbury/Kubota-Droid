package com.android.kubota.extensions

import android.app.Activity
import android.support.design.widget.Snackbar
import android.util.Base64
import com.android.kubota.R
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.ui.FlowActivity
import com.android.kubota.viewmodel.UIDealer
import com.android.kubota.viewmodel.UIModel
import com.kubota.repository.data.Dealer
import com.kubota.repository.data.Model
import com.kubota.repository.service.SearchDealer as ServiceDealer
import com.kubota.repository.user.PCASetting
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.UiBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


//
// PublicClientApplication extension methods
//
fun PublicClientApplication.login(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, accounts.getUserByPolicy(PCASetting.SignIn().policy),
        UiBehavior.FORCE_LOGIN, null, callback)
}

fun PublicClientApplication.createAccount(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, "", UiBehavior.SELECT_ACCOUNT, null,
        emptyArray<String>(), PCASetting.SignUp().authority, callback)
}

fun PublicClientApplication.forgotPassword(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, null as IAccount?, UiBehavior.SELECT_ACCOUNT, null,
        null, PCASetting.ResetPassword().authority, callback)
}

fun PublicClientApplication.changePassword(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, accounts.getUserByPolicy(PCASetting.ResetPassword().policy),
        UiBehavior.SELECT_ACCOUNT, null, null,
        PCASetting.ResetPassword().authority, callback)
}

private fun List<IAccount>.getUserByPolicy(policy: String): IAccount? {
    for (user in this) {
        val userIdentifier = user.accountIdentifier.identifier.split("\\.")[0].base64UrlDecode()
        if (userIdentifier.contains(policy.toLowerCase())) {
            return user
        }
    }

    return null
}

private fun String.base64UrlDecode(): String {
    val data = Base64.decode(this, Base64.DEFAULT or Base64.URL_SAFE)
    return String(data, Charsets.UTF_8)
}

private fun String?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

fun Model.toUIModel(): UIModel {
    return when (category) {
        "Construction" -> UIModel(id, model, serialNumber, R.string.equipment_construction_category,
            R.drawable.ic_construction_category_thumbnail, !manualLocation.isNullOrEmpty(), hasGuide)
        "Mowers" -> UIModel(id, model, serialNumber, R.string.equipment_mowers_category, R.drawable.ic_mower_category_thumbnail,
            !manualLocation.isNullOrEmpty(), hasGuide)
        "Tractors" -> UIModel(id, model, serialNumber, R.string.equipment_tractors_category, R.drawable.ic_tractor_category_thumbnail,
            !manualLocation.isNullOrEmpty(), hasGuide)
        "Utility Vehicles" -> UIModel(id, model, serialNumber, R.string.equipment_utv_category, R.drawable.ic_utv_category_thumbnail,
            !manualLocation.isNullOrEmpty(), hasGuide)
        else -> UIModel(id, model, serialNumber, 0, 0, !manualLocation.isNullOrEmpty(), hasGuide)
    }

}

fun Dealer.toUIDealer(): UIDealer {
    return UIDealer(id = id, name = name, address = streetAddress, city = city, state = stateCode, postalCode = postalCode, phone = phone, website = webAddress, dealerNumber = number)
}

//
// CoroutineScope extension methods
//

fun CoroutineScope.backgroundTask(block: suspend () -> Unit): Job {
    return this.launch {
        block()
    }
}

fun ServiceDealer.toDealer(isFavorited: Boolean): SearchDealer {
    return SearchDealer(
        serverId = serverId, name = name, streetAddress = streetAddress, city = city,
        stateCode = stateCode, postalCode = postalCode, countryCode = countryCode, phone = phone,
        webAddress = webAddress, dealerNumber = dealerNumber, latitude = latitude, longitude = longitude,
        distance = distance, isFavorited = isFavorited
    )
}

//
// FlowActivity extension methods
//
fun FlowActivity.showServerErrorSnackBar() {
    makeSnackbar()?.apply {
        setText(this.context.getString(R.string.server_error_message))
        duration = Snackbar.LENGTH_INDEFINITE
        setAction(this.context.getString(R.string.dismiss)) {}
        show()
    }
}
