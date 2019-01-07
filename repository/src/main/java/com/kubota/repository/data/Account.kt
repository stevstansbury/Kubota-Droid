package com.kubota.repository.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "account")
data class Account internal constructor(
    @PrimaryKey @ColumnInfo(name = "id")
    val id: Int = 1,
    val userName: String,
    var accessToken: String,
    var expireDate: Long,
    var flags: Int = FLAGS_INCOMPLETE) {


    fun isGuest() = id == GUEST_ACCOUNT_ID

    companion object {
        val FLAGS_NORMAL = 1
        val FLAGS_INCOMPLETE = 1 shl 2
        val FLAGS_TOKEN_EXPIRED = 1 shl 3
        val FLAGS_SYNCING = 1 shl 4

        val GUEST_ACCOUNT_ID = -1

        fun createAccount(userName: String, accessToken: String, expireDate: Long): Account {
            return Account(userName = userName, accessToken = accessToken, expireDate = expireDate)
        }

        fun createGuestAccount(): Account {
            return Account(id = GUEST_ACCOUNT_ID, userName = "guest", accessToken = "", expireDate = -1L)
        }
    }
}