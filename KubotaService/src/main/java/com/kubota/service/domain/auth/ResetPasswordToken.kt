//
//  ResetPasswordToken.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain.auth

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ResetPasswordToken (
    val token: String
): Parcelable
