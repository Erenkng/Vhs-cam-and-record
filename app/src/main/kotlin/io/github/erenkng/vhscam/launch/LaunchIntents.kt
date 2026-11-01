package io.github.erenkng.vhscam.launch

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import io.github.erenkng.vhscam.MainActivity
import io.github.erenkng.vhscam.SecureCameraActivity
import io.github.erenkng.vhscam.core.look.CaptureMode

/** Screens a launch from outside the app can open. */
enum class LaunchDestination { CAMERA, GALLERY, SETTINGS }

/**
 * The intents that open the camera from outside the app (the widget, the Quick Settings tile,
 * app shortcuts, the lock screen) and what they ask for. Photo and video use the standard
 * camera actions, so other apps and the system ask the same way.
 */
object LaunchIntents {

    private const val EXTRA_DESTINATION = "io.github.erenkng.vhscam.extra.DESTINATION"

    /** The camera as it was left. */
    fun camera(context: Context): Intent = main(context, Intent.ACTION_MAIN)

    fun photo(context: Context): Intent = main(context, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

    fun video(context: Context): Intent = main(context, MediaStore.INTENT_ACTION_VIDEO_CAMERA)

    /** [destination] in the app, once the phone is unlocked. */
    fun open(context: Context, destination: LaunchDestination): Intent =
        camera(context).putExtra(EXTRA_DESTINATION, destination.name)

    /** The camera over the lock screen, and nothing else of the app. */
    fun secure(context: Context): Intent =
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
            .setClass(context, SecureCameraActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The mode [intent] asks the camera to start in, if any. */
    fun mode(intent: Intent?): CaptureMode? = when (intent?.action) {
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE,
        -> CaptureMode.PHOTO
        MediaStore.INTENT_ACTION_VIDEO_CAMERA -> CaptureMode.VIDEO
        else -> null
    }

    /** The screen [intent] asks for, if any. */
    fun destination(intent: Intent?): LaunchDestination? {
        val name = intent?.getStringExtra(EXTRA_DESTINATION) ?: return null
        return LaunchDestination.entries.firstOrNull { it.name == name }
    }

    private fun main(context: Context, action: String): Intent =
        Intent(action)
            .setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}
