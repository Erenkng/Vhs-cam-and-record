package io.github.erenkng.vhscam.gl

import io.github.erenkng.vhscam.core.look.CcdParams
import io.github.erenkng.vhscam.core.look.FilmParams
import io.github.erenkng.vhscam.core.look.PhotoLook
import io.github.erenkng.vhscam.core.look.PolaroidParams
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.look.VhsParams

/** Everything the renderer needs to know about the look of a frame. */
sealed interface LookSpec {
    val kind: PhotoLook

    /** @param glitch 0..1 burst of tape tearing, e.g. right after recording starts. */
    data class Vhs(val params: VhsParams, val speed: TapeSpeed, val glitch: Float = 0f) : LookSpec {
        override val kind get() = PhotoLook.VHS
    }

    data class Ccd(val params: CcdParams) : LookSpec {
        override val kind get() = PhotoLook.CCD
    }

    data class Film(val params: FilmParams, val leakSeed: Float) : LookSpec {
        override val kind get() = PhotoLook.FILM
    }

    data class Polaroid(val params: PolaroidParams, val seed: Float) : LookSpec {
        override val kind get() = PhotoLook.POLAROID
    }
}

/** Text shown by the on-screen displays. Which fields are used depends on the look. */
data class OsdContent(
    val camcorderTime: String? = null,
    val camcorderDate: String? = null,
    val playIndicator: Boolean = false,
    val speedLabel: String? = null,
    val counter: String? = null,
    val digitalStamp: String? = null,
    val filmStamp: String? = null,
)
