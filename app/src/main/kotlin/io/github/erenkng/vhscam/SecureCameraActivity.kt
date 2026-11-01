package io.github.erenkng.vhscam

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import io.github.erenkng.vhscam.launch.LaunchDestination
import io.github.erenkng.vhscam.launch.LaunchIntents
import io.github.erenkng.vhscam.ui.SecureCameraRoot

/**
 * The camera over the lock screen, for the power-button camera gesture and the Quick Settings
 * tile on a locked phone. It shows only the camera and what was shot since it opened; the
 * gallery and settings ask to unlock first and then open in the app. It goes away when the
 * screen turns off.
 */
class SecureCameraActivity : ShutterKeyActivity() {

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            screenOff,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (savedInstanceState == null) container.onLaunch(intent)
        setContent { SecureCameraRoot(onUnlock = ::unlockTo) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        container.onLaunch(intent)
    }

    override fun onDestroy() {
        unregisterReceiver(screenOff)
        super.onDestroy()
    }

    /** Asks for the lock screen to go away, then opens [destination] in the app. */
    private fun unlockTo(destination: LaunchDestination) {
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    startActivity(LaunchIntents.open(this@SecureCameraActivity, destination))
                    finish()
                }
            },
        )
    }
}
