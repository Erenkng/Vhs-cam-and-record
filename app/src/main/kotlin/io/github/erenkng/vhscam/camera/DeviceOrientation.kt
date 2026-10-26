package io.github.erenkng.vhscam.camera

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How the phone is physically held, as a `Surface.ROTATION_*` value. The UI is locked to
 * portrait, so this drives capture rotation, the VHS processing space and icon rotation.
 */
class DeviceOrientation(context: Context) {

    private val _rotation = MutableStateFlow(Surface.ROTATION_0)
    val rotation: StateFlow<Int> = _rotation.asStateFlow()

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            // 20 degree dead zones around the diagonals stop the value from flickering.
            val next = when (orientation) {
                in 0..34, in 325..359 -> Surface.ROTATION_0
                in 55..124 -> Surface.ROTATION_270
                in 145..214 -> Surface.ROTATION_180
                in 235..304 -> Surface.ROTATION_90
                else -> return
            }
            _rotation.value = next
        }
    }

    fun start() {
        if (listener.canDetectOrientation()) listener.enable()
    }

    fun stop() = listener.disable()

    companion object {
        /** Clockwise degrees to rotate UI content so it stays upright for [rotation]. */
        fun iconRotationDegrees(rotation: Int): Float = when (rotation) {
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> -90f
            else -> 0f
        }
    }
}
