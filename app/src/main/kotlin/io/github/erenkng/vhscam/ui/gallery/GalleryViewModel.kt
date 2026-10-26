package io.github.erenkng.vhscam.ui.gallery

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.media.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(private val media: MediaRepository) : ViewModel() {

    private val _items = MutableStateFlow<List<CapturedMedia>?>(null)

    /** Everything in DCIM/VHS Cam, newest first; null while loading. */
    val items: StateFlow<List<CapturedMedia>?> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            media.version.collect { _items.value = media.loadAll() }
        }
    }

    fun delete(item: CapturedMedia) {
        viewModelScope.launch { media.delete(item) }
    }

    companion object {
        fun factory(media: MediaRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { GalleryViewModel(media) }
        }

        fun share(context: Context, item: CapturedMedia) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = if (item.isVideo) "video/mp4" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
