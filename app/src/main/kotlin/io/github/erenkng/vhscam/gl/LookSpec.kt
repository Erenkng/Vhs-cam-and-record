package io.github.erenkng.vhscam.gl

import io.github.erenkng.vhscam.core.look.CamcorderEffect
import io.github.erenkng.vhscam.core.look.CcdParams
import io.github.erenkng.vhscam.core.look.CctvParams
import io.github.erenkng.vhscam.core.look.DisposableParams
import io.github.erenkng.vhscam.core.look.FilmStockParams
import io.github.erenkng.vhscam.core.look.FilmParams
import io.github.erenkng.vhscam.core.look.GameBoyParams
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.PolaroidParams
import io.github.erenkng.vhscam.core.look.Super8Params
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.look.VhsParams
import io.github.erenkng.vhscam.core.look.WebcamParams

/** Everything the renderer needs to know about the look of a frame. */
sealed interface LookSpec {
    val kind: Look

    /** Vignette and line structure added by the present pass. */
    val vignette: Float get() = 0f
    val scanlines: Float get() = 0f

    /** Magnify with nearest-neighbour sampling, for pixel-art looks. */
    val nearest: Boolean get() = false

    /** Show a new picture every this many camera frames. */
    val holdFrames: Int get() = kind.holdFrames

    /** Show this many pictures a second instead, whatever the camera's frame rate. */
    val pictureRate: Int? get() = kind.pictureRate

    /**
     * @param glitch 0..1 burst of tape tearing, e.g. right after recording starts.
     * @param effect the camcorder's digital effect, applied before the tape.
     */
    data class Vhs(
        val params: VhsParams,
        val speed: TapeSpeed,
        val glitch: Float = 0f,
        val effect: CamcorderEffect = CamcorderEffect.NONE,
    ) : LookSpec {
        override val kind get() = Look.VHS
        override val vignette get() = params.vignette
        override val scanlines get() = params.scanlines
        override val holdFrames get() = effect.holdFrames
    }

    data class Ccd(val params: CcdParams) : LookSpec {
        override val kind get() = Look.CCD
    }

    data class Film(val params: FilmParams, val leakSeed: Float) : LookSpec {
        override val kind get() = Look.FILM
    }

    data class Polaroid(val params: PolaroidParams, val seed: Float) : LookSpec {
        override val kind get() = Look.POLAROID
    }

    data class GameBoy(val params: GameBoyParams) : LookSpec {
        override val kind get() = Look.GAMEBOY
        override val nearest get() = true
    }

    data class Webcam(val params: WebcamParams) : LookSpec {
        override val kind get() = Look.WEBCAM
    }

    data class Super8(val params: Super8Params) : LookSpec {
        override val kind get() = Look.SUPER8
    }

    /** One of the colour film stocks (see FilmStocks); [kind] says which. */
    data class FilmStock(override val kind: Look, val params: FilmStockParams) : LookSpec

    data class Disposable(val params: DisposableParams) : LookSpec {
        override val kind get() = Look.DISPOSABLE
    }

    data class Cctv(val params: CctvParams) : LookSpec {
        override val kind get() = Look.CCTV
        override val scanlines get() = params.scanlines
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
    val cctvStamp: String? = null,
)

/** What the present pass adds on top of the look: the VCR blue screen and the fader. */
data class PresentFx(val blueScreen: Float = 0f, val fade: Float = 0f, val fadeWhite: Boolean = false)
