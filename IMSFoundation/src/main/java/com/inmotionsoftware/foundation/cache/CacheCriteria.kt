//
//  CacheCriteria.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.cache

enum class CacheAge(val interval: Long){
    oneMinute (interval = 60)
    , oneHour (interval = 60 * 60)
    , oneDay  ( interval = 60 * 60 * 24)
    , immortal (interval = Long.MAX_VALUE)
}

enum class CachePolicy {
    useAge
    , useAgeReturnCacheIfError
    , returnCacheElseLoad
    , reloadReturnCacheIfError
    , reloadReturnCacheWithAgeCheckIfError
}

data class CacheCriteria(val policy: CachePolicy, val age: Long, val cacheKey: String? = null)
