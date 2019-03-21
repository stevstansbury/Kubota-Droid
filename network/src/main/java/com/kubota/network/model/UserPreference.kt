package com.kubota.network.model

import com.google.gson.annotations.SerializedName

class UserPreference() {
    @SerializedName("userId")
    var userId: String? = null

    @SerializedName("models")
    var models: List<Model> = emptyList()

    @SerializedName("dealers")
    var dealers: List<Dealer> = emptyList()
}

class Model() {
    @SerializedName("id")
    var id: String = ""

    @SerializedName("manualName")
    var manualName: String = ""

    @SerializedName("model")
    var model: String = ""

    @SerializedName("serialNumber")
    var serialNumber: String? = null

    @SerializedName("category")
    var modelCategory: String? = null
}