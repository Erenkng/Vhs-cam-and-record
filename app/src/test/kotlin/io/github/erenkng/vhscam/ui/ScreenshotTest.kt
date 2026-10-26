package io.github.erenkng.vhscam.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.erenkng.vhscam.camera.CameraCapabilities
import io.github.erenkng.vhscam.camera.ExposureInfo
import io.github.erenkng.vhscam.camera.ZoomInfo
import io.github.erenkng.vhscam.core.exposure.Exposure
import io.github.erenkng.vhscam.core.look.CamcorderEffect
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.IntervalSetting
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.settings.AppSettings
import io.github.erenkng.vhscam.ui.camera.BoothUi
import io.github.erenkng.vhscam.ui.camera.CameraActions
import io.github.erenkng.vhscam.ui.camera.CameraContent
import io.github.erenkng.vhscam.ui.camera.CameraUiState
import io.github.erenkng.vhscam.ui.camera.EffectsSheetContent
import io.github.erenkng.vhscam.ui.camera.FocusStatus
import io.github.erenkng.vhscam.ui.camera.FocusUi
import io.github.erenkng.vhscam.ui.camera.IntervalUi
import io.github.erenkng.vhscam.ui.camera.ProSetting
import io.github.erenkng.vhscam.ui.camera.RecordingUi
import io.github.erenkng.vhscam.ui.gallery.GalleryContent
import io.github.erenkng.vhscam.ui.permission.PermissionContent
import io.github.erenkng.vhscam.ui.restyle.RestyleContent
import io.github.erenkng.vhscam.ui.restyle.RestyleUiState
import io.github.erenkng.vhscam.ui.settings.SettingsContent
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the screens on the JVM with Robolectric. `./gradlew :app:recordRoborazziDebug`
 * writes the PNGs to app/build/outputs/roborazzi.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val settings = AppSettings(dynamicColor = false)

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent(content)
        composeRule.mainClock.advanceTimeBy(1_200)
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun permission() = capture("01_permission") {
        VhsCamTheme(darkTheme = true, dynamicColor = false) {
            PermissionContent(denied = false, onRequest = {}, onOpenSettings = {})
        }
    }

    @Test
    fun cameraRecordingVhs() = capture("02_camera_vhs_recording") {
        CameraContent(
            state = CameraUiState(
                settings = settings.copy(mode = CaptureMode.VIDEO, grid = true),
                settingsLoaded = true,
                signal = true,
                zoom = ZoomInfo(ratio = 1f, min = 0.6f, max = 10f),
                exposure = ExposureInfo(index = 2, min = -12, max = 12, step = 1f / 6f),
                capabilities = CameraCapabilities(hasFlash = true, minFocusDiopters = 10f),
                recording = RecordingUi.Active(elapsedMillis = 83_000, paused = false),
                focus = FocusUi(x = 420f, y = 760f, status = FocusStatus.FOCUSED, locked = true, id = 1),
            ),
            actions = CameraActions(),
            viewfinder = { FakeScene(it) },
        )
    }

    @Test
    fun cameraPhotoFilm() = capture("03_camera_photo_film") {
        CameraContent(
            state = CameraUiState(
                settings = settings.copy(mode = CaptureMode.PHOTO, photoLook = Look.FILM),
                settingsLoaded = true,
                signal = true,
                zoom = ZoomInfo(ratio = 2f, min = 1f, max = 8f),
                capabilities = CameraCapabilities(hasFlash = true),
            ),
            actions = CameraActions(),
            viewfinder = { FakeScene(it) },
        )
    }

    @Test
    fun cameraEffects() = capture("07_camera_vhs_effects") {
        CameraContent(
            state = CameraUiState(
                settings = settings.copy(
                    mode = CaptureMode.VIDEO,
                    effect = CamcorderEffect.NIGHT_VISION,
                    fade = true,
                    interval = IntervalSetting.S10,
                ),
                settingsLoaded = true,
                signal = true,
                zoom = ZoomInfo(ratio = 5f, min = 1f, max = 5f),
                digitalZoom = 3f,
                capabilities = CameraCapabilities(hasFlash = true, dualCamera = true),
                recording = RecordingUi.Active(elapsedMillis = 14_000, paused = true),
                interval = IntervalUi(nextInSeconds = 7),
            ),
            actions = CameraActions(),
            effectRowOpen = true,
            viewfinder = { FakeScene(it) },
        )
    }

    @Test
    fun cameraPro() = capture("11_camera_pro") {
        CameraContent(
            state = CameraUiState(
                settings = settings.copy(mode = CaptureMode.PRO, photoLook = Look.FILM),
                settingsLoaded = true,
                signal = true,
                zoom = ZoomInfo(ratio = 1f, min = 0.6f, max = 10f),
                exposure = ExposureInfo(index = 0, min = -12, max = 12, step = 1f / 6f),
                capabilities = CameraCapabilities(
                    hasFlash = true,
                    minFocusDiopters = 10f,
                    isoRange = 50..3200,
                    exposureTimeRange = 100_000L..1_000_000_000L,
                ),
                iso = 400,
                meter = Exposure(iso = 320, timeNanos = 16_666_666L),
                manualFocus = 1f,
            ),
            actions = CameraActions(),
            proSetting = ProSetting.ISO,
            viewfinder = { FakeScene(it) },
        )
    }

    @Test
    fun cameraBooth() = capture("08_camera_booth") {
        CameraContent(
            state = CameraUiState(
                settings = settings.copy(mode = CaptureMode.PHOTO, photoLook = Look.GAMEBOY, booth = true),
                settingsLoaded = true,
                signal = true,
                capabilities = CameraCapabilities(hasFlash = true),
                booth = BoothUi(shot = 2, total = 4),
                countdown = 2,
            ),
            actions = CameraActions(),
            viewfinder = { FakeScene(it) },
        )
    }

    @Test
    fun cameraNoSignal() = capture("09_camera_no_signal") {
        CameraContent(
            state = CameraUiState(
                settings = settings.copy(mode = CaptureMode.VIDEO, videoLook = Look.CCTV),
                settingsLoaded = true,
                signal = false,
            ),
            actions = CameraActions(),
            viewfinder = { FakeScene(it) },
        )
    }

    @Test
    fun effectsSheet() = capture("06_effects_sheet") {
        VhsCamTheme(darkTheme = true, dynamicColor = false) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                EffectsSheetContent(
                    state = CameraUiState(
                        settings = settings.copy(mode = CaptureMode.VIDEO, playOsd = true),
                        settingsLoaded = true,
                        capabilities = CameraCapabilities(hasFlash = true, minFocusDiopters = 10f),
                        manualFocus = 2.5f,
                    ),
                    actions = CameraActions(),
                )
            }
        }
    }

    @Test
    fun gallery() = capture("04_gallery") {
        VhsCamTheme(darkTheme = false, dynamicColor = false) {
            GalleryContent(
                items = List(9) { i ->
                    CapturedMedia(
                        uri = Uri.parse("content://media/external/$i"),
                        isVideo = i % 3 == 0,
                        displayName = "VHS_$i",
                        dateTakenMillis = 0L,
                        durationMillis = 12_000L * (i + 1),
                        width = 640,
                        height = 480,
                    )
                },
                onBack = {},
                onOpen = {},
                thumbnail = { item, modifier ->
                    val hue = (item.displayName.hashCode() and 0xff) / 255f
                    Box(modifier.background(Brush.linearGradient(listOf(Color.hsv(hue * 360f, 0.5f, 0.8f), Color(0xFF331A00)))))
                },
            )
        }
    }

    @Test
    fun restyle() = capture("10_restyle") {
        val preview = remember { fakeInstantPhoto() }
        RestyleContent(
            state = RestyleUiState(settings = settings, look = Look.POLAROID, preview = preview),
            onBack = {},
            onLook = {},
            onIntensity = {},
            onSave = {},
        )
    }

    @Test
    fun settingsScreen() = capture("05_settings") {
        VhsCamTheme(darkTheme = false, dynamicColor = false) {
            SettingsContent(
                settings = settings,
                versionName = "1.0",
                onChange = {},
                onBack = {},
                onOpenLicenses = {},
                originalsBytes = 18_400_000L,
            )
        }
    }
}

/** An instant photo of the fake scene, for the restyle preview. */
private fun fakeInstantPhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(880, 1070, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(0xFFF3F0E8.toInt())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(
        0f, 50f, 0f, 840f, 0xFF6A8CAF.toInt(), 0xFFE8A87C.toInt(), android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRect(50f, 66f, 830f, 846f, paint)
    paint.shader = null
    paint.color = 0xFFFFE8A3.toInt()
    canvas.drawCircle(560f, 400f, 90f, paint)
    paint.color = 0xFF3E6B48.toInt()
    canvas.drawRect(50f, 600f, 830f, 846f, paint)
    return bitmap
}

/** A stand-in for the camera feed: a warm sky, a sun and some hills. */
@Composable
private fun FakeScene(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF3A6EA5), Color(0xFFF2B880), Color(0xFFE07A5F))))
        drawCircle(Color(0xFFFFE8A3), radius = size.minDimension * 0.12f, center = Offset(size.width * 0.68f, size.height * 0.42f))
        drawRect(Color(0xFF2F4F3A), topLeft = Offset(0f, size.height * 0.62f))
        drawCircle(Color(0xFF3E6B48), radius = size.width * 0.5f, center = Offset(size.width * 0.2f, size.height * 0.78f))
    }
}
