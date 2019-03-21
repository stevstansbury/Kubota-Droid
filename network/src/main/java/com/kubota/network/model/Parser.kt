package com.kubota.network.model

import com.google.gson.internal.LinkedTreeMap

interface Parser<T> {
    companion object {
        fun getUserPreferencesParser(linkedTreeMap: LinkedTreeMap<String, *>) = UserPreferenceParser(linkedTreeMap) as Parser<UserPreference>
    }

    fun parse(): T
}

internal class UserPreferenceParser(private val linkedTreeMap: LinkedTreeMap<String, *>): Parser<UserPreference> {

    override fun parse(): UserPreference {
        val userPreference = UserPreference()
        userPreference.userId = linkedTreeMap.get("userId") as String

        for (key in linkedTreeMap.keys) {
            when(key) {
                "userId" -> userPreference.userId = linkedTreeMap[key] as String
                "models" -> {
                    val list = linkedTreeMap[key] as List<LinkedTreeMap<String, String?>>
                    userPreference.models = parseModels(list)
                }
                "dealers" -> {
                    val list = linkedTreeMap[key] as List<LinkedTreeMap<String, *>>
                    userPreference.dealers = parseDealers(list)
                }
            }

        }
        return userPreference
    }

    private fun parseModels(list: List<LinkedTreeMap<String, String?>>): List<Model> {

        if (list.isNotEmpty()) {
            val results = ArrayList<Model>(list.size)

            for (tree in list) {
                results.add(ModelParser(tree).parse())
            }

            return results
        }

        return emptyList()
    }

    private fun parseDealers(list: List<LinkedTreeMap<String, *>>): List<Dealer> {

        if (list.isNotEmpty()) {
            val results = ArrayList<Dealer>(list.size)

            for (tree in list) {
                results.add(DealerParser(tree).parse())
            }

            return results
        }

        return emptyList()
    }

}

internal class ModelParser(private val linkedTreeMap: LinkedTreeMap<String, String?>): Parser<Model> {

    override fun parse(): Model {
        val model = Model()
        model.id = linkedTreeMap.get("id") as String
        model.manualName = linkedTreeMap.get("manualName") as String
        model.model = linkedTreeMap.get("model") as String
        model.serialNumber = linkedTreeMap.get("serialNumber")
        model.modelCategory = linkedTreeMap.get("category")

        return model
    }

}

internal class DealerParser(private val linkedTreeMap: LinkedTreeMap<String, *>): Parser<Dealer> {

    override fun parse(): Dealer {
        val value = Dealer()
        value.id = linkedTreeMap.get("id") as String
        value.lastModified = linkedTreeMap.get("lastModified") as String
        value.publicationDate = linkedTreeMap.get("publicationDate") as String
        value.dateCreated = linkedTreeMap.get("dateCreated") as String
        value.urlName = linkedTreeMap.get("urlName") as String
        value.serviceCertified = linkedTreeMap.get("serviceCertified") as Boolean
        value.tier2Participant = linkedTreeMap.get("tier2Participant") as Boolean
        value.rsmNumber = linkedTreeMap.get("rsmNumber") as String
        value.productCodes = linkedTreeMap.get("productCodes") as String
        value.dealerDivision = linkedTreeMap.get("dealerDivision") as String
        value.address = AddressParser(linkedTreeMap.get("address") as LinkedTreeMap<String, *>).parse()
        value.phone = linkedTreeMap.get("phone") as String
        value.dealerName = linkedTreeMap.get("dealerName") as String
        value.dealerCertificationLevel = linkedTreeMap.get("dealerCertificationLevel") as String
        value.dealerEmail = linkedTreeMap.get("dealerEmail") as String
        value.rsmEmail = linkedTreeMap.get("rsmEmail") as String
        value.districtNumber = linkedTreeMap.get("districtNumber") as String
        value.fax = linkedTreeMap.get("fax") as String
        value.rsmName = linkedTreeMap.get("rsmName") as String
        value.extendedWarranty = linkedTreeMap.get("extendedWarranty") as Boolean
        value.salesQuoteEmail = linkedTreeMap.get("salesQuoteEmail") as String
        value.dealerNumber = linkedTreeMap.get("dealerNumber") as String
        value.distance = linkedTreeMap.get("distance") as String

        return value
    }
}

internal class AddressParser(private val linkedTreeMap: LinkedTreeMap<String, *>): Parser<Address> {
    override fun parse(): Address {
        val value = Address()
        value.id = linkedTreeMap.get("id") as String
        value.countryCode = linkedTreeMap.get("countryCode") as String
        value.stateCode = linkedTreeMap.get("stateCode") as String
        value.city = linkedTreeMap.get("city") as String
        value.zip = linkedTreeMap.get("zip") as String
        value.street = linkedTreeMap.get("street") as String
        value.latitude = linkedTreeMap.get("latitude") as Double
        value.longitude = linkedTreeMap.get("longitude") as Double

        return value
    }

}