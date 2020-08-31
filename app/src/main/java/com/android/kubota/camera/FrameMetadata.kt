package com.android.kubota.camera

data class FrameMetadata(
    val width: Int,
    val height: Int,
    val rotation: Int,
    val cameraFacing: Int
)