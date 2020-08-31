//
//  PromiseDispatch.java
//
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.promise.android

import android.os.Handler
import android.os.Looper
import java.util.concurrent.*

enum class PromiseDispatch {

    MAIN, BACKGROUND;

    companion object {
        private val main: Executor by lazy {Executor{ command -> Handler(Looper.getMainLooper()).post(command) }}
        private val background: Executor by lazy {Executors.newCachedThreadPool()}
    }

    val executor: Executor get() {
        return when (this) {
            MAIN -> main
            BACKGROUND -> background
        }
    }
}
