//
//  JSONAdapter.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

interface JSONAdapter

class CodableTypes {
    companion object {
        fun newParameterizedType(rawType: Type, typeArgument: Type): ParameterizedType {
            return Types.newParameterizedType(rawType, typeArgument)
        }
    }
}
