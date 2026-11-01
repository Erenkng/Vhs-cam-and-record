package io.github.erenkng.vhscam

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.erenkng.vhscam.ui.AppRoot

class MainActivity : ShutterKeyActivity() {

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
        // The widget, the tile, a shortcut or another app may ask for a mode or a screen.
        if (savedInstanceState == null) container.onLaunch(intent)
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        container.onLaunch(intent)
    }

    private companion object {
        const val SPLASH_EXIT_MILLIS = 260L
    }
}
