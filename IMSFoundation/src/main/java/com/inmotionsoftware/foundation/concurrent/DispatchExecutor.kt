//
//  DispatchExecutor.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.concurrent

import android.os.Handler
import android.os.Looper
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

object DispatchExecutor {
    val main: Executor by lazy { Executor { Handler(Looper.getMainLooper()).post(it) } }
    val global: Executor by lazy { Executors.newCachedThreadPool() }
}

/**
 * Asynchronously executes the provided closure on an Executor.
 *
 *  DispatchExecutor.global().async {
 *      // a long runing task
 *  }
 *
 * @param body: The closure to run on the Executor
 */
fun Executor?.async(body: () -> Unit) {
    when (this) {
        null -> body()
        else -> execute(body)
    }
}

fun <T> Executor?.asyncPromise(body: () -> T): Promise<T> {
    return Promise.value(Unit).map(on = this) { body() }
}

fun Executor.asyncAfter(seconds: Double, invoke: () -> Unit) {
    execute {
        Thread.sleep((max(seconds, 0.0) * 1000).toLong())
        invoke()
    }
}

fun Executor?.sync(body: () -> Unit) {
    when (this) {
        null -> body()
        else -> {
            val countDown = CountDownLatch(1)
            execute {
                synchronized(lock = this) { body() }
                countDown.countDown()
            }
            countDown.await()
        }
    }
}
