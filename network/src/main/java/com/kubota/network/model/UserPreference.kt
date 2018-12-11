package com.kubota.network.model

data class UserPreference(val UserId: String, val Models: List<Model>, val Dealers: List<Dealer>)

data class Model(val Id: String, val ManualName: String, val Model: String, val SerialNumber: String)