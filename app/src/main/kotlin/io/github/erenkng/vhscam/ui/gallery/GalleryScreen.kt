package io.github.erenkng.vhscam.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.ui.components.formatElapsed

@Composable
fun GalleryScreen(viewModel: GalleryViewModel, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    GalleryContent(items = items, onBack = onBack, onOpen = onOpen)
}

/** "Tapes": a grid of everything VHS Cam saved. */
@Composable
fun GalleryContent(
    items: List<CapturedMedia>?,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable (CapturedMedia, Modifier) -> Unit = { item, m ->
        AsyncImage(model = item.uri, contentDescription = item.displayName, contentScale = ContentScale.Crop, modifier = m)
    },
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                subtitle = {
                    val count = items?.size ?: 0
                    Text(pluralStringResource(R.plurals.gallery_count, count, count))
                },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            items == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.size(64.dp))
            }
            items.isEmpty() -> EmptyTapes(Modifier.fillMaxSize().padding(padding))
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(items, key = { _, item -> item.uri.toString() }) { index, item ->
                    Thumbnail(item, thumbnail) { onOpen(index) }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(item: CapturedMedia, image: @Composable (CapturedMedia, Modifier) -> Unit, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = item.displayName, onClick = onClick),
    ) {
        image(item, Modifier.fillMaxSize())
        if (item.isVideo) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        formatElapsed(item.durationMillis),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTapes(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(140.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_video_library),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            stringResource(R.string.gallery_empty),
            style = MaterialTheme.typography.titleLargeEmphasized,
            textAlign = TextAlign.Center,
        )
    }
}
