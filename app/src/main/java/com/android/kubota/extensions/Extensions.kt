package com.android.kubota.extensions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.Base64
import com.android.kubota.R
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.ui.FlowActivity
import com.android.kubota.utility.Utils
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


//
// PublicClientApplication extension methods
//
fun PublicClientApplication.login(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, null as IAccount?, UiBehavior.FORCE_LOGIN, null, callback)
}

fun PublicClientApplication.createAccount(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, "", UiBehavior.SELECT_ACCOUNT, null,
        emptyArray<String>(), PCASetting.SignUp().authority, callback)
}

fun PublicClientApplication.forgotPassword(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, null as IAccount?, UiBehavior.SELECT_ACCOUNT, null,
        null, PCASetting.ResetPassword().authority, callback)
}

fun PublicClientApplication.changePassword(activity: Activity, iAccount: IAccount, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, iAccount, UiBehavior.SELECT_ACCOUNT, null,
        null, PCASetting.ResetPassword().authority, callback)
}

private fun String?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

//
// Model classes' related extension methods
//
fun Model.toUIModel(): UIModel {
    return when (category) {
        "Construction" -> UIModel(id, model, serialNumber, R.string.equipment_construction_category, Utils.getModelImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide)
        "Mowers" -> UIModel(id, model, serialNumber, R.string.equipment_mowers_category, Utils.getModelImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide)
        "Tractors" -> UIModel(id, model, serialNumber, R.string.equipment_tractors_category, Utils.getModelImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide)
        "Utility Vehicles" -> UIModel(id, model, serialNumber, R.string.equipment_utv_category, Utils.getModelImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide)
        else -> UIModel(id, model, serialNumber, 0, 0, !manualLocation.isNullOrEmpty(), hasGuide)
    }

}

fun Dealer.toUIDealer(): UIDealer {
    return UIDealer(id = id, name = name, address = streetAddress, city = city, state = stateCode, postalCode = postalCode, phone = phone, website = webAddress, dealerNumber = number)
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

//
// Context extension methods
//
fun Context.isLocationEnabled(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
