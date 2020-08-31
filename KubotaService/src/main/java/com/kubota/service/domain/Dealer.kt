//
//  Dealer.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain


data class Dealer (
    val dealerNumber: Int,
    val name: String,
    val email: String,
    val website: String,
    val phone: String,
    val address: Address,
    val distanceMeters: Double?
)
