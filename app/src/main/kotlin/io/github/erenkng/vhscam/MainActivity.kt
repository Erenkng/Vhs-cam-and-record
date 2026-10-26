package io.github.erenkng.vhscam

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.erenkng.vhscam.ui.AppRoot

class MainActivity : ComponentActivity() {

    private val container get() = (application as VhsCamApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The tape sinks into the deck as the splash fades; the screen then powers on.
            splashScreen.setOnExitAnimationListener { splash ->
                splash.iconView?.animate()
                    ?.translationY(splash.height * 0.04f)
                    ?.scaleX(0.82f)
                    ?.scaleY(0.82f)
                    ?.setInterpolator(AccelerateInterpolator())
                    ?.setDuration(SPLASH_EXIT_MILLIS)
                    ?.start()
                splash.animate()
                    .alpha(0f)
                    .setDuration(SPLASH_EXIT_MILLIS)
                    .withEndAction { splash.remove() }
                    .start()
            }
        }
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

    private companion object {
        const val SPLASH_EXIT_MILLIS = 260L
    }
}
