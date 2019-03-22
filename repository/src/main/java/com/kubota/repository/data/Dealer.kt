package com.kubota.repository.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "dealers",
    foreignKeys = arrayOf(
        ForeignKey(entity = Account::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("userId"),
            onDelete = ForeignKey.CASCADE)
    ))
data class Dealer(
    @PrimaryKey @ColumnInfo(name = "id")
    val id: String,
    val name: String,
    val streetAddress: String,
    val city: String,
    val stateCode: String,
    val postalCode: String,
    val countryCode: String,
    val phone: String,
    val webAddress: String,
    val lastModified : String,
    val publicationDate : String,
    val dateCreated : String,
    val serviceCertified : Boolean,
    val tier2Participant : Boolean,
    val rsmNumber : String,
    val productCodes : String,
    val dealerDivision : String,
    val dealerCertificationLevel : String,
    val rsmEmail : String,
    val districtNumber : String,
    val fax : String,
    val rsmName : String,
    val extendedWarranty : Boolean,
    val salesQuoteEmail : String,
    val latitude: Double,
    val longitude: Double,
    var isDirty: Boolean = true,
    val userId: Int)