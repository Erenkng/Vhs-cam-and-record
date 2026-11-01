package io.github.erenkng.vhscam.launch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.erenkng.vhscam.R

/**
 * A home screen widget: the cassette opens the camera as it was left, the red button starts in
 * video and the light one in photo.
 */
class QuickLaunchWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick).apply {
            setOnClickPendingIntent(R.id.widget_open, pending(context, REQUEST_OPEN, LaunchIntents.camera(context)))
            setOnClickPendingIntent(R.id.widget_video, pending(context, REQUEST_VIDEO, LaunchIntents.video(context)))
            setOnClickPendingIntent(R.id.widget_photo, pending(context, REQUEST_PHOTO, LaunchIntents.photo(context)))
        }
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun pending(context: Context, request: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private companion object {
        const val REQUEST_OPEN = 1
        const val REQUEST_VIDEO = 2
        const val REQUEST_PHOTO = 3
    }
}
