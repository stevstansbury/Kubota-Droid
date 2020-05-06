//
//  CacheStore.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.cache

import com.inmotionsoftware.promisekt.Promise

interface CacheStore {

    fun get(key: String) : Promise<ByteArray?>

    fun get(key: String, maxAge: Long): Promise<ByteArray?>

    fun put(key: String, value: ByteArray): Promise<Unit>

    fun put(key: String, value: ByteArray, flush: Boolean): Promise<Unit>

    fun remove(key: String): Promise<Unit>

}
