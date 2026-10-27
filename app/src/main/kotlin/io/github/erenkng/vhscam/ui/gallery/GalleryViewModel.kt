package io.github.erenkng.vhscam.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erenkng.vhscam.AppContainer
import io.github.erenkng.vhscam.media.CapturedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One-off messages for the viewer's snackbar. */
enum class GalleryMessage { GIF_SAVED, GIF_FAILED }

class GalleryViewModel(private val container: AppContainer) : ViewModel() {

    private val media = container.media

    private val _items = MutableStateFlow<List<CapturedMedia>?>(null)

    /** Everything in DCIM/VHS Cam, newest first; null while loading. */
    val items: StateFlow<List<CapturedMedia>?> = _items.asStateFlow()

    /** 0..1 while a GIF is being made, null otherwise. */
    val gifProgress: StateFlow<Float?> = container.gifMaker.progress

    private val _messages = MutableSharedFlow<GalleryMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<GalleryMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            media.version.collect { _items.value = media.loadAll() }
        }
    }

    fun delete(item: CapturedMedia) {
        viewModelScope.launch {
            if (media.delete(item) && !item.isVideo) container.originals.delete(ContentUris.parseId(item.uri))
        }
    }

    /** Whether the photo kept its original, so its look can change. */
    suspend fun hasOriginal(item: CapturedMedia): Boolean {
        if (item.isVideo || item.isGif) return false
        val id = ContentUris.parseId(item.uri)
        return withContext(Dispatchers.IO) { container.originals.has(id) }
    }

    /** Makes a GIF of [item] from [startMillis] on. Keeps going if the user leaves the gallery. */
    fun makeGif(item: CapturedMedia, startMillis: Long) {
        if (gifProgress.value != null) return
        container.appScope.launch {
            val result = runCatching { container.gifMaker.fromVideo(item.uri, startMillis) }
            result.exceptionOrNull()?.let { Log.e(TAG, "Making a GIF failed", it) }
            _messages.tryEmit(if (result.isSuccess) GalleryMessage.GIF_SAVED else GalleryMessage.GIF_FAILED)
        }
    }

    companion object {
        private const val TAG = "GalleryViewModel"

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { GalleryViewModel(container) }
        }

        fun share(context: Context, item: CapturedMedia) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
