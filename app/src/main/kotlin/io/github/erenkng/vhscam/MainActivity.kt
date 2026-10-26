package io.github.erenkng.vhscam

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.erenkng.vhscam.ui.AppRoot

class MainActivity : ComponentActivity() {

    private val container get() = (application as VhsCamApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }

    /** Volume keys (and a hardware camera key) work as a shutter while the camera is showing. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode.isShutterKey() && container.cameraVisible) {
            if (event.repeatCount == 0) container.onHardwareShutter()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode.isShutterKey() && container.cameraVisible) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun Int.isShutterKey() =
        this == KeyEvent.KEYCODE_VOLUME_DOWN || this == KeyEvent.KEYCODE_VOLUME_UP || this == KeyEvent.KEYCODE_CAMERA
}
