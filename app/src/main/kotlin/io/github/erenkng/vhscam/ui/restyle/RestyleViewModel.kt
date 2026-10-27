package io.github.erenkng.vhscam.ui.restyle

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erenkng.vhscam.AppContainer
import io.github.erenkng.vhscam.camera.LookFactory
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.media.Original
import io.github.erenkng.vhscam.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

data class RestyleUiState(
    /** A copy of the settings: intensity changes here don't touch the camera's. */
    val settings: AppSettings = AppSettings(),
    val look: Look = Look.VHS,
    val preview: Bitmap? = null,
    val rendering: Boolean = false,
    val saving: Boolean = false,
    /** The photo kept no original, so its look can't change. */
    val missing: Boolean = false,
) {
    val intensity: Float get() = LookFactory.intensity(settings, look)
}

enum class RestyleMessage { SAVED, FAILED }

/** Develops a photo's original again with another look, as a new photo. */
class RestyleViewModel(private val container: AppContainer, private val mediaId: Long) : ViewModel() {

    private val _state = MutableStateFlow(RestyleUiState())
    val state: StateFlow<RestyleUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<RestyleMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<RestyleMessage> = _messages.asSharedFlow()

    private var original: Original? = null
    private var source: Bitmap? = null

    /** Light leaks and instant-film blotches stay put while trying looks. */
    private val seed = Random.nextFloat()
    private val requests = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            val loaded = container.originals.load(mediaId)
            if (loaded == null) {
                _state.update { it.copy(settings = settings, missing = true) }
                return@launch
            }
            source = withContext(Dispatchers.Default) {
                container.photoPipeline.decodeUpright(loaded.photo, PREVIEW_EDGE)
            }
            original = loaded
            _state.update { it.copy(settings = settings, look = settings.photoLook) }
            requests.value++
        }
        viewModelScope.launch {
            requests.collectLatest { request ->
                if (request == 0) return@collectLatest
                // Slider drags send many values; only the last one is worth rendering.
                delay(RENDER_DEBOUNCE_MILLIS)
                _state.update { it.copy(rendering = true) }
                val rendered = try {
                    renderPreview(_state.value)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Preview failed", e)
                    null
                }
                _state.update { it.copy(preview = rendered ?: it.preview, rendering = false) }
            }
        }
    }

    private suspend fun renderPreview(s: RestyleUiState): Bitmap? {
        val photo = original ?: return null
        val upright = source ?: return null
        return container.photoPipeline.preview(
            upright = upright,
            spec = LookFactory.spec(s.settings, s.look, seed),
            osd = LookFactory.osd(s.settings, s.look, LookFactory.clockTime(s.settings, photo.capturedAtMillis), 0L),
            mirror = photo.mirror,
            lookAspect = LookFactory.photoAspect(s.settings, s.look),
            longEdge = PREVIEW_EDGE,
        )
    }

    fun setLook(look: Look) {
        if (look == _state.value.look) return
        _state.update { it.copy(look = look) }
        requests.value++
    }

    fun setIntensity(value: Float) {
        _state.update { it.copy(settings = LookFactory.withIntensity(it.settings, it.look, value)) }
        requests.value++
    }

    /** Saves the current look as a new photo next to the old one. Keeps going if the user leaves. */
    fun save() {
        val photo = original ?: return
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        container.appScope.launch {
            val capturedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(photo.capturedAtMillis), ZoneId.systemDefault())
            val result = runCatching {
                container.photoPipeline.develop(
                    photo = photo.photo,
                    look = s.look,
                    spec = LookFactory.spec(s.settings, s.look, seed),
                    osd = LookFactory.osd(s.settings, s.look, LookFactory.clockTime(s.settings, photo.capturedAtMillis), 0L),
                    mirror = photo.mirror,
                    lookAspect = LookFactory.photoAspect(s.settings, s.look),
                    capturedAt = capturedAt,
                    capturedAtMillis = photo.capturedAtMillis,
                    // The new photo can change its look again.
                    keepOriginal = true,
                )
            }
            result.exceptionOrNull()?.let { Log.e(TAG, "Restyle failed", it) }
            _state.update { it.copy(saving = false) }
            _messages.tryEmit(if (result.isSuccess) RestyleMessage.SAVED else RestyleMessage.FAILED)
        }
    }

    companion object {
        private const val TAG = "RestyleViewModel"
        private const val PREVIEW_EDGE = 1280
        private const val RENDER_DEBOUNCE_MILLIS = 40L

        fun factory(container: AppContainer, mediaId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { RestyleViewModel(container, mediaId) }
        }
    }
}
