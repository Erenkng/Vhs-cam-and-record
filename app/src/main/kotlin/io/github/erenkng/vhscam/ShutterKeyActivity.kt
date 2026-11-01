package io.github.erenkng.vhscam

import android.view.KeyEvent
import androidx.activity.ComponentActivity

/** An activity showing the camera: volume keys (and a hardware camera key) work as a shutter. */
abstract class ShutterKeyActivity : ComponentActivity() {

    protected val container: AppContainer get() = (application as VhsCamApp).container

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
