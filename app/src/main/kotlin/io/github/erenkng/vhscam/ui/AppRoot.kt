package io.github.erenkng.vhscam.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.erenkng.vhscam.settings.AppSettings
import io.github.erenkng.vhscam.ui.camera.CameraScreen
import io.github.erenkng.vhscam.ui.gallery.GalleryScreen
import io.github.erenkng.vhscam.ui.gallery.GalleryViewModel
import io.github.erenkng.vhscam.ui.gallery.ViewerScreen
import io.github.erenkng.vhscam.ui.permission.PermissionScreen
import io.github.erenkng.vhscam.ui.settings.LicensesScreen
import io.github.erenkng.vhscam.ui.settings.SettingsScreen
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme
import kotlinx.serialization.Serializable

@Serializable
data object CameraRoute

@Serializable
data object GalleryRoute

@Serializable
data class ViewerRoute(val index: Int)

@Serializable
data object SettingsRoute

@Serializable
data object LicensesRoute

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as VhsCamApp
    val settings by app.container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    fun cameraGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    var hasCamera by remember { mutableStateOf(cameraGranted()) }
    // The user may grant the permission from system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasCamera = cameraGranted() }

    VhsCamTheme(dynamicColor = settings.dynamicColor) {
        if (!hasCamera) {
            PermissionScreen(onGranted = { hasCamera = true })
            return@VhsCamTheme
        }
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = CameraRoute) {
            composable<CameraRoute> {
                CameraScreen(
                    onOpenGallery = { nav.navigate(GalleryRoute) },
                    onOpenSettings = { nav.navigate(SettingsRoute) },
                )
            }
            composable<GalleryRoute> {
                val gallery: GalleryViewModel = viewModel(factory = GalleryViewModel.factory(app.container.media))
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
                    factory = GalleryViewModel.factory(app.container.media),
                )
                ViewerScreen(viewModel = gallery, startIndex = route.index, onBack = { nav.popBackStack() })
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { nav.popBackStack() }, onOpenLicenses = { nav.navigate(LicensesRoute) })
            }
            composable<LicensesRoute> {
                LicensesScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
