package com.kubota.repository.data

import android.arch.persistence.room.*

private const val DEFAULT_ID = 0

@Entity(tableName = "dealers",
        foreignKeys = arrayOf(
            ForeignKey(entity = Account::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("userId"),
                onDelete = ForeignKey.CASCADE)
            ),
        indices = arrayOf(Index(value = arrayOf("serverId"), unique = true)))
data class Dealer(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = DEFAULT_ID,
    @ColumnInfo(name = "serverId")
    val serverId: String,
    val userId: Int,
    val name: String,
    val streetAddress: String,
    val city: String,
    val stateCode: String,
    val postalCode: String,
    val countryCode: String,
    val phone: String,
    val webAddress: String,
    val number : String)