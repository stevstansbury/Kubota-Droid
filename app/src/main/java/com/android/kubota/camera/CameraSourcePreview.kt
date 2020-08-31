package com.android.kubota.camera

import android.view.SurfaceHolder
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import java.io.IOException


/** Preview the camera image in the screen.  */
class CameraSourcePreview(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {
    private val surfaceView: SurfaceView
    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false
    private var cameraSource: CameraSource? = null

    private var overlay: GraphicOverlay? = null

    init {
        startRequested = false
        surfaceAvailable = false

        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }

    @Throws(IOException::class)
    private fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
        }

        this.cameraSource = cameraSource

        if (this.cameraSource != null) {
            startRequested = true
            startIfReady()
        }
    }

    @Throws(IOException::class)
    fun start(cameraSource: CameraSource, overlay: GraphicOverlay) {
        this.overlay = overlay
        start(cameraSource)
    }

    fun stop() {
        if (cameraSource != null) {
            cameraSource!!.stop()
        }
    }

    fun resume() {
        if (this.cameraSource != null) {
            startRequested = true
            startIfReady()
        }
    }

    fun release() {
        if (cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
        }
        surfaceView.holder.surface.release()
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            if (true) {
                cameraSource!!.start(surfaceView.holder)
            } else {
                cameraSource!!.start()
            }
            requestLayout()

            if (overlay != null) {
                val size = cameraSource!!.previewSize
                size?.let {
                    val min = Math.min(size.width, size.height)
                    val max = Math.max(size.width, size.height)
                    overlay!!.setCameraInfo(min, max, cameraSource!!.cameraFacing)
                    overlay!!.clear()
                }
            }
            startRequested = false
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }

        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val horizontalPadding = paddingStart + paddingEnd
        val verticalPadding = paddingTop + paddingBottom
        val width = right - left - horizontalPadding
        val height = bottom - top - verticalPadding

        val layoutWidth = right - left
        val layoutHeight = bottom - top

        // Computes height and width for potentially doing fit width.
        var childWidth = layoutWidth
        var childHeight = (layoutWidth.toFloat() / width.toFloat() * height).toInt()

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight
            childWidth = (layoutHeight.toFloat() / height.toFloat() * width).toInt()
        }

        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, childWidth, childHeight)
            Log.d(TAG, "Assigned view: $i")
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }

    }

    companion object {
        private const val TAG = "myKubota:Preview"
    }
}