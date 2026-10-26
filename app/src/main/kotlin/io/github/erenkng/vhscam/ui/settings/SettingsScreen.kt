package io.github.erenkng.vhscam.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.VhsCamApp
import io.github.erenkng.vhscam.settings.AppSettings
import io.github.erenkng.vhscam.settings.VideoQuality
import io.github.erenkng.vhscam.ui.components.ConnectedToggleGroup
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenLicenses: () -> Unit) {
    val container = (LocalContext.current.applicationContext as VhsCamApp).container
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val versionName = remember { context.versionName() }
    SettingsContent(
        settings = settings,
        versionName = versionName,
        onChange = { transform -> scope.launch { container.settings.update(transform) } },
        onBack = onBack,
        onOpenLicenses = onOpenLicenses,
    )
}

private fun Context.versionName(): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return info.versionName.orEmpty()
}

@Composable
fun SettingsContent(
    settings: AppSettings,
    versionName: String,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            header(R.string.settings_video)
            item {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.video_quality), style = MaterialTheme.typography.titleSmallEmphasized)
                    ConnectedToggleGroup(
                        options = VideoQuality.entries,
                        selected = settings.videoQuality,
                        onSelect = { q -> onChange { it.copy(videoQuality = q) } },
                        fill = true,
                        modifier = Modifier.fillMaxWidth(),
                    ) { q -> Text(stringResource(q.label())) }
                    Text(
                        stringResource(R.string.video_quality_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            toggles(
                listOf(
                    Toggle(R.string.microphone, R.string.microphone_help, R.drawable.ic_mic, settings.microphone) { v ->
                        onChange { it.copy(microphone = v) }
                    },
                    Toggle(R.string.tape_audio, R.string.tape_audio_help, R.drawable.ic_graphic_eq, settings.tapeAudio, enabled = settings.microphone) { v ->
                        onChange { it.copy(tapeAudio = v) }
                    },
                ),
            )

            header(R.string.settings_camera)
            toggles(
                listOf(
                    Toggle(R.string.mirror_selfies, R.string.mirror_selfies_help, R.drawable.ic_flip, settings.mirrorSelfies) { v ->
                        onChange { it.copy(mirrorSelfies = v) }
                    },
                    Toggle(R.string.shutter_sound, null, R.drawable.ic_volume_up, settings.shutterSound) { v ->
                        onChange { it.copy(shutterSound = v) }
                    },
                    Toggle(R.string.haptics, null, R.drawable.ic_vibration, settings.haptics) { v ->
                        onChange { it.copy(haptics = v) }
                    },
                ),
            )

            header(R.string.settings_appearance)
            toggles(
                listOf(
                    Toggle(R.string.dynamic_color, R.string.dynamic_color_help, R.drawable.ic_palette, settings.dynamicColor) { v ->
                        onChange { it.copy(dynamicColor = v) }
                    },
                ),
            )

            header(R.string.settings_about)
            item {
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
                    supportingContent = { Text(versionName) },
                ) {
                    Text(stringResource(R.string.version))
                }
            }
            item {
                SegmentedListItem(
                    onClick = onOpenLicenses,
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    leadingContent = { Icon(painterResource(R.drawable.ic_description), contentDescription = null) },
                    supportingContent = { Text(stringResource(R.string.licenses_help)) },
                ) {
                    Text(stringResource(R.string.licenses))
                }
            }
        }
    }
}

private class Toggle(
    val title: Int,
    val summary: Int?,
    val icon: Int,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onChange: (Boolean) -> Unit,
)

private fun LazyListScope.header(title: Int) {
    item {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        )
    }
}

/** A group of switches drawn as one rounded M3E segmented list. */
private fun LazyListScope.toggles(items: List<Toggle>) {
    items.forEachIndexed { index, toggle ->
        item {
            val summary = toggle.summary
            SegmentedListItem(
                checked = toggle.checked,
                onCheckedChange = toggle.onChange,
                shapes = ListItemDefaults.segmentedShapes(index = index, count = items.size),
                enabled = toggle.enabled,
                leadingContent = { Icon(painterResource(toggle.icon), contentDescription = null) },
                supportingContent = if (summary != null) {
                    { Text(stringResource(summary)) }
                } else {
                    null
                },
                trailingContent = { Switch(checked = toggle.checked, onCheckedChange = null, enabled = toggle.enabled) },
            ) {
                Text(stringResource(toggle.title))
            }
        }
    }
}

private fun VideoQuality.label(): Int = when (this) {
    VideoQuality.SD -> R.string.quality_sd
    VideoQuality.HD -> R.string.quality_hd
    VideoQuality.FHD -> R.string.quality_fhd
}

/** Open-source licences of the bundled fonts and icons. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val texts = remember {
        context.assets.list("licenses").orEmpty().sorted().map { name ->
            name to context.assets.open("licenses/$name").bufferedReader().use { it.readText() }
        }
    }
    var expanded by remember { mutableStateOf<String?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.licenses)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            texts.forEachIndexed { index, (name, text) ->
                item {
                    SegmentedListItem(
                        onClick = { expanded = if (expanded == name) null else name },
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = texts.size),
                        supportingContent = if (expanded == name) {
                            { Text(text, style = MaterialTheme.typography.bodySmall) }
                        } else {
                            null
                        },
                    ) {
                        Text(name.removeSuffix(".txt"))
                    }
                }
            }
        }
    }
}
