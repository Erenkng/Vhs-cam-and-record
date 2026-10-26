package io.github.erenkng.vhscam.gl

import androidx.camera.core.CameraEffect
import androidx.core.util.Consumer

/**
 * Plugs [VhsSurfaceProcessor] into CameraX for the given targets, e.g. `PREVIEW` in photo mode
 * or `PREVIEW or VIDEO_CAPTURE` while filming.
 */
class VhsCameraEffect(
    targets: Int,
    processor: VhsSurfaceProcessor,
    onError: (Throwable) -> Unit,
) : CameraEffect(targets, processor.executor, processor, Consumer<Throwable> { onError(it) })
