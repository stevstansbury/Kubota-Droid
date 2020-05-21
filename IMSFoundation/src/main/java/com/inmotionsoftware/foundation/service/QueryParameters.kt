//
//  QueryParameters.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

fun queryParams(vararg pairs: Pair<String, String>): QueryParameters {
    return pairs.fold(QueryParameters()) { qp, pair ->
        qp.addQueryParameter(pair.first, pair.second)
        qp
    }
}

fun queryParamsMultiValue(vararg pairs: Pair<String, List<String>>): QueryParameters {
    return pairs.fold(QueryParameters()) { qp, pair ->
        qp.addQueryParameters(pair.first, pair.second)
        qp
    }
}

class QueryParameters {

    private var items: MutableMap<String, List<String>> = mutableMapOf()
    val fields: Map<String, List<String>> get() { return this.items.toMap() }

    fun addQueryParameter(name: String, value: String) {
        this.items[name] = this.items[name]?.let { it.toMutableList().apply { add(value) } } ?: listOf(value)
    }

    fun addQueryParameters(name: String, values: List<String>) {
        this.items[name] = this.items[name]?.let { it.toMutableList().apply { addAll(values) } } ?: values
    }

}
