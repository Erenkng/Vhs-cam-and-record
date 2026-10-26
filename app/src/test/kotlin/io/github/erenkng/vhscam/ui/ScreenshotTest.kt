package io.github.erenkng.vhscam.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders screens on the JVM with Robolectric. Run `./gradlew :app:recordRoborazziDebug` to
 * write the PNGs to app/build/outputs/roborazzi.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appRoot() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            VhsCamTheme(darkTheme = true, dynamicColor = false) {
                AppRoot()
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/app_root.png")
    }
}
