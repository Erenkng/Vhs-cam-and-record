package io.github.erenkng.vhscam.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.erenkng.vhscam.VhsCamApp
import io.github.erenkng.vhscam.ui.camera.CameraScreen
import io.github.erenkng.vhscam.ui.gallery.GalleryScreen
import io.github.erenkng.vhscam.ui.gallery.GalleryViewModel
import io.github.erenkng.vhscam.ui.gallery.ViewerScreen
import io.github.erenkng.vhscam.ui.launch.CrtPowerOn
import io.github.erenkng.vhscam.ui.onboarding.OnboardingScreen
import io.github.erenkng.vhscam.ui.permission.PermissionScreen
import io.github.erenkng.vhscam.ui.restyle.RestyleScreen
import io.github.erenkng.vhscam.ui.restyle.RestyleViewModel
import io.github.erenkng.vhscam.ui.settings.LicensesScreen
import io.github.erenkng.vhscam.ui.settings.SettingsScreen
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object CameraRoute

@Serializable
data object GalleryRoute

@Serializable
data class ViewerRoute(val index: Int)

@Serializable
data class RestyleRoute(val mediaId: Long)

@Serializable
data object SettingsRoute

@Serializable
data object LicensesRoute

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as VhsCamApp
    // Null until the settings are read, so returning users never see the onboarding flash by.
    val settings by app.container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    fun cameraGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    var hasCamera by remember { mutableStateOf(cameraGranted()) }
    // The user may grant the permission from system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasCamera = cameraGranted() }
    val scope = rememberCoroutineScope()
    // The screen powers on at launch, and again when the onboarding hands over to the camera.
    var powerOns by rememberSaveable { mutableIntStateOf(1) }
    var poweringOn by rememberSaveable { mutableStateOf(true) }

    val loaded = settings
    VhsCamTheme(dynamicColor = loaded?.dynamicColor ?: true) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when {
                loaded == null -> Unit
                !loaded.onboardingDone -> OnboardingScreen(
                    dynamicColor = loaded.dynamicColor,
                    onDone = {
                        powerOns++
                        poweringOn = true
                        scope.launch { app.container.settings.update { it.copy(onboardingDone = true) } }
                    },
                )
                !hasCamera -> PermissionScreen(onGranted = { hasCamera = true })
                else -> AppNavigation(app)
            }
            if (poweringOn) {
                key(powerOns) { CrtPowerOn(onFinished = { poweringOn = false }) }
            }
        }
    }
}

@Composable
private fun AppNavigation(app: VhsCamApp) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = CameraRoute) {
        composable<CameraRoute> {
            CameraScreen(
                onOpenGallery = { nav.navigate(GalleryRoute) },
                onOpenSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<GalleryRoute> {
            val gallery: GalleryViewModel = viewModel(factory = GalleryViewModel.factory(app.container))
            GalleryScreen(
                viewModel = gallery,
                onBack = { nav.popBackStack() },
                onOpen = { index -> nav.navigate(ViewerRoute(index)) },
            )
        }
        composable<ViewerRoute> { entry ->
            val route = entry.toRoute<ViewerRoute>()
            val galleryEntry = remember(entry) { nav.getBackStackEntry<GalleryRoute>() }
            val gallery: GalleryViewModel = viewModel(
                viewModelStoreOwner = galleryEntry,
                factory = GalleryViewModel.factory(app.container),
            )
            ViewerScreen(
                viewModel = gallery,
                startIndex = route.index,
                onBack = { nav.popBackStack() },
                onRestyle = { item -> nav.navigate(RestyleRoute(ContentUris.parseId(item.uri))) },
            )
        }
        composable<RestyleRoute> { entry ->
            val route = entry.toRoute<RestyleRoute>()
            val restyle: RestyleViewModel = viewModel(factory = RestyleViewModel.factory(app.container, route.mediaId))
            RestyleScreen(viewModel = restyle, onBack = { nav.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { nav.popBackStack() }, onOpenLicenses = { nav.navigate(LicensesRoute) })
        }
        composable<LicensesRoute> {
            LicensesScreen(onBack = { nav.popBackStack() })
        }
    }
}
